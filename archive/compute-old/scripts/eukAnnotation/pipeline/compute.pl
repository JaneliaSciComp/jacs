#!/usr/local/perl

#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

use strict;
our $errorMessage;
#our $vicsWSDL = "http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl";	#production
our $vicsWSDL = "http://camdev3:8180/compute-compute/ComputeWS?wsdl"; #jinman testing
require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline/');
use EAP::generic;
use EAP::dataset;
use EAP::compute;
$| = 1;

#-----------------------------------------------------------------------
# get options from command line
my ( $annotationDB, $configDB, $projectCode, $maxRetries, $queryaliases, $onlycompute, $vicswsdlurl ) = &initialize;
if ( defined $vicswsdlurl ) { $vicsWSDL = $vicswsdlurl }
$annotationDB = realpath($annotationDB);

my %queryAlias;
if ( defined $queryaliases ) {
	my @aliases = split( /,/, $queryaliases );
	foreach my $alias ( @aliases ) {
		( my $tmp = $alias ) =~ s/\s\s*as\s\s*/\|/;
		my ( $aliasname, $queryname ) = split( /\|/, $tmp );
		$queryname =~ s/^\s+//;
		$queryname =~ s/\s+$//;
		$aliasname =~ s/^\s+//;
		$aliasname =~ s/\s+$//;
		$queryAlias{$queryname} = $aliasname;
		print "Using $aliasname in place of query set $queryname\n";
		if ( ! defined $aliasname || length( $aliasname ) == 0
				|| ! defined $queryname || length( $queryname) == 0 ) {
			die "\nMis-formatted alias: $alias\n";								
		}
	}
}

#-----------------------------------------------------------------------
# open annotation database
my $annodbh = &connectSQLite( $annotationDB, my $autocommit = 1 );
if ( !defined $annodbh ) {
	die "\nCould not open annotation db: " . $errorMessage . "\n";
}

#-----------------------------------------------------------------------
# read required computes from config file
my $computes;
{
	my $tmp = &getJobConfiguration( $configDB, $onlycompute );
	if ( !defined $tmp ) {
		die "\nCould not read configuration file: " . $errorMessage . "\n";
	}

    # substitute aliases for query datasets
	foreach my $compute ( @$tmp ) {
		if ( exists $queryAlias{ $$compute{query_db_name} } ) {
			$$compute{query_db_name} = $queryAlias{ $$compute{query_db_name} };
		}
	}

    # fill in specific project code, program version, and dataset versions
	$tmp = &prepareComputes( $annodbh, $tmp, $configDB, $projectCode );
	if ( !defined $tmp ) {
		die "\nCould not prepare computes: " . $errorMessage . "\n";
	}

	$computes = arrayHashToHashHash( $tmp, "job_name" );
}

#-----------------------------------------------------------------------
# get current result sets from annotation database
my $resultsets;
{
	my $tmp = &getCurrentResultSets( $annodbh );
	if ( !defined $tmp ) {
		die "\nCould not read current resultsets: " . $errorMessage . "\n";
	}

    # remove results for non-applicable computes
	$resultsets = arrayHashToHashHash( $tmp, "job_name" );
	foreach my $job_name ( keys %$resultsets ) {
		if ( !exists $$computes{$job_name} ) {
			delete ( $$resultsets{$job_name} );
		}
	}
}

#-----------------------------------------------------------------------
# submit compute jobs for missing or stale results
my $jobs;
{
	my $tmp = &scheduleJobs( $annodbh, $computes, $resultsets );
	if ( !defined $tmp) {
		die "\nCould not initialize jobs: " . $errorMessage . "\n";
	}
	$jobs = arrayHashToHashHash( $tmp, "job_id" );
}

# ignore result sets for computes that are in-progress
for my $jobid ( keys %$jobs ) {
	my $jobname = $$jobs{$jobid}{job_name};
	delete( $$resultsets{$jobname} );
}

# ignore result sets for computes that haven't been loaded
foreach my $jobname ( keys %$resultsets ) {
	if ( $$resultsets{$jobname}{status} ne "loaded" ) {
		delete( $$resultsets{$jobname} );
	}
}

#-----------------------------------------------------------------------
# report loaded computations
print "\nCurrent results:\n";
&displayComputes( $resultsets );

# report in-progress computations
print "\nResults in progress:\n";
&displayComputes( $jobs );
	
#-----------------------------------------------------------------------
# if there are hmm jobs, refresh the egad data
if ( scalar keys %$jobs > 0 ) {

	foreach my $job ( values %$jobs ) {
		if ( $$job{program_name} eq "hmmpfam" ) {
			if ( !defined &refreshEgad( $annodbh ) ) {
				die "\nCould not refresh egad data: " . $errorMessage . "\n";
			}
		last;
		}
	}
	
#-----------------------------------------------------------------------
# run local jobs and remove from queue
	$resultsets = &runLocalJobs( $annodbh, $jobs, $resultsets);
	if ( !defined $resultsets ) {
		die "\nCould not run local jobs: " . $errorMessage . "\n";
	}

#-----------------------------------------------------------------------
# load VICS results as they become available
	$resultsets = &loadVICSResults( $annodbh, $jobs, $maxRetries, $resultsets);
	if ( !defined $resultsets ) {
		die "\nCould not load VICS results: " . $errorMessage . "\n";
	}

#-----------------------------------------------------------------------
# report final results
	print "\nUpdated results:\n";
	&displayComputes( $resultsets );

#-----------------------------------------------------------------------
# update database statistics
	&executeSQL( $annodbh, "analyze" );

#-----------------------------------------------------------------------
# all computes have finished
	$annodbh->disconnect;
}
foreach my $compute ( values %$resultsets ) {
	if ( $$compute{status} ne "loaded" ) { die "\nOne or more computes failed.\n" }
}
print "\nComputation completed.\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_C $opt_P $opt_a $opt_c $opt_h $opt_r $opt_v );
	&Getopts('D:C:P:r:a:c:v:h');

	if ( $opt_h ) {
		print
"
This script checks the status of all required computes, submits grid jobs for new computes or
to refresh computes based on obsolete data sources or programs, and loads the results into a
sqlite database.

usage: ./compute.pl -D ntsm07.db -C eapPrecomputesCfg.db -P 0999

-D <annotation db file> path to sqlite db contains query sets and compute results
-C <configuration db file> path to sqlite db defining computes
-P <project code> project code for grid accounting
-a <query aliases> aliases from query dataset names that do not match configuration, e.g -a \"metagene_mapped_proteins as proteins, reprocessed_reads as reads\"
-r <max retries> optional, maximum number of times to resubmit a failed job (default is 0)
-c <compute list> optional, run only the computes named in comma separated list
-v <jacs wsdl url> optional, use an alternate vics wsdl

Originally written by Jeff Hoover, now maintained by:
Jason Inman
jinman\@jcvi.org
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the annotation db file (-D)."
	} elsif ( ! -e $opt_D) {
		die "\nThe specified annotation db file does not exist: \"-D " . $opt_D . "\".";
	}
	
	if ( !$opt_C ) {
		die "\nYou must specify the configuration db file (-C)."
	} elsif ( ! -e $opt_C ) {
		die "\nThe specified configuration db file does not exist: \"-C " . $opt_C ."\".";
	}
	
	if ( !$opt_P ) {
		die "\nYou must specify a project code (-P)."
	}
	
	if ( !defined $opt_r || length($opt_r) == 0 ) {
		$opt_r = 0;
	}
	
	return ( $opt_D, $opt_C, $opt_P, $opt_r, $opt_a, $opt_c, $opt_v );	
}

# process active VICS jobs
sub loadVICSResults {
	my ( $dbh, $jobs, $maxRetries, $resultsets ) = @_;
	while ( scalar keys %$jobs > 0 ) {
		my $vicsJobs = &vicsWaitForAnyJobs( keys %$jobs );
		if ( !defined $vicsJobs ) {
			$errorMessage =  "loadVICSResults: " . $errorMessage;
			return undef;		
		} elsif ( $vicsJobs == 0 ) {
			$errorMessage =  "loadVICSResults:  unexpected end-of-queue.";
			return undef;		
		}

        # process finished jobs
		foreach my $vicsJob ( @$vicsJobs ) {
			my $jobid = $$vicsJob{id};
			my $jobname = $$vicsJob{name};
			my $status = $$vicsJob{status};
			my $result_type = $$vicsJob{result_type};
			my $result_message = $$vicsJob{result_message};
			my $compute = $$jobs{$jobid};

            # update status and remove from queue
			$$compute{date_completed} = &now;
			if ( !defined &updateComputeStatus( $dbh,  $compute, $status, $result_type, $result_message ) ) {
				$errorMessage = "loadVICSResults: " . $errorMessage;
				return undef;
			}
			delete( $$jobs{$jobid} );

            # load results if job completed successfully
			if ( $status eq "completed" ) {
				print "Task $$compute{job_id} ($$compute{job_name}) completed at " . &now . "\nloading results...";
				$compute = &loadComputeResults( $dbh, $compute );
				if ( defined $compute ) {
					print $$compute{num_results} . " hits.\n";
				} else {
					print "  failed: " . $errorMessage . "\n";	
				} 
				my $msg = &vicsDeleteTask( $$compute{submitted_by}, $$compute{job_id} );

            # resubmit it if job failed and retry limit hasn't been reached
			} elsif ( $$compute{retries} < $maxRetries ) {
				$$compute{retries}++;
				print "Task $$compute{job_id} ($$compute{job_name}) failed at " . &now . ", resubmitting ($$compute{retries}).\n";
				my $msg = &vicsDeleteTask( $$compute{submitted_by}, $$compute{job_id} );

				&submitCompute( $dbh, $compute );
				$$jobs{$$compute{job_id}} = $compute;

            # skip it if job failed and retry limit has been reached
			} else {
				print "Task $$compute{job_id} ($$compute{job_name}) failed at " . &now . ", retry limit reached.\n";
				my $msg = &vicsDeleteTask( $$compute{submitted_by}, $$compute{job_id} );
			}

            # save updated result status
			$$resultsets{$jobname} = $compute;
		}
	}
	
    # return results
	return $resultsets;
}

# run local jobs
sub runLocalJobs {
	my ( $dbh, $jobs,  $resultsets ) = @_;
	foreach my $jobid ( keys %$jobs ) {

        # find next local job
		my $compute = $$jobs{$jobid};
		if ( $$compute{status} eq "local" ) {

            # pepstats
			if ( $$compute{program_name} =~ /pepstats/i ) {
				print "running " . $$compute{job_name} . " locally...";
				$$compute{date_submitted} = &now;
				$$compute{submitted_by} = getlogin || getpwuid($>) || 'ccbuild';
				my $compute = &runPepstats( $dbh, $compute );
				if ( !defined $compute ) {
					$errorMessage = "runLocalJobs: " . $errorMessage;
					return undef;
				} elsif ( $$compute{status} eq "error" ) {
					print "failed: " . $$compute{result_message} . "\n";
				} else {
					print $$compute{num_results} . " sequences processed\n";
				}

            # no other programs implemented for local execution
			} else {
				$errorMessage = "runLocalJobs: unrecognized program: \"$$compute{program_name}\".";
				return undef;
			}

            # save updated result
			$$resultsets{ $$compute{job_name} } = $compute;
			delete( $$jobs{ $jobid } );
		}
	}
	
    # return results
	return $resultsets;
}

# read config file and return array or required computes
sub getJobConfiguration {
	my ( $configDB, $onlycompute ) = @_;

	my $dbh = &connectSQLite( $configDB );
	if ( !defined $dbh ) {
		$errorMessage = "getJobConfiguration: " . $errorMessage;
		return undef;
	}
	
	my $sql = "select * from job_config";
	if ( defined $onlycompute ) {
		my $computelist = lc($onlycompute);
		$computelist =~ s/ *, */,/g;
		$computelist =~ s/,/','/g;
		$computelist = "'" . $computelist . "'";
		$sql .= " where lower(job_name) in ($computelist)";
	}
	$sql .= " order by job_name";

	my $computes = &querySQLArrayHash( $dbh, $sql );
	if ( !defined $computes ) {
		$errorMessage = "getJobConfiguration: " . $errorMessage;
		return undef;
	}

	$dbh->disconnect;	

	return $computes;
}

# update computes with project code, latest program and dataset details
sub prepareComputes {
	my ( $dbh, $computes, $subjectLibrary, $projectCode ) = @_;
	
	foreach my $compute ( @$computes ) {
		
        # get latest version of compute program
		$$compute{program_version} = &vicsGetProgramVersion( $$compute{program_name} );
		if ( $$compute{program_version} ) {
			$errorMessage = "prepareComputes: " . $errorMessage;
			return undef;
		}
		
        # get latest version of compute subject
		my $subject_db;
		if ( length($$compute{subject_db_name}) > 0 ) {
			$subject_db = &importExternalDataset( $dbh, $$compute{subject_db_name}, $subjectLibrary, $$compute{subject_db_name} );	
		} else {
			$subject_db = &getDatasetVersion( $dbh, 0 );
		}
		if ( !defined $subject_db ) {
			$errorMessage = "prepareComputes: " . $errorMessage;
			return undef;
		}
		$$compute{subject_db} = $subject_db;
		$$compute{subject_db_id} = $$subject_db{version_id};
		$$compute{subject_db_version} = $$subject_db{dataset_version};
		$$compute{subject_db_node} = $$subject_db{subject_node};
			
        # get latest version of compute query
		my $query_db;
		if ( length($$compute{query_db_name}) > 0 ) {
			$query_db = &getDatasetByName( $dbh, $$compute{query_db_name} );
		} else {
			$query_db = &getDatsetVersion( $dbh, 0 );
		}
		if ( !defined $query_db ) {
			$errorMessage = "prepareComputes: " . $errorMessage;
			return undef;
		}
		$$compute{query_db} = $query_db;
		$$compute{query_db_id} = $$query_db{version_id};
		$$compute{query_db_version} = $$query_db{dataset_version};
		$$compute{query_db_node} = $$query_db{query_node};
		
        # fill in grid project code
		$$compute{project_code} = $projectCode;
	}
	
	return $computes;
}

#
# compute which jobs need to be run given:
# computes - hash of required computes
# querysets - hash of available query datasets
# subjectsets - hash of available subject datasets
# resultsets - hash of existing compute results
sub scheduleJobs {
	my ( $dbh, $computes, $resultsets ) = @_;

    # check each required compute against the available results set
	my $jobs = ();
	foreach my $compute ( values %$computes ) {

        # get existing results
		my $result = $$resultsets{$$compute{job_name}};
		
        # check required computes against available results
        # no results - submit job
		if ( !defined $result  ) {

			$compute = &submitCompute ( $dbh, $compute );
			if ( !defined $compute ) {
				$errorMessage = "scheduleJobs: new job: " . $errorMessage;
				return undef;
			}
			push ( @$jobs, $compute );

        # invalid results - re-submit job
		} elsif ( $$result{status} eq "error"
					|| $$result{status} eq "aborted"
					|| $$result{status} eq "deleted" ) {

			$compute = &submitCompute ( $dbh, $compute );
			if ( !defined $compute ) {
				$errorMessage = "scheduleJobs: re-run job: " . $errorMessage;
				return undef;
			}
			push ( @$jobs, $compute );
				
        # stale results - abort and re-submit job
		} elsif ( $$compute{program_name} ne $$result{program_name}
					|| $$compute{program_version} ne $$result{program_version}
					|| $$compute{program_options} ne $$result{program_options}
					|| $$compute{subject_db_id} ne $$result{subject_db_id}
					|| $$compute{query_db_id} ne $$result{query_db_id} ) {
			if ( $$result{status} eq "running"
					|| $$result{status} eq "pending"
					|| $$result{status} eq "completed"
					|| $$result{status} eq "local" ) {
				if ( !defined &abortCompute( $dbh, $result, "Parameters are out of date." ) ) {
					$errorMessage = "scheduleJobs: refresh job:" . $errorMessage;
					return undef;
				}
			}

			$compute = &submitCompute ( $dbh, $compute );
			if ( !defined $compute ) {
				$errorMessage = "scheduleJobs: " . $errorMessage;
				return undef;
			}
			push ( @$jobs, $compute );

        # still running or completed but not loaded - requeue for continuation
		} elsif ( $$result{status} eq "running"
					|| $$result{status} eq "pending"
					|| $$result{status} eq "completed"
					|| $$result{status} eq "local" ) {

			$compute = &requeueCompute ( $dbh, $result );
			if ( !defined $compute ) {
				$errorMessage = "scheduleJobs: requeue job:" . $errorMessage;
				return undef;
			}
			push ( @$jobs, $compute );
			
        # unknown status
		} elsif ( $$result{status} ne "loaded" ) {

			$errorMessage = "scheduleJobs: unexpected compute status (\"" . $$result{status} . "\").";
			return undef;
		} 

	}

    # return array of jobs on queue (submitted or continued jobs)
	foreach my $job ( @$jobs ) {
		$$job{retries} = 0;
	}
	
	return $jobs;
}
