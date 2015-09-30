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

require Exporter;
@ISA = qw(Exporter);
use warnings;
use strict;
use SOAP::Lite;
use File::Basename;
use Data::Dumper;
our $errorMessage;

# url for services
our $vicsWSDL;
#my $defaultVicsWsdl = "http://saffordt-ws1:8180/compute-compute/ComputeWS?wsdl";
my $defaultVicsWsdl = "http://camdev3.jcvi.org:8180/compute-compute/ComputeWS?wsdl";

# job queue and optional handle to database where job queue is persisted  
my $jobQueue;

###############################################################################
#Added this sub for running iprscan webservice nikhat
sub vicsSubmitIprscan {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runInterProScan( @$args );
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "iprscan", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitIprscan: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitIprscan: " . $errorMessage;
		return undef;
	}
	
	return $job;
}


# submit a blastN job
sub vicsSubmitBlastN {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runBlastN( @$args );
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "blastn", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitBlastN: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitBlastN: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit a blastP job
sub vicsSubmitBlastP {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runBlastP( @$args );

	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	
	my $job = &_makeJob( $job_id, "blastp", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitBlastP: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitBlastP: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit a TBlastN job
sub vicsSubmitTBlastN {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runTBlastN( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitTBlastN: runTBlastN service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitTBlastN: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "tblastn", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitTBlastN: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitTBlastN: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit am Hmmpfam job
sub vicsSubmitHmmpfam {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );

	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runHmmpfam( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitHmmpfam: runHmmpfam service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitHmmpfam: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "hmmpfam", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitHmmpfam: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitHmmpfam: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit an RPSBlast job
sub vicsSubmitRPSBlast {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runReversePsiBlast( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitRPSBlast: runReversePsiBlast service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitRPSBlast: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	my $job = &_makeJob( $job_id, "rpsblast", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitRPSBlast: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitRPSBlast: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit a Priam job
sub vicsSubmitPriam {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runPriam( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitPriam: runPriam service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitPriam: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "priam", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitPriam: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitPriam: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit a Tmhmm job
sub vicsSubmitTmhmm {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );

	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runTmhmm( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitTmhmm: runTmhmm service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitTmhmm: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "tmhmm", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitTmhmm: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitTmhmm: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit a Signalp job
sub vicsSubmitSignalp {
	my ( $params ) = @_;
	
	my $args = &_extractWSDLArgs( $params );

	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> runSignalp( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsSubmitSignalp: runSignalp service failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsSubmitSignalp: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}
	
	my $job = &_makeJob( $job_id, "signalp", $params );
	if ( !defined $job ) {
		$errorMessage = "vicsSubmitSignalp: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsSubmitSignalp: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# submit getVmapAnnotation
sub vicsGetVmapAnnotation {
	my ( $userlogin, $sqlitedb, $outputpath, $projectcode ) = @_;

	my $cmdopts = "-D $sqlitedb -O $outputpath";
	my @tmp_params = (
		"username", $userlogin, undef,
		"token", "0", undef,
		"projectCode", $projectcode, undef,
		"workSessionId", undef, undef,
		"jobName", "Annotation of $sqlitedb", undef,
		"serviceName", "getVmapAnnotation", undef,
		"serviceOptions", $cmdopts, undef,
		"gridOptions", "medium", undef );
	my $params = _buildParamsArray( \@tmp_params );
	my $args = &_extractWSDLArgs( $params );
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> genericService( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsGetVmapAnnotation: genericService failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsGetVmapAnnotation: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}

	my $job = &_makeJob( $job_id, "getVmapAnnotation", $params, $userlogin );
	if ( !defined $job ) {
		$errorMessage = "vicsGetVmapAnnotation: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsGetVmapAnnotation: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

# run the com2GO service
sub vicsCom2GO {
	my ( $userlogin, $annotationfile, $ifield, $cfield, $outdest, $projectcode ) = @_;

# -F required, full path to tab-delimited annotation file
# -C required, column number of common name data (first column=1)
# -I required, column number of sequence identifier (first column=1)
# -D required, full path for the tgz results file to be generated

	my $cmdopts = "-F $annotationfile -I $ifield -C $cfield -D $outdest";
	my @tmp_params = (
		"username", $userlogin, undef,
		"token", "0", undef,
		"projectCode", $projectcode, undef,
		"workSessionId", undef, undef,
		"jobName", "GO mapping for $annotationfile", undef,
		"serviceName", "com2GO", undef,
		"serviceOptions", $cmdopts, undef,
		"gridOptions", "medium", undef );
	my $params = _buildParamsArray( \@tmp_params );
	my $args = &_extractWSDLArgs( $params );

	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> genericService( @$args );
	if ( !defined $message ) {
		$errorMessage = "vicsCom2GO: genericService failed args: <" . join( "> <", @$args ) . ">";
		return undef;
	}
	
	my $job_id;
	if ( ! $message =~ /^Job Id: / ) {
		$errorMessage = "vicsCom2GO: " . $message;
		return undef;
	} else {
		( $job_id ) = split ( /\n/, $message );
		$job_id =~ s/^Job Id: //;
	}

	my $job = &_makeJob( $job_id, "vicsCom2GO", $params, $userlogin );
	if ( !defined $job ) {
		$errorMessage = "vicsCom2GO: " . $errorMessage;
		return undef;
	}
	
	if ( !defined &_queueJob( $job ) ) {
		$errorMessage = "vicsCom2GO: " . $errorMessage;
		return undef;
	}
	
	return $job;
}

#
# restore a detached job to queue
sub vicsRequeueJob {
	my ( $job_id, $job_type, $job_name, $job_owner, $date_submitted ) = @_;
#	my $job = &vicsRequeueJob( $$compute{compute_id}, $$compute{compute_program_name}, $$compute{compute_name},
#		$$compute{submitted_by}, $$compute{date_submitted} );

	my @tmp_params = (
		"username",	$job_owner,		undef,
		"jobName",	$job_name,		undef);
	my $params = &_buildParamsArray( \@tmp_params );

	my $job = &_makeJob( $job_id, $job_type, $params );
	$$job{date_submitted} = $date_submitted;

	_queueJob( $job );
	
	return $job;	
}

#
#  get job message
sub vicsGetJobMessage {
	my ( $jobid, $jobtype, $jobowner ) = @_;
	if ( !defined $jobowner ) { $jobowner = getlogin || getpwuid($>) || 'ccbuild' }
#print "job Id: $jobid\n";
	my $message =  SOAP::Lite -> service( getVicsWsdl() ) -> getTaskStatus( $jobowner, '0', $jobid );
#print "vicGetJobMessage<<STOP$message\nSTOP\n";	
	if ( ! $message =~ /StatusType/ ) {
		$errorMessage = "vicsGetJobMessage: unexpected response: " . $message;
		return undef;
	}
	$message =~ s/StatusType\:\s*\n//;
	$message =~ s/\n\s*\n/\n/g;

	if ( !defined $message ) {
		$errorMessage = "vicsGetJobMessage: null response from VICS.";
		return undef;
	}
	
	return $message;
}

###############################################################################
# return a specific list of job from queue
# if no jobs specified retrieve all jobs
sub vicsGetJobsFromQueue {
	my @jobids  = @_;
	if ( scalar @jobids > 0 ) {
		foreach my $jobid ( @jobids ) {
			if ( !exists $$jobQueue{$jobid} ) {
				$errorMessage = "vicsGetJobsFromQueue: no such job.";
				return undef;
			}
		}
	} else {		
		@jobids = keys %$jobQueue;
	}

	my @jobs = ();
	foreach my $jobid ( @jobids ) {
		push ( @jobs, $$jobQueue{$jobid} );
	}
	return \@jobs;
}

sub vicsGetJobFromQueue {
	my ( $jobid ) = @_;
	
	my $jobs = &vicsGetJobsFromQueue( $jobid );
	if ( !defined $jobs ) {
		$errorMessage = "vicsGetJobFromQueue: " . $errorMessage;
		return undef;		
	} elsif ( scalar @$jobs == 0 ) {
		$errorMessage = "vicsGetJobFromQueue: no such job.";
		return undef;
	}	
	
	return $$jobs[0];
}

###############################################################################
# wait for specific job to complete
# returns the job with status and result when it completes
sub vicsWaitForJob {
	my ( $jobid ) = @_;
	if ( !defined $jobid ) {
		$errorMessage = "vicsWaitForJob: no job id specified.";
		return undef;
	}
	my $result = &_waitForJobSet( 1, $jobid );
	return $$result[0];
}

###############################################################################
# wait for any of the jobs in a list to complete
# if no jobs listed check ALL jobs in active queue
#
# returns array of completed jobs with status and result
# as soon any completed jobs are found
sub vicsWaitForAnyJobs {
	my @jobids = @_;
	my $result = &_waitForJobSet( 0, @jobids );
	if ( !defined $result ) {
		$errorMessage = "vicsWaitForAnyJobs: " . $errorMessage;
		return undef;
	}
	return $result;
}

###############################################################################
# wait for any of the jobs in a list to complete
# if no jobs listed check ALL jobs in active queue
#
# returns the first completed job with status and result
# as soon as a completed job is found
# returns an "empty" job if queue is empty
sub vicsWaitForAnyJob {
	my @jobids = @_;
	my $result = &waitForJobSet( 0, @jobids );
	if ( !defined $result ) {
		$errorMessage = "vicsWaitForAnyJob: " . $errorMessage;
		return undef;
	}
	if ( scalar @$result == 0 ) {
		return &_emptyJob;
	} else {
		return $$result[0];
	}
}

###############################################################################
# wait for all of the jobs in a list to complete
# if no jobs listed check ALL jobs in active queue
#
# returns array of completed jobs with status and result
# when all jobs have comepleted
sub vicsWaitForAllJobs {
	my @jobids = @_;
	my $result = &_waitForJobSet( 1, @jobids );
	if ( !defined $result ) {
		$errorMessage = "vicsWaitForAllJobs: " . $errorMessage;
		return undef;
	}
	return $result;
}

###############################################################################
# delete task
#
sub vicsDeleteTask {
	my ( $username, $taskid ) = @_;

	my $message = 
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> deleteTaskById( $username, "0", $taskid );
	if ( !defined $message ) {
		$errorMessage = "vicsDeleteTask failed";
		return undef;
	}
	
	return $message;	
}

###############################################################################
# load a fasta file
sub vicsUploadFasta {
	my ( $fastafile, $owner ) = @_;
	
	my $file_owner = $owner;
	if ( !defined $file_owner || length( $file_owner) == 0  ) {
		$file_owner = getlogin || getpwuid($>) || 'ccbuild';
	}
	my $tmpfasta = "/usr/local/scratch/tmp.$$.fasta";
	system( "sed 's/\t/ /g' <$fastafile >$tmpfasta" );

	my $message = 
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> uploadFastaFileToSystem( $file_owner, "0", undef, $tmpfasta );

	unlink $tmpfasta;
	if ( $message =~ /^File id: / ) {
		my ( $file_id ) = split ( /\n/, $message );
		$file_id =~ s/^File id: //;
		return $file_id;
	} else {
		$errorMessage = "vicsUploadFasta: " . $message;
		return undef;
	}
}

###############################################################################
# load a fasta file as a blastable database
sub vicsBlastIndexFasta {
	my ( $name, $description, $fastafile, $owner) = @_;
	
	my $db_owner = $owner;
	if ( !defined $db_owner || length( $db_owner) == 0  ) {
		$db_owner = getlogin || getpwuid($>) || 'ccbuild';
	}
	my $message =
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> uploadAndFormatBlastDataset( $db_owner, undef, undef, $name, $description, $fastafile );
	if ( $message =~ /Check status of job/ ) {
		( undef, $message ) = split( /\n/, $message );
		$message =~ s/Check status of job //;
		( my $taskid, undef ) = split ( /\s/, $message );

		my $job = &vicsRequeueJob( $taskid, "blastindex", $name, $db_owner, &now() );
		if ( !defined $job ) {
			$errorMessage = "vicsBlastIndexFasta: " . $errorMessage;
			return undef;
		}
		$job = &vicsWaitForJob( $taskid );
		if ( !defined $job ) {
			$errorMessage = "vicsBlastIndexFasta: " . $errorMessage;
			return undef;
		}
		my $node_id = basename( $$job{result_message} );

		return $node_id;
	} else {
		$errorMessage = "vicsBlastIndexFasta: " . $message;
		return undef;
	}	
}

###############################################################################
# return a list of available blast databases
sub vicsListBlastDatabases {
	my ( $username ) = @_;
	if ( !defined $username ) { $username = getlogin || getpwuid($>) || 'ccbuild' }
	
	my $list = 
		SOAP::Lite
			-> service( getVicsWsdl() )
			-> getBlastDatabaseLocations( $username, "" );
	
	my @dbs = ();
	my @lines = split( /\n/, $list );
	foreach my $line ( @lines ) {
		$line =~ s/\n//;
		my ( $node_id, $name ) = split( /\t/, $line );
		my %db;
		$db{node_id} = $node_id;
		$db{name} = $name;
		push( @dbs, \%db );	
	}
	return \@dbs;
}
###############################################################################
# get current version of program used by vics
# (service not currently implemented)
sub vicsGetProgramVersion {
	my ( $program_name ) = @_;
	return "0";	
}

###############################################################################
# set a named parameter value
sub vicsSetParam {
	my ( $params, $name, $value ) = @_;
	
	my $i = &_findParam( $params, $name );
	if ( $i >=0 ) {
		$$params[$i]{value} = $value;
		return $i;
	} else {
		$errorMessage= "vicsSetParam: unknown parameter: \"" . $name . "\".";
	}
}

###############################################################################
# get a named parameter value
sub vicsGetParam {
	my ( $params, $name ) = @_;
	
	my $i = &_findParam( $params, $name );
	if ( $i >=0 ) {
		my $value = $$params[$i]{value};
		if ( defined $value ) {
			return $value;
		} else {
			return "";
		}
	} else {
		$errorMessage= "vicsGetParam: unknown parameter: \"" . $name . "\".";
		return undef;
	}
}

###############################################################################
# return blastN parameters with default values
sub vicsParamsBlastN {

	my @tmp_params = (
		"username",						getlogin || getpwuid($>) || 'ccbuild',	undef,
		"token",						"0",		undef,
		"project",						undef,		undef,
		"worksession_id",				undef,		undef,
		"jobName",						undef,		undef,
		"subjectDBIdentifier",			undef,		undef,
		"queryFastaFileNodeId",			undef,		undef,
		"databaseAlignmentsPerQuery",	250,		"-b",
		"filter",						"T",		"-F",
		"eValueExponent",				1,			"-e",
		"lowercaseFiltering",			0,			"-U",
		"believeDefline",				0,			"-J",
		"databaseSize",					0,			"-z",
		"gapExtendCost",				-1,			"-E",
		"gappedAlignment",				1,			"-g",
		"hitExtensionThreshold",		0,			"-f",
		"matrix",						"BLOSUM62",	"-M",
		"multihitWindowSize",			0,			"-A",
		"searchStrand",					"both",		"-S",
		"ungappedExtensionDropoff",		0,			"-y",
		"bestHitsToKeep",				0,			"-K",
		"finalGappedDropoff",			0,			"-Z",
		"gapOpenCost",					-1,			"-G",
		"gappedAlignmentDropoff",		0,			"-X",
		"matchReward",					1,			"-r",
		"mismatchPenalty",				-3,			"-q",
		"searchSize",					0,			"-Y",
		"showGIs",						0,			"-I",
		"wordsize",						0,			"-W",
		"formatTypesCsv",	  		    "xml",      "-m"
		);
		
	return &_buildParamsArray( \@tmp_params );
}

### added sub to process params for iprscan
sub vicsParamsIprscan {

	my @tmp_params = (
		"username",         getlogin || getpwuid($>) || 'ccbuild',    undef,
		"token",            "0",		 undef,
		"project",			undef,		 undef,
		"worksession_id",	undef,		 undef,
		"jobName",			undef,		 undef,
		"fastaInputNodeId",	undef,		 undef,
		"goterms",			"-goterms",  "-goterms",
        "iprlookup",		"-iprlookup","-iprlookup",
        "format",			"raw",	     "raw",
		);
		
	return &_buildParamsArray( \@tmp_params );
}



###############################################################################
# return TBlastN parameters with default values
sub vicsParamsTBlastN {

	my @tmp_params = (
		"username",						getlogin || getpwuid($>) || 'ccbuild',	undef,
		"token",						"0",			undef,
		"project",						undef,		undef,
		"worksession_id",				undef,		undef,
		"jobName",						undef,		undef,
		"subjectDBIdentifier",			undef,		undef,
		"queryFastaFileNodeId",			undef,		undef,
		"databaseAlignmentsPerQuery",	250,		"-b",
		"filter",						"T",		"-F",
		"eValueExponent",				1,			"-e",
		"lowercaseFiltering",			0,			"-U",
		"believeDefline",				0,			"-J",
		"databaseSize",					0,			"-z",
		"gapExtendCost",				-1,			"-E",
		"gappedAlignment",				1,			"-g",
		"hitExtensionThreshold",		0,			"-f",
		"multihitWindowSize",			0,			"-A",
		"showGIs",						0,			"-I",
		"wordsize",						0,			"-W",
		"bestHitsToKeep",				0,			"-K",
		"finalGappedDropoff",			0,			"-Z",
		"gapOpenCost",					-1,			"-G",
		"gappedAlignmentDropoff",		0,			"-X",
		"matrix",						"BLOSUM62",	"-M",
		"searchSize",					0,			"-Y",
		"ungappedExtensionDropoff",		0,			"-y",
		"formatTypesCsv",	  		    "xml",      "-m"
		);
		
	return &_buildParamsArray( \@tmp_params );
}
###############################################################################
# return blastP parameters with default values
sub vicsParamsBlastP {

	my @tmp_params = (
		"username",						getlogin || getpwuid($>) || 'ccbuild',	undef,
		"token",						"0",			undef,
		"project",						undef,		undef,
		"worksession_id",				undef,		undef,
		"jobName",						undef,		undef,
		"subjectDBIdentifier",			undef,		undef,
		"queryFastaFileNodeId",			undef,		undef,
		"databaseAlignmentsPerQuery",	250,		"-b",
		"filter",						"T",		"-F",
		"eValueExponent",				1,			"-e",
		"lowercaseFiltering",			0,			"-U",
		"believeDefline",				0,			"-J",
		"databaseSize",					0,			"-z",
		"gapExtendCost",				-1,			"-E",
		"gappedAlignment",				1,			"-g",
		"hitExtensionThreshold",		0,			"-f",
		"multihitWindowSize",			0,			"-A",
		"showGIs",						0,			"-I",
		"wordsize",						0,			"-W",
		"bestHitsToKeep",				0,			"-K",
		"finalGappedDropoff",			0,			"-Z",
		"gapOpenCost",					-1,			"-G",
		"gappedAlignmentDropoff",		0,			"-X",
		"matrix",						"BLOSUM62",	"-M",
		"searchSize",					0,			"-Y",
		"ungappedExtensionDropoff",		0,			"-y",
		"formatTypesCsv",	  		    "xml",      "-m"
		);
		
	return &_buildParamsArray( \@tmp_params );
}

sub vicsParamsHmmpfam {
	
	my @tmp_params = (
		"username",					getlogin || getpwuid($>) || 'ccbuild',		undef,
		"token",					"0",			undef,
		"project",					undef,			undef,
		"workSessionId",			undef,			undef,
		"jobName",					undef,			undef,
		"subjectDBIdentifier",		undef,			undef,
		"queryFastaFileNodeId",		undef,			undef,
		"maxBestDomainAligns",		0,				"-A",
		"evalueCutoff",				10.0,			"-E",
		"tbitThreshold",			-1000000.0,		"-T",
		"zModelNumber",				"59021",		"-Z",
		"useHmmAccessions",			0,				"--acc",
		"cutGa",					0,				"--cut_ga",
		"cutNc",					0,				"--cut_nc",
		"cutTc",					0,				"--cut_tc",
		"domE",						"1.0E9",		"--domE",
		"domT",						-1000000.0,		"--domT",
		"null2",					0,				"--null2"
		);

	return &_buildParamsArray( \@tmp_params );
}

sub vicsParamsRPSBlast {
	
	my @tmp_params = (
		"username",						getlogin || getpwuid($>) || 'ccbuild',	undef,
		"token",						"0",			undef,
		"project",						undef,		undef,
		"worksession_id",				undef,		undef,
		"jobName",						undef,		undef,
		"subjectDBIdentifier",			undef,		undef,
		"queryFastaFileNodeId",			undef,		undef,
		"eValueExponent",				1,			"-e",
		"blastExtensionDropoffBits",	7,			"-y",
		"believeDefline",				0,			"-J",
		"showGIsInDeflines",			0,			"-I",
		"lowercaseFiltering",			0,			"-U",
		"forceLegacyBlastEngine",		0,			"-V",
		"filterQueryWithSEG",			0,			"-F",
		"gappedAlignmentDropoff",		15,			"-X",
		"bitsToTriggerGapping",			22,			"-N",
		"finalGappedAlignmentDropoff",	25,			"-Z",
		"databaseAlignmentsPerQuery",	250,		"-b"
		);
#		"databaseAlignmentsPerQuery",	250,		"-b",
#		"formatTypesCsv",	  		    7,          "-m"
#		);

	return &_buildParamsArray( \@tmp_params );
}

sub vicsParamsPriam {
	
	my @tmp_params = (
		"username",					getlogin || getpwuid($>) || 'ccbuild',		undef,
		"token",					"0",			undef,
		"project",					undef,			undef,
		"worksessionId",			undef,			undef,
		"jobName",					undef,			undef,
		"inputFastaFileNodeId",		undef,			undef,
		"rpsblast_options",			undef,			undef,
		"max_evalue",				"1e-10",		"-e"
		);

	return &_buildParamsArray( \@tmp_params );
}

sub vicsParamsTmhmm {
	
	my @tmp_params = (
		"username",					getlogin || getpwuid($>) || 'ccbuild',		undef,
		"token",					"0",			undef,
		"project",					undef,			undef,
		"workSessionId",			undef,			undef,
		"jobName",					undef,			undef,
		"fastaInputNodeId",		    undef,			undef,
        "html",                     "0",            '--html',
        "short",                    "0",            '--short',
        "plot",                     "0",            '--plot',
        "v1",                       "0",            '--v1',
		);

	return &_buildParamsArray( \@tmp_params );
}

sub vicsParamsSignalp {
	
	my @tmp_params = (
		"username",					getlogin || getpwuid($>) || 'ccbuild',		undef,
		"token",					"0",			undef,
		"project",					undef,			undef,
		"workSessionId",			undef,			undef,
		"jobName",					undef,			undef,
		"fastaInputNodeId",		    undef,			undef,
		"type_of_organism",		    undef,			'-t',
		"format",	    			undef,          '-f',
		"method",		    		undef,          '-m',
		"truncate_length",      	undef,          '-trunc'
		);

	return &_buildParamsArray( \@tmp_params );
}

###############################################################################
# internal subroutines
###############################################################################
# format parameter data
sub _buildParamsArray {
	my ( $tmp_params ) = @_;
	

	my @params = ();
	my $i = 0;
	while ( $i < scalar @$tmp_params ) {
		my $name = $$tmp_params[$i];
		my $value = $$tmp_params[$i+1];
		my $altname = $$tmp_params[$i+2];
		my %param;
		$param{name} = $name;
		$param{value} = $value;
		$param{altname} = $altname;
		push ( @params, \%param );
		$i += 3; 
	}
	
	return \@params;
}

###############################################################################
# retrieve ordered array of parameter values
sub _extractWSDLArgs {
	
	my ( $params ) = @_;
	
	my @args = ();
	foreach my $param ( @$params ) {
		push ( @args, $$param{value} );
	}
	
	return \@args;
}

###############################################################################
# find position of named parameter in array
sub _findParam {
	my ( $params, $name ) = @_;

    my $i = (scalar @$params) - 1;
    while ( $i >= 0 ) { 
        last if (defined $$params[$i]{name})    && ($$params[$i]{name} eq $name);
        last if (defined $$params[$i]{altname}) && ($$params[$i]{altname} eq $name);
        $i--;       
    }
    return $i;

}

###############################################################################
# update job status
sub _updateJobStatus {
	my ( $job ) = @_;
	my $message = &vicsGetJobMessage( $$job{id}, $$job{type}, $$job{owner} );
print "MESSAGE:$message\n";
	if ( !defined $message ) {
		$errorMessage = "_updateJobStatus: " . $errorMessage;
		return undef;
	} elsif ($message =~ /problem/) {
        $errorMessage = "_updateJobStatus: " . $message;
        return undef;
    }

	my ( undef, $status, $detail, $result ) = split( /\n/, $message );
	( undef, $status ) = split( /: /, $status );
	my ( $result_type, $result_message ) = split( /: /, $result );

	if ( $status =~ /postprocess/i ) { $status = "running" }
	elsif ( $status =~ /created/i ) { $status = "pending" }
	elsif ( $status =~ /not applicable/i ) { $status = "error" }
	elsif ( $status =~ /deleted/i ) { $status = "error" }
	elsif ( $status =~ /canceled/i ) { $status = "error" }
	elsif ( $status =~ /merging/i ) { $status = "running" }
	elsif ( $status =~ /format/i ) { $status = "running" }

	$$job{status} = $status;
	
	if ( $status eq "completed" ) {
		$$job{result_type} = $result_type;
		$$job{result_message} = $result_message;
	} elsif ( $status eq "running" || $status eq "pending" ) {
	} else {
		$$job{result_type} = "error message";
		$$job{result_message} = $message;
	} 
	return $job;
}

###############################################################################
# build and return a job hash
sub _makeJob {
	my ( $jobid, $jobtype, $jobparams, $jobowner ) = @_;
	
	my $job;

	my $owner = $jobowner;
	if ( !defined $owner ) { $owner = &vicsGetParam($jobparams,"username") }
	if ( !defined $owner ) { $owner = getlogin || getpwuid($>) || 'ccbuild' }

	$$job{id} = $jobid;
	$$job{type} = $jobtype;
	$$job{name} = &vicsGetParam($jobparams,"jobName");
	$$job{owner} = $owner;
	$$job{parameters} = $jobparams;
	return $job;
}
###############################################################################
# return an "empty" job hash
sub _emptyJob {
	my %job;
	$job{id} = undef;
	return \%job;
}

###############################################################################
# wait for the jobs in a list to complete
#
# an empty list of job ids implies all jobs in active job queue 
sub _waitForJobSet {
	my ( $wait_for_all, @jobids ) = @_;

# validate the jobs in the user provided list
	if ( scalar @jobids > 0 ) {
		foreach my $jobid ( @jobids ) {
			if ( !exists $$jobQueue{$jobid} ) {
				$errorMessage = "_waitForJobSet: no such job.";
				return undef;
			}
		}
	} else {
		@jobids = keys %{$$jobQueue};
	}

# are there any jobs to check?
	my @completed = ();
	my $numjobs = scalar @jobids;
	if ( $numjobs == 0 ) { return 0 }
	
# remove completed jobs from active job queue
# and add them to the return set
	my $totalsleep = 0;
	my $sleeptime = 1;
	my $done = 0;
	while ( !$done ) {
		my $i = 0;
		while ( $i < $numjobs ) {
			my $jobid = $jobids[$i];
			my $job = $$jobQueue{$jobid};
			
			if ( $$jobQueue{$jobid}{status} ne "running"
					&& $$jobQueue{$jobid}{status} ne "pending") {
				push( @completed, $job );
				if ( !defined &_dequeueJob( $job ) ) {
					$errorMessage = "_waitForJobSet: " . $errorMessage;
					return undef;
				}
				$numjobs--;
				$jobids[$i] = $jobids[$numjobs];

# at least one job completed, reset the sleep timer to increase 
# the polling rate in case other jobs are nearing completion 
				if ( $totalsleep > 300 ) { $totalsleep = 300 };
			} else {
				$i++;
			}
		}
		
# have we met the return condition (any vs all)?
		if ( $numjobs==0 ) { $done = 1 }
		elsif ( !$wait_for_all && scalar @completed>0 ) { $done = 1 }

# sleep before refreshing status
# slowly increase sleeptime so we don't overwhelm server while polling job status
		if ( !$done ) {
			sleep $sleeptime;
			$totalsleep += $sleeptime;
			if ( $totalsleep < 300 ) {			# every five seconds for 5 minutes
				$sleeptime = 5;
			} elsif ( $totalsleep < 900 ) {		# every 10 seconds for 1st 15 minutes
				$sleeptime = 10;
			} elsif ( $totalsleep < 7200 ) {	# every 30 seconds for 1st 2 hours
				$sleeptime = 30
			} elsif ( $totalsleep < 24*3600 ) {	# every 2 minutes for 1st day
				$sleeptime = 120;
			} else {
				$sleeptime = 600;				# every 10 minutes after 1st day
			}

# refresh status
			foreach my $i ( 0..$numjobs-1 ) {
				my $jobid = $jobids[$i];
				if ( !defined &_updateJobStatus( $$jobQueue{$jobid} ) ) {
					$errorMessage = "_waitForJobSet: " . $errorMessage;
					return undef;
				}
			}
		}
	}

# return array of completed jobs
	@completed = sort { $$a{id}<=>$$b{id} } @completed;
	return \@completed;
}

###############################################################################
# manage job queue
sub _queueJob {
	my ( $job ) = @_;
	if ( !defined $job ) {
		$errorMessage = "_queueJob: undefined job.";
		return undef;
	} elsif ( !exists $$jobQueue{$$job{id}} ) {
		if ( !defined $$job{date_submitted} ) { $$job{date_submitted} = &now };
		$$job{owner} = getlogin || getpwuid($>) || 'ccbuild';
		$job = _updateJobStatus( $job );
		if ( !defined $job ) {
			$errorMessage = "_queueJob: " . $errorMessage;
			return undef;
		}
		$$jobQueue{$$job{id}} = $job;
		
	} else {
		$errorMessage = "_queueJob: job already queued.";
		return undef;
	}
	
	return $job;
}

sub _queueJobs {
	my ( $jobs ) = @_;
	
	foreach my $job ( @$jobs ) {
		if ( !defined &_queueJob( $job ) ) {
			$errorMessage = "_queueJobs: " . $errorMessage;
			return undef;
		}
	}
}

sub _dequeueJob {
	my ( $job ) = @_;

	if ( exists $$jobQueue{$$job{id}} )	{
		delete( $$jobQueue{$$job{id}} );
	} else {
		$errorMessage = "_dequeueJob: job not on queue.";
		return undef;
	}
}

sub _dequeueJobs {
	my ( $jobs ) = @_;
	
	foreach my $job ( @$jobs ) {
		if ( !defined &_dequeueJob( $job ) ) {
			$errorMessage = "_dequeueJobs: " . $errorMessage;
			return undef;
		} 
	}
}

sub getVicsWsdl {
	if ( !defined $vicsWSDL ) { $vicsWSDL = $defaultVicsWsdl }
	return $vicsWSDL;
}
1;
