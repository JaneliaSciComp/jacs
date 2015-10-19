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
our $myLib;
our $errorMessage;
our $filestore = "Xruntime-shared/filestore";
our $scratchspace = "/usr/local/scratch";
our $sortsize = 100000;
our $insertsize = 10000;
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::db;
use EAP::vics;
use Switch;

###################################################################
# compute utilities
###################################################################
#
# return latest result set for each compute
sub getCurrentResultSets {
	my ( $dbh ) = @_;

	my $currentResults = &querySQLArrayHash( $dbh,
		"select * from compute where is_obsolete=0" );
	if ( !defined $currentResults ) {
		$errorMessage = "getCurrentResultSets: " . $errorMessage;
		return undef;
	}
	
	foreach my $result ( @$currentResults ) {
		my $subject_db = &getDatasetVersion( $dbh, $$result{subject_db_id} );
		if ( !defined $subject_db ) {
			$errorMessage = "getCurrentResultSets: subject: " . $errorMessage;
		}
		$$result{subject_db} = $subject_db;

		my $query_db = &getDatasetVersion( $dbh, $$result{query_db_id} );
		if ( !defined $query_db ) {
			$errorMessage = "getCurrentResultSets: query: " . $errorMessage;
		}
		$$result{query_db} = $query_db;
	}
	
	return $currentResults;
}

#
# return all result sets
sub getAllResultSets {
	my ( $dbh ) = @_;

	my $allResults = &querySQLArrayHash( $dbh,
		"select * from compute");
	if ( !defined $allResults ) {
		$errorMessage = "getAllResultSets: " . $errorMessage;
		return undef;
	}
	
	foreach my $result ( @$allResults ) {
		my $subject_db = &getDatasetVersion( $dbh, $$result{subject_db_id} );
		if ( !defined $subject_db ) {
			$errorMessage = "getAllResultSets: subject: " . $errorMessage;
		}
		$$result{subject_db} = $subject_db;

		my $query_db = &getDatasetVersion( $dbh, $$result{query_db_id} );
		if ( !defined $query_db ) {
			$errorMessage = "getAllResultSets: query: " . $errorMessage;
		}
		$$result{query_db} = $query_db;
	}
	
	return $allResults;
}

#
# submit a compute via VICS services
sub submitCompute {
	my ( $dbh, $compute ) = @_;

# fill in paramaters and submit job via VICS services
	
# blastp
	if ( $$compute{program_name} =~ /^blastp$/i ) {
		my $params = &vicsParamsBlastP;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "subjectDBIdentifier", $$compute{subject_db_node} );
		&vicsSetParam( $params, "queryFastaFileNodeId", $$compute{query_db_node} );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		my $job = &vicsSubmitBlastP( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

# blastn
	} elsif ( $$compute{program_name} =~ /^blastn$/i ) {
		my $params = &vicsParamsBlastN;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "subjectDBIdentifier", $$compute{subject_db_node} );
		&vicsSetParam( $params, "queryFastaFileNodeId", $$compute{query_db_node} );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		my $job = &vicsSubmitBlastN( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

# tblastn
	} elsif ( $$compute{program_name} =~ /^tblastn$/i ) {
		my $params = &vicsParamsTBlastN;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "subjectDBIdentifier", $$compute{subject_db_node} );
		&vicsSetParam( $params, "queryFastaFileNodeId", $$compute{query_db_node} );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		my $job = &vicsSubmitTBlastN( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

# rpsblast
	} elsif ( $$compute{program_name} =~ /^rpsblast$/i ) {
		my $params = &vicsParamsRPSBlast;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "subjectDBIdentifier", $$compute{subject_db_node} );
		&vicsSetParam( $params, "queryFastaFileNodeId", $$compute{query_db_node} );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		my $job = &vicsSubmitRPSBlast( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

# priam
	} elsif ( $$compute{program_name} =~ /^priam$/i ) {
		my $params = &vicsParamsPriam;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "inputFastaFileNodeId", $$compute{query_db_node} );
#		&vicsSetParam( $params, "max_evalue", "1e-10" );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		my $job = &vicsSubmitPriam( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

# hmmpfam
	} elsif ( $$compute{program_name} =~ /^hmmpfam$/i ) {
		my $params = &vicsParamsHmmpfam;
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "subjectDBIdentifier", $$compute{subject_db_node} );
		&vicsSetParam( $params, "queryFastaFileNodeId", $$compute{query_db_node} );

		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		

		my $job = &vicsSubmitHmmpfam( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};
# iprscan
	} elsif ( $$compute{program_name} =~ /^IPRSCAN$/i ) {
		my $params = &vicsParamsIprscan;
		
		&vicsSetParam( $params, "project", $$compute{project_code} );
		&vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "fastaInputNodeId", $$compute{query_db_node} );
		if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		my $job = &vicsSubmitIprscan( $params );
		if ( !defined $job ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		
		$$compute{job_id} = $$job{id};
		$$compute{status} = $$job{status};

    # signalp
    } elsif ( $$compute{program_name} =~ /^signalp/i ) {
        my $params = &vicsParamsSignalp;
        &vicsSetParam( $params, "project", $$compute{project_code} );
        &vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "fastaInputNodeId", $$compute{query_db_node} );

        if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
            $errorMessage = "submitCompute: " . $errorMessage;
            return undef;
        }


        my $job = &vicsSubmitSignalp( $params );
        if ( !defined $job ) {
            $errorMessage = "submitCompute: " . $errorMessage;
            return undef;
        }

        $$compute{job_id} = $$job{id};
        $$compute{status} = $$job{status};

    # tmhmm
    } elsif ( $$compute{program_name} =~ /^tmhmm/i ) {
        my $params = &vicsParamsTmhmm;
        &vicsSetParam( $params, "project", $$compute{project_code} );
        &vicsSetParam( $params, "jobName", $$compute{job_name} );
		&vicsSetParam( $params, "fastaInputNodeId", $$compute{query_db_node} );
        if ( !defined &_setProgramOptions( $params, $$compute{program_options} ) ) {
            $errorMessage = "submitCompute: " . $errorMessage;
            return undef;
        }
        my $job = &vicsSubmitTmhmm( $params );
        if ( !defined $job ) {
            $errorMessage = "submitCompute: " . $errorMessage;
            return undef;
        }

        $$compute{job_id} = $$job{id};
        $$compute{status} = $$job{status};

# not a recognized VICS service,
# mark jobs for local execution
	}else {
		if ( !defined &openSequence( $dbh, "job_id" ) ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		$$compute{job_id} = &nextSequenceValue( "job_id" );
		if ( !defined $$compute{job_id} ) {
			$errorMessage = "submitCompute: " . $errorMessage;
			return undef;
		}
		&closeSequence( "job_id" );
		$$compute{status} = "local";
	}


# mark old results obsolete
	my $obsolete = &executeSQL( $dbh, "update job set is_obsolete=? where is_obsolete=? and job_name=?",
		1, 0, $$compute{job_name} );
	if ( !defined $obsolete ) {
		$errorMessage = "submitCompute: " . $errorMessage;
		return undef;
	}
	
# save compute to database
	if ( $$compute{status} eq "local" ) {
		$$compute{date_submitted} = " ";	# date_submitted will be set when job runs locally
	} else {
		$$compute{date_submitted} = &now;
	}
	$$compute{submitted_by} = getlogin || getpwuid($>) || 'ccbuild';
	$$compute{is_obsolete} = 0;
	my $insert = &executeSQL( $dbh,
		"insert into job(job_id,job_name,program_name,program_version,program_options,"
			. "subject_db_id,query_db_id,project_code,"
			. "status,result_type,result_message,date_submitted,submitted_by,is_obsolete)"
			. "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
		&extractArrayFromHash( $compute,
			split( /,/, "job_id,job_name,program_name,program_version,program_options,"
			. "subject_db_id,query_db_id,project_code,"
			. "status,result_type,result_message,date_submitted,submitted_by,is_obsolete" ) ) );
	if ( !defined $insert ) {
		$errorMessage = "submitCompute: " . $errorMessage . "\n job details:";
		foreach my $key ( keys %$compute ) {
 if ($key eq 'query_db') {
    $errorMessage .= join("\n",map {"$_\t$$compute{$key}->{$_}"} keys %{$$compute{$key}});
}
			$errorMessage .= "\n$key '$$compute{$key}'\n";
		}
		return undef;
	}
	
	return $compute;
}

#
# update status of compute
sub updateComputeStatus {
	my ( $dbh, $compute, $status, $result_type, $result_message ) = @_;

	$$compute{status} = $status;

	if ( !defined $result_type ) { 
		$result_type = $$compute{result_type};
		$result_message = $$compute{result_message};
	} else {
		$$compute{result_type} = $result_type;
		$$compute{result_message} = $result_message;
	}

	my $date_completed;
	if ( $status eq "pending" || $status eq "running" ) {
		$$compute{date_completed} = undef;
	} else {
		if ( !defined $$compute{date_completed} ) {
			$$compute{date_completed} = &now;
		}
	}
	
	my $update = &executeSQL( $dbh,
		"update job set status=?, result_type=?, result_message=?, date_completed=? "
			. "where job_id=?",
		$$compute{status}, $$compute{result_type}, $$compute{result_message}, $$compute{date_completed},
			$$compute{job_id} );
	if ( !defined $update ) {
		$errorMessage = "updateComputeStatus: " . $errorMessage;
		return undef;		
	}
	
	return $compute;
}

#
# load computed results
sub loadComputeResults {
	my ( $dbh, $compute ) = @_;

# start update transaction to synchronize result load and status update 
	$dbh->begin_work;

	my $num_results;

# load blast/rpsblast results
	if ( $$compute{program_name} =~ /blast/i ) {
		my $xmlpath = $$compute{result_message} . "/blastResults.xml";
		$num_results = &_loadBlastXML( $dbh, $compute, $xmlpath );

# load hmmpfam results
	} elsif ( $$compute{program_name} =~ /hmmpfam/i ) {

		$num_results = &_loadComputeHtab( $dbh, $compute );

# load priam results
	} elsif ( $$compute{program_name} =~ /priam/i ) {

		$num_results = &_loadPriam( $dbh, $compute );

# load iprscan result nikhat
	}  elsif ( $$compute{program_name} =~ /IPRSCAN/i ) {

		$num_results = &_loadIprscan( $dbh, $compute );

# load signalp results
	} elsif ( $$compute{program_name} =~ /signalp/i ) {

        $num_results = &_loadSignalp( $dbh, $compute );

# load tmhmm results
    } elsif ( $$compute{program_name} =~ /tmhmm/i ) {

        $num_results = &_loadTmhmm( $dbh, $compute );
    
# not implemented
    } else {
		$errorMessage = "loadComputeResults: load for $$compute{program_name} not implemented.";
		$dbh->rollback;
		return undef;
	}

    if ( !defined $num_results ) {
        $errorMessage = "loadComputeResults: " . $errorMessage;
        $dbh->rollback;
        return undef;
    }
    $$compute{status} = "loaded";
    $$compute{num_results} = $num_results;

	
# update job status/results
	my $update = &executeSQL( $dbh,
		"update job set status=?, num_results=? where job_id=?",
		$$compute{status}, $$compute{num_results}, $$compute{job_id} );
	if ( !defined $update ) {
		$errorMessage = "loadComputeResults: " . $errorMessage;
		$dbh->rollback;
		return undef;
	}

# commit and return
	$dbh->commit;
	return $compute;
}

#
# continue a detached compute
sub requeueCompute {
	my ( $dbh, $compute ) = @_;
	
	if ( $$compute{status} eq "local" ) {
		return $compute;
	} elsif ( $$compute{status} ne "running"
			&& $$compute{status} ne "pending"
			&& $$compute{status} ne "completed") {
		$errorMessage = "requeueCompute: invalid status: \"$$compute{status}\".";
		return undef;
	} else {
		my $job = &vicsRequeueJob( $$compute{job_id}, $$compute{program_name}, $$compute{job_name},
			$$compute{submitted_by}, $$compute{date_submitted} );
		if ( !defined $job ) {
			$errorMessage = "requeueCompute: " . $errorMessage;
			return undef;
		}
		$$compute{status} = $$job{status};
		return $compute;
	}
}

#
# abort a running compute
sub abortCompute {
	my ( $dbh, $compute, $message ) = @_;
	
	$compute = &updateComputeStatus( $dbh, $compute, "aborted", "error message", $message );
	if ( !defined $compute ) {
		$errorMessage = "abortCompute: " . $errorMessage;
		return undef;
	}
	
	return $compute;
}


###################################################################
# internal subroutines
###################################################################
#
# set options for VICS job submission
sub _setProgramOptions {
	my ( $params, $options ) = @_;
	
	if ( !defined $options || length($options) == 0 ) { return $params } 

	my @options = split( /\~\~/, $options );
	foreach my $option ( @options ) {

		$option =~ s/^\s+//;
		$option =~ s/\s+$//;
		my $brk;
		if ( $option =~ /^-/ ) {
			$brk = index( $option, " " );
		} else {
			$brk = index( $option, "=" );
		}
    	if ( $brk < 0  ) {
    		$errorMessage = "_setProgramOptions: invalid option \"" . $option . "\".";
			return undef;
   		} else {
   			my $name = substr( $option, 0, $brk);
   			my $value = substr( $option, $brk+1);
   			if ( !defined &vicsSetParam( $params, $name, $value ) ) {
				$errorMessage = "_setProgramOptions: " . $errorMessage;
				return undef;
   			}
   		}
	}
	
	return $params;
}

#
# get program details from VICS
# currently no such VICS service exists
sub _getVICSProgram {
	my ( $program_name ) = @_;

	my $program;
	$$program{name} = $program_name;
	$$program{version} = 0;
	
	return $program;
}

sub _compareHmmHits {
# compare hits for sorting
	my ( $a, $b ) = @_;

# compare blast metrics
	if ( $$a{total_evalue} > $$b{total_evalue} ) {
		return 1;
	} elsif ( $$a{total_evalue} < $$b{total_evalue} ) {
		return -1;
	} elsif ( $$a{total_score} < $$b{total_score} ) {
		return 1;
	} elsif ( $$a{total_score} > $$b{total_score} ) {
		return -1;
	} elsif ( $$a{domain_evalue} > $$b{domain_evalue} ) {
		return 1;
	} elsif ( $$a{domain_evalue} < $$b{domain_evalue} ) {
		return -1;
	} elsif ( $$a{domain_score} < $$b{domain_score} ) {
		return 1;
	} elsif ( $$a{domain_score} > $$b{domain_score} ) {
		return -1;
	} elsif ( $$a{domain_count} > $$b{domain_count} ) {
		return 1;
	} elsif ( $$a{domain_count} < $$b{domain_count} ) {
		return -1;

# break ties so sort is deterministic
	} elsif ( $$a{query_id} lt $$b{query_id} ) {
		return 1;
	} elsif ( $$a{query_id} gt $$b{query_id} ) {
		return -1;
	} elsif ( $$a{hmm_acc} lt $$b{hmm_acc} ) {
		return 1;
	} elsif ( $$a{hmm_acc} gt $$b{hmm_acc} ) {
		return -1;
	} elsif ( $$a{query_begin} < $$b{query_begin} ) {
		return 1;
	} elsif ( $$a{query_begin} > $$b{query_begin} ) {
		return -1;
	} elsif ( $$a{query_end} < $$b{query_end} ) {
		return 1;
	} elsif ( $$a{query_end} > $$b{query_end} ) {
		return -1;
	} elsif ( $$a{hmm_begin} < $$b{hmm_begin} ) {
		return 1;
	} elsif ( $$a{hmm_begin} > $$b{hmm_begin} ) {
		return -1;
	} elsif ( $$a{hmm_end} < $$b{hmm_end} ) {
		return 1;
	} elsif ( $$a{hmm_end} > $$b{hmm_end} ) {
		return -1;
	} else {
		return 0;
	}	
}

#
# load compute results from hmmpfam
sub _loadComputeHtab {
    my ( $dbh, $compute ) = @_;
	our $errorMessage;

# get hash of existing query sequences
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$errorMessage = "_loadComputeHtab: " . $errorMessage;
			return undef;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}

# get hash of existing subject sequences
	my $subjectHmms;
	if ( $$compute{subject_db_id} > 0 ) {
		$subjectHmms = &getDatasetSeqAccs( $dbh, $$compute{subject_db_id} );
		if ( !defined $subjectHmms ) {
			$errorMessage = "_loadComputeHtab: " . $errorMessage;
			return undef;
		} elsif ( $subjectHmms == 0 ) {
			$subjectHmms = undef;
		}
		
	}
	
# get custom hmm annotation (if any)
	my $hmmanno = $$compute{subject_db}{hmm_annotation}; 

# open seq identifier
	my $idseq = &openSequence( $dbh, "seq_id", 100000 );
	if ( !defined $idseq ) {
		$errorMessage = "_loadComputeHtab: " . $errorMessage;
		return undef;
	}

# parse hmmpfam results
	my @results;
	{
		my $hmmeroutput = $$compute{result_message} . "/hmmerpfam.out";
		my $htab = &_htab( $dbh, $hmmeroutput );
		if ( !defined $htab ) {
			$errorMessage = "loadComputeHtab: " . $errorMessage;
			return undef;
		} elsif ( $htab == 0 ) {
			return 0;
		}

# load hmm hits
		my ( $hmm_acc, $hmm_len, $hmm_description, $query_id, $query_description );
		foreach my $row ( @$htab ) {
			my %hit;
	    	($hmm_acc, undef, $hmm_len, undef, undef, $query_id,
			     $hit{hmm_begin}, $hit{hmm_end}, $hit{query_begin}, $hit{query_end}, undef,
    			 $hit{domain_score}, $hit{total_score}, $hit{domain_index}, $hit{domain_count},
    			 $hmm_description, $query_description,
    			 $hit{trusted_cutoff}, $hit{noise_cutoff}, $hit{total_evalue}, $hit{domain_evalue})
    			 = @$row;

	    	$hit{job_id} = $$compute{job_id};
			$hit{hmm_acc} = $hmm_acc;
			$hit{query_id} = $query_id;

# apply custom hmm annotation
			if ( defined $hmmanno ) {
				my $anno = $$hmmanno{$hmm_acc};
				$hit{noise_cutoff} = $$anno{noise_cutoff};
				$hit{trusted_cutoff} = $$anno{trusted_cutoff};
				$hmm_len = $$anno{hmm_len};
				$hmm_description = $$anno{definition};
			}
			
# look up or add subject hmm
			if ( $$compute{subject_db_id} == 0 ) {
				$hit{subject_hmm_id} = 0;
			} else {
				if ( ! exists $$subjectHmms{$hmm_acc} ) {
					my $id = &nextSequenceValue( "seq_id" );
					my $seq = &addDatasetSeq( $dbh,
						$$compute{subject_db_id}, $id, $hmm_acc, $hmm_description, $hmm_len );
					if ( !defined $seq ) {
						$errorMessage = "_loadComputeHtab: hmm: " . $errorMessage;
						return undef;	
					}
					$$subjectHmms{$hmm_acc}{seq_id} = $$seq{seq_id};
					$$subjectHmms{$hmm_acc}{seq_acc} = $$seq{seq_acc};
#				} else {
				}
				$hit{subject_hmm_id} = $$subjectHmms{$hmm_acc}{seq_id}
			}
	
# look up or add query seq
			if ( $$compute{query_db_id} == 0 ) {
				$hit{query_seq_id} = 0;
			} else {
				if ( ! exists $$querySeqs{$query_id} ) {
					my $id = &nextSequenceValue( "seq_id");
					my $seq = &addDatasetSeq( $dbh,
						$$compute{query_db_id}, $id, $query_id, $query_description, -1 );
					if ( !defined $seq ) {
						$errorMessage = "_loadComputeHtab: qry: " . $errorMessage;
						return undef;	
					}
					$$querySeqs{$query_id} = $seq;
				}
				$hit{query_seq_id} = $$querySeqs{$query_id}{seq_id};
			}

# save hit
			push( @results, \%hit );
		}
	}

# rank hits
	if ( scalar @results > 1 ) {
		@results =  sort { return int( &_compareHmmHits( $a, $b ) ) } @results;
	}
	
	my $cnt = 0;
	foreach my $hit ( @results ) {
		$$hit{rank} = ++$cnt;
			
		my $hits = $$subjectHmms{$$hit{hmm_acc}}{hits};
		if ( ! exists ( $$hits{$$hit{query_id}} ) ) {
			my $rank = 1 + scalar keys %$hits;
			$$hits{$$hit{query_id}} = $rank;
			if ( $rank == 1 ) { $$subjectHmms{$$hit{hmm_acc}}{hits} = $hits }
		}
		$$hit{rank_vs_hmm} = $$hits{$$hit{query_id}};
			
		$hits = $$querySeqs{$$hit{query_id}}{hits};
		if ( ! exists ( $$hits{$$hit{hmm_acc}} ) ) {
			my $rank = 1 + scalar keys %$hits;
			$$hits{$$hit{hmm_acc}} = $rank;
			if ( $rank == 1 ) { $$querySeqs{$$hit{query_id}}{hits} = $hits }
		}
		$$hit{rank_vs_query} = $$hits{$$hit{hmm_acc}};

	}

# write to database
	my @cols = split ( /,/,
		"job_id,rank,rank_vs_hmm,rank_vs_query,subject_hmm_id,query_seq_id,hmm_begin,hmm_end,query_begin,query_end,"
			. "domain_score,total_score,domain_index,domain_count,trusted_cutoff,noise_cutoff,"
			. "total_evalue,domain_evalue" );

	my @buffer = ();
	foreach my $hit ( @results ) {
		my @row = &extractArrayFromHash( $hit, @cols );
		push( @buffer, \@row );
	}
	if ( scalar @buffer == 0 ) { return 0 }
	
	my $columns = join( ",", @cols );
	my $values = join( ",", split( //, &rpad( "?", scalar @cols, "?" ) ) );

	my $insert =
		&bulkInsertData( $dbh,
			"insert into htab(" . $columns . ") values (" . $values . ")",
			\@buffer );
	if ( !defined $insert ) {
		$errorMessage = "_loadComputeHtab: " . $errorMessage;
		return undef;	
	}

# return # hits
	return $insert; 
}

#
# refresh contents of "egad"" tables (hmm2 and rfam) from sybase
sub refreshEgad {
	my ( $sqlh ) = @_;
	
# clear old data from sqlite db
	$sqlh->begin_work;
	&executeSQL( $sqlh, "delete from hmm2" );
	&executeSQL( $sqlh, "delete from rfam" );

# connect to sybase
	my $sybh = connectSybase( "egad", "access", "access" );
	if ( !defined $sybh ) {
		$errorMessage = "refreshEgad: connect sybase: " . $DBI::errstr;
		return undef;
	}

# copy rfam data
	my $data =
		$sybh->selectall_arrayref(
			"select id,accession,feat_type,feat_class,com_name,gene_sym,window_size,noise_cutoff,gathering_thresh,trusted_cutoff,euk,prok,vir,iscurrent "
				. "from rfam where iscurrent=1");
	if ( !defined $data ) {
		$errorMessage = "refreshEgad: select rfam: " . $DBI::errstr;
		$sqlh->rollback;
		$sybh->disconnect;
		return undef;
	}

	foreach my $row ( @$data ) {
		my $insert =
			$sqlh->do(
				"insert into rfam(id,accession,feat_type,feat_class,com_name,gene_sym,window_size,noise_cutoff,gathering_thresh,trusted_cutoff,euk,prok,vir,iscurrent,date_refreshed)"
				. "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,datetime('now'))",
				undef,@$row,);
		if ( !defined $insert ) {
			$errorMessage = "refreshEgad: insert rfam: " . $DBI::errstr;
			$sqlh->rollback;
			$sybh->disconnect;
			return undef;
		}
	}

# copy hmm2 data
	$data =
		$sybh->selectall_arrayref(
			"select id,hmm_acc,hmm_len,iso_type,hmm_com_name,gene_sym,ec_num,noise_cutoff,gathering_cutoff,trusted_cutoff,trusted_cutoff2,is_current "
				. "from hmm2 where is_current=1");
	if ( !defined $data ) {
		$errorMessage = "refreshEgad: select hmm2: " . $DBI::errstr;
		return undef;
		$sqlh->rollback;
		$sybh->disconnect;
	}

	foreach my $row ( @$data ) {
		my $insert =
			$sqlh->do(
				"insert into hmm2(id,hmm_acc,hmm_len,iso_type,hmm_com_name,gene_sym,ec_num,noise_cutoff,gathering_cutoff,trusted_cutoff,trusted_cutoff2,is_current,date_refreshed)"
					. "values(?,?,?,?,?,?,?,?,?,?,?,?,datetime('now'))",
				undef,@$row);
		if ( !defined $insert ) {
			$errorMessage = "refreshEgad: insert hmm2: " . $DBI::errstr;
			$sqlh->rollback;
			$sybh->disconnect;
			return undef;
		}
	}

# disconnect sybase, commit sqlite, and return
	$sybh->disconnect;
	$sqlh->commit;
	return 0;
}

sub _htab {
	my ( $db_proc, $file ) = @_;

# get extended HMM data from egad tables
	my $hmm2;
	{
		my $tmp = &querySQLArrayHash( $db_proc,
			"select hmm_acc, hmm_len, trusted_cutoff, noise_cutoff, gathering_cutoff, hmm_com_name as com_name, "
				. "gene_sym, ec_num, iso_type, trusted_cutoff2 "
				. "from hmm2 where is_current = 1" );
		if ( !defined $tmp ) {
			$errorMessage = "_htab: hmm2: " . $errorMessage;
			return undef;
		}
		$hmm2 = &arrayHashToHashHash( $tmp, "hmm_acc" );
	}

	my $rfam;
	{
		my $tmp = &querySQLArrayHash( $db_proc,
			"select accession as hmm_acc, null as hmm_len, trusted_cutoff, noise_cutoff, gathering_thresh, com_name, "
				. "window_size, feat_type, feat_class, gene_sym "
				. "from rfam where iscurrent = 1" );
		if ( !defined $tmp ) {
			$errorMessage = "_htab: rfam: " . $errorMessage;
			return undef;
		}
		$rfam = &arrayHashToHashHash( $tmp, "hmm_acc" );
	}

# parse HMM results
	my $hits = &_htabParseInput( $file, $hmm2, $rfam );
	if ( !defined $hits ) {
		$errorMessage = "_htab: " . $errorMessage;
		return undef;
	}
#print "# hits: " . scalar @$hits . "\n";

# return results
    return $hits;
}
	
#
# parse HMM results and return array ref of hits
sub _htabParseInput {
    my ( $inputfile, $hmm2, $rfam ) = @_;
	
	my $output = ();

	my ( $inputsize ) = split / /, `wc -l $inputfile`;
	$inputsize =~ s/[\n\r]//g;
#print "opening $inputfile ($inputsize)\n";
	if ( ! open( HMM, $inputfile ) ) {
		$errorMessage = "_htabParseInput: could not open input file: $inputfile";
		return undef;
	}

    my $method = 'hmmpfam';
    my $version = 2;
    my ( $query_obj, %seq, $query_desc );
    
    my $line;
    for my $i ( 0..8 ) {
    	$line = <HMM>;
#print "header: $line\n";
	}
	$line =~ s/[\n\r]//g;
    my $n = 8;

#print "PARSE HMM OUTPUT\n";

    while ( $n < $inputsize ) {

# get query sequence name for new hit
# extend previous hit and add to results
        if ( $line =~ /^Query sequence:\s+(\S+)/ ) {
#print "query sequence *****************************************************************************************\n";
            $query_obj = $1;
            $query_desc = "";
            %seq = ();
            ++$n;
            $line = <HMM>;
			$line =~ s/[\n\r]//g;
#print "QS: $n of $inputsize: $line\n";

# get query sequence definition
        } elsif ( $line =~ /^Description:\s+(.+)/ ) {
#print "query description *****************************************************************************************\n";
            $query_desc = $1;
            chomp $query_desc;
            $query_desc =~ s/\t/ /g;
            $query_desc =~ s/^\s+//;
            $query_desc =~ s/\s+$//;
            ++$n;
            $line = <HMM>;
			$line =~ s/[\n\r]//g;
#print "QD: $n of $inputsize: $line\n";

# find hit header, (HMM, description, score, e_value, #domains)
        } elsif (  $line =~ /^Model\s+Description\s+Score/ ) {
#print "hmm *****************************************************************************************\n";
            my $descr_pos = index($line, "Description");
            my $descr_end = index($line, "Score") - 1;
            $seq{number} = 0;
			for my $i ( 1..2 ) {
				++$n;
				$line = <HMM>;
#print "H1: $n of $inputsize: $line\n";
			}
			$line =~ s/[\n\r]//g;

# no hit, jump to end-of-[non]hit
			if ( $line =~ /no hits above thresholds/ ) {
#print " not a hit, skip 10 *****************************************************************************************\n";
				for my $i ( 1..10 ) {
					++$n;
					$line = <HMM>;
#print "H2: $n of $inputsize: $line\n";
				}
				$line =~ s/[\n\r]//g;

# process hits
			} else {
#print "hmm hit *****************************************************************************************\n";
	            while ( $line =~ /^\S+/ ) {
#print ">> $n of $inputsize: $line\n";
#print "hmmpfam: $line\n";
					my $tmppos = $descr_end;
#print "descr_pos=$descr_pos  descr_end=$descr_end  tmppos=$tmppos  ch=" . substr( $line, $tmppos, 1 ) . "\n";
					while ( $tmppos > $descr_pos+10 && substr( $line, $tmppos, 1) ne " " ) {
						$tmppos--;
#print "descr_pos=$descr_pos  descr_end=$descr_end  tmppos=$tmppos  ch=" . substr( $line, $tmppos, 1 ) . "\n";
					}
					my $descr = substr( $line, $descr_pos, $tmppos-$descr_pos+1 );
					$descr =~ s/^\s+//;
					$descr =~ s/\s+$//;
					
					$line = substr( $line , 0, $descr_pos ) . substr( $line, $tmppos+1 );
	            	$line =~ s/^\s+//;
	            	$line =~ s/\s+/\t/g;
    	            my ( $header, $score, $e_value, $number ) = split( /\t/, $line );

        	        $seq{$seq{number}}{header} = $header;
    	            $seq{$seq{number}}{comment} = $descr;
	                $seq{$seq{number}}{score} = $score;
                	$seq{$seq{number}}{e_value} = $e_value;
            	    $seq{$seq{number}}{number} = $number;
        	        ++$seq{number};
            	    ++$n;
            	    $line = <HMM>;
					$line =~ s/[\n\r]//g;
#print "HH: $n of $inputsize: $line\n";
            	}
			}

# get seq-f, seq-t, hmm-f, hmm-t, score, e_value for each domain for hmmsearch or hmmpfam
        } elsif ( $line =~ /^Model\s+Domain\s+seq-f\s+seq-t\s+hmm-f\s+hmm-t\s+score\s+E-value/i ) {
#print "domain detail *****************************************************************************************\n";
			for my $i ( 1..2 ) {
				++$n;
				$line = <HMM>;
#print "D1: $n of $inputsize: $line\n";
			}
			$line =~ s/[\n\r]//g;
            while ( $line =~ /^(\S+)\s+(\d+)\/(\d+)\s+(\d+)\s+(\d+).+?(\d+)\s+(\d+).+?(-?\d\S*)\s+(\d\S*)/ ) {
#print ">> $n of $inputsize: $line\n";
                for my $m ( 0 .. $seq{number} - 1 ) {
                    if ( $seq{$m}{header} eq $1 ) {
                        $seq{$m}{$2}{seq_f}   = $4;
                        $seq{$m}{$2}{seq_t}   = $5;
                        $seq{$m}{$2}{hmm_f}   = $6;
                        $seq{$m}{$2}{hmm_t}   = $7;
                        $seq{$m}{$2}{score}   = $8;
                        $seq{$m}{$2}{e_value} = $9;
                    }
                }
   	            ++$n;
				$line = <HMM>;
				$line =~ s/[\n\r]//g;
#print "D2: $n of $inputsize: $line\n";
            }

# end of this hit (//), extend data and add to results
        } elsif ( $line =~ /\/\// ) {
#print "end-of-hit *****************************************************************************************\n";
            if ( $seq{number} > 0 ) {
            	my $hits = &_htabExtendHit( $method, \%seq, $query_obj, $query_desc, $hmm2, $rfam );
	            if ( !defined $hits ) {
	            	$errorMessage = "_htabParseInput: " . $errorMessage;
	            	return undef;
	            }      
	            push( @$output, @$hits );
            }
            ++$n;
			$line = <HMM>;
			$line =~ s/[\n\r]//g;
#print "EH: $n of $inputsize: $line\n";
            
        } else {
        	++$n;
			$line = <HMM>;
			$line =~ s/[\n\r]//g;
#print "X1: $n of $inputsize: $line\n";
        }
    }
	close( HMM );
 
# return parsed results
    if ( !defined $output || scalar @$output == 0 ) { return 0 };
    return $output;
}

#
# extend HMM hits with details from egad tables and return as an array ref 
sub _htabExtendHit {

    my ( $method, $seq_r, $query_obj, $query_desc, $hmm2, $rfam ) = @_;
	my ( $details, $com_name, $hmm_len, $noise_cutoff, $trusted_cutoff );

	my $output = ();

# are there any hits?
	if ( $$seq_r{number} <= 0 ) {
		return $output;
	}
	
# process HMM hits
	for my $m ( 0 .. $$seq_r{number} - 1 ) {

# adjust strand specific HMMs
		if ( $$seq_r{$m}{header} =~ /_rev$/ ) {	# check for strand specific HMM (_fwd or _rev)
			$$seq_r{$m}{header} =~ s/_rev$//;
			for my $n ( 1 .. $$seq_r{$m}{number} ) {
				my $seq_t = $$seq_r{$m}{$n}{seq_t};
				$$seq_r{$m}{$n}{seq_t} = $$seq_r{$m}{$n}{seq_f};
				$$seq_r{$m}{$n}{seq_f} = $seq_t;
			}
		} elsif ( $$seq_r{$m}{header} =~ /_fwd$/ ) {
			$$seq_r{$m}{header} =~ s/_fwd$//;
		}

# extend curated HMMs from annotation data
		my $ref = &_htabGetHmmInfo( $$seq_r{$m}{header}, $$seq_r{$m}{comment}, $hmm2, $rfam );
		if ( !defined $ref ) {
			$errorMessage = "_htabExtendHmmHit: " . $errorMessage;
			return undef;
		}
		$$seq_r{$m}{hmm_len} = $$ref{hmm_len};
		$$seq_r{$m}{noise_cutoff} = $$ref{noise_cutoff};
		$$seq_r{$m}{trusted_cutoff} = $$ref{trusted_cutoff};
		$$seq_r{$m}{com_name} = $$ref{com_name};
		$$seq_r{$m}{details} = $$ref{details};
	}

# add each domain match as a hit
	my ( $day, $month, $year ) = ( (localtime)[3], (localtime)[4] + 1, (localtime)[5] + 1900 );	
	for my $m ( 0 .. $$seq_r{number} - 1 ) {
		for my $n ( 1 .. $$seq_r{$m}{number} ) {
			my @row = ( $$seq_r{$m}{header}, "$year-$month-$day", $$seq_r{$m}{hmm_len}, $method, "", $query_obj, $$seq_r{$m}{$n}{hmm_f}, $$seq_r{$m}{$n}{hmm_t}, $$seq_r{$m}{$n}{seq_f}, $$seq_r{$m}{$n}{seq_t}, undef, $$seq_r{$m}{$n}{score}, $$seq_r{$m}{score}, $n, $$seq_r{$m}{number}, $$seq_r{$m}{com_name}, $query_desc, $$seq_r{$m}{trusted_cutoff}, $$seq_r{$m}{noise_cutoff}, $$seq_r{$m}{e_value}, $$seq_r{$m}{$n}{e_value}, $$seq_r{$m}{details} );
			push ( @$output, \@row );
		}
	}

# return hits
	return $output;
}

#
# return hmm details
sub _htabGetHmmInfo { 
	my ( $acc, $comment, $hmm2, $rfam ) = @_;

	my $info;
	
# first try hmm2 data
	if ( exists( $$hmm2{$acc} ) ) {
		$info = $$hmm2{$acc};
# if not in hmm2, try rfam data
	} else {
		my $acc2 = $acc;
		$acc2 =~ s/_rev$//;
		$acc2 =~ s/_fwd$//;
		if ( exists( $$rfam{$acc2} ) ) {
			my $info = $$rfam{$acc2};
	
# not in db, return empty details
		} else {
			$$info{hmm_acc} = $acc2;
			$$info{hmm_len} = undef;
			$$info{trusted_cutoff} = undef;
			$$info{noise_cutoff} = undef;
			$$info{details} = undef;
			$$info{com_name} = $comment;
		}
	}

# remove tabs from com_name
	$$info{com_name} =~ s/\t/ /g;
	if ( $$info{com_name} eq "-" ) { $$info{com_name} = undef }

# return details
	return $info;
}

sub _loadBlastXML {
	my ( $dbh, $compute, $xmlpath )= @_;
	my $euler = 2.718281828;

# open seq identifier
	print &rpad(" parse",10) . &lpad(chr(8),10,chr(8));
	my $idseq = &openSequence( $dbh, "seq_id", 100000 );
	if ( !defined $idseq ) {
		$errorMessage = "_loadBlastXML: " . $errorMessage;
		return undef;
	}
# get hash of existing query sequences
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$errorMessage = "_loadBlastXML: " . $errorMessage;
			return undef;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}

# get hash of existing subject sequences
	my $subjectSeqs;
	if ( $$compute{subject_db_id} > 0 ) {
		$subjectSeqs = &getDatasetSeqAccs( $dbh, $$compute{subject_db_id} );
		if ( !defined $subjectSeqs ) {
			$errorMessage = "_loadBlastXML: " . $errorMessage;
			return undef;
		} elsif ( $subjectSeqs == 0 ) {
			$subjectSeqs = undef;
		}
	}

# define database
# !!! to use sqlite import (currently commented out) hitcols order must match btab table !!!
	my $hittab = "btab";
	my @hitcols = split( /,/,
		"job_id,rank,rank_vs_subject,rank_vs_query,"
		. "subject_seq_id,subject_left,subject_right,subject_frame,"
		. "orientation,query_seq_id,query_end5,query_end3,query_frame,"
		. "num_identical,num_similar,num_gaps,alignment_length,"
		. "pct_identity,pct_similarity,pct_length,hsp_score,bit_score,evalue,pvalue" );

	my @rankcols = split( /,/,
			"evalue,bit_score");

	my $seqtab = "dataset_seq";
	my @seqcols = split( /,/,
		"version_id,seq_id,seq_acc,seq_definition,seq_length");
	
# define XML parsing
	my %xmlattr;
	$xmlattr{"Iteration_query-def"} = "query_definition"; 
	$xmlattr{"Iteration_query-len"} = "query_length";
	$xmlattr{"Hit_def"} = "subject_definition";
	$xmlattr{"Hit_len"} = "subject_length";
	$xmlattr{"Hsp_bit-score"} = "bit_score";
	$xmlattr{"Hsp_score"} = "hsp_score";
	$xmlattr{"Hsp_evalue"} = "evalue";
	$xmlattr{"Hsp_query-from"} = "query_end5";
	$xmlattr{"Hsp_query-to"} = "query_end3";
	$xmlattr{"Hsp_hit-from"} = "subject_left";
	$xmlattr{"Hsp_hit-to"} = "subject_right";
	$xmlattr{"Hsp_query-frame"} = "query_frame";
	$xmlattr{"Hsp_hit-frame"} = "subject_frame";
	$xmlattr{"Hsp_identity"} = "num_identical";
	$xmlattr{"Hsp_positive"} = "num_similar";
	$xmlattr{"Hsp_gaps"} = "num_gaps";
	$xmlattr{"Hsp_align-len"} = "alignment_length";

# open temporary files to store parsed data
# (large blast jobs produce too much data to buffer in memory)
	my $nhits = 0;
	my $tmphitfile = "$scratchspace/$$compute{job_id}.hits.$$";
	if ( ! open( HIT, ">$tmphitfile") ) {
		$errorMessage = "_loadBlastXML: could not tmp hit file for write.";
		return undef;
	}
	my $nseqs = 0;
	my $tmpseqfile = "$scratchspace/$$compute{job_id}.seqs.$$";
	if ( ! open( SEQ, ">$tmpseqfile") ) {
		$errorMessage = "_loadBlastXML: could not tmp seq file for write.";
		unlink $tmpseqfile;
		return undef;
	}

# parse XML
	if ( ! open( XML, $xmlpath ) ) {
		$errorMessage = "_loadBlastXML: could not open results\"" . $xmlpath . "\".";
		unlink $tmpseqfile;
		unlink $tmphitfile;
		return undef;
	}
	my $line = <XML>;
	if ( !defined $line ) {
		$errorMessage = "_loadBlastXML: malformed XML.";
		unlink $tmpseqfile;
		unlink $tmphitfile;
		return undef;
	} 
	my ( $hit, $subject_definition, $query_definition );
	$$hit{job_id} = $$compute{job_id};
	$$hit{num_gaps} = 0;

	while ( $line = <XML> ) {
		$line =~ s/[\n\r]/ /g;
		$line =~ s/^\s+//;
		$line =~ s/\s+$//;
		$line =~ s/&lt;/</g;
		$line =~ s/&gt;/>/g;
		$line =~ s/&amp;/&/g;
		$line =~ s/&quot;/"/g;
		$line =~ s/&apos;/'/g;

# check XML tag
		if ( substr($line,0,1) eq "<" ) {
			$line = substr( $line, 1 );
			my ( $xml_tag ) = split( ">", $line);
			my $attrname = $xmlattr{"$xml_tag"};
			my $attrval;

# get tag value
			if ( defined $attrname ) {
				$line = substr( $line, length($xml_tag) + 1 );
				my $close_tag = "</" . $xml_tag . ">";
				my  $eod = index( $line, $close_tag );
				if ( $close_tag == -1 ) {
					$attrval = $line;
				} else {
					$attrval = substr( $line, 0, $eod );
				}

# save tag value
				if ( $attrname eq "subject_definition" ) {
					$subject_definition = $attrval;
					my ( $subject_id ) = split( /\s/, $subject_definition );
					$subject_id = &_parseBlastSeqId( $subject_id );
					$$hit{"subject_id"} = $subject_id;
				} elsif ( $attrname eq "query_definition" ) {
					$query_definition = $attrval;
					my ( $query_id ) = split( /\s/, $query_definition );
					$query_id = &_parseBlastSeqId( $query_id );
					$$hit{"query_id"} = $query_id;
				} else {
					$$hit{$attrname} = $attrval;
				}
			}

# save hit
			if ( $attrname eq "alignment_length" ) {

# look up or add subject seq
				if ( $$compute{subject_db_id} == 0 ) {
					$$hit{subject_seq_id} = 0;
				} else {
					if ( ! exists $$subjectSeqs{$$hit{subject_id}} ) {
						my $seq;
						$$seq{version_id} = $$compute{subject_db_id};
						$$seq{seq_id} = &nextSequenceValue( "seq_id" );
						$$seq{seq_acc} = $$hit{subject_id};
						$$seq{seq_definition} = $subject_definition;
						$$seq{seq_length} = $$hit{subject_length};
						$$subjectSeqs{$$hit{subject_id}}{seq_acc} = $$seq{seq_acc};
						$$subjectSeqs{$$hit{subject_id}}{seq_id} = $$seq{seq_id};

						my @tmprow = &extractArrayFromHash( $seq, @seqcols );
						print SEQ join( "\t", @tmprow ) . "\n";
						$nseqs++;
					}
				}
				$$hit{subject_seq_id} = $$subjectSeqs{$$hit{subject_id}}{seq_id};

# look up or add query seq
				if ( $$compute{query_db_id} == 0 ) {
					$$hit{query_seq_id} = 0;
				} else {
					if ( ! exists $$querySeqs{$$hit{query_id}} ) {
						$errorMessage = "_loadBlastXML: unknown query sequence \"" . $$hit{query_seq_id} . "\"";
						unlink $tmpseqfile;
						unlink $tmphitfile;
						return undef;	
					}
					$$hit{query_seq_id} = $$querySeqs{$$hit{query_id}}{seq_id};
				}

# calculate orientation
				my $orientation = 1;
				if ( $$hit{query_frame} < 0 ) {
					$orientation = -$orientation;
				}
				if ( $$hit{subject_frame} < 0 ) {
					$orientation = -$orientation;
				}
				$$hit{orientation} = $orientation;

# calculate percentages
				$$hit{pvalue} = 1. - $euler**(-$$hit{evalue});
				my $qlen = abs( $$hit{query_end3} - $$hit{query_end5} ) + 1;
				my $hlen = abs( $$hit{subject_right} - $$hit{subject_left} ) + 1;
				$$hit{pct_identity} = int( 1000. * 2. * $$hit{num_identical} / ( $qlen + $hlen ) ) / 10.;
				$$hit{pct_similarity} = int( 1000. * 2. * $$hit{num_similar} / ( $qlen + $hlen ) ) / 10.;
				my $qpctlen = int( 1000. * $qlen / $$hit{query_length} ) / 10.;
				my $spctlen = int( 1000. * $hlen / $$hit{subject_length} ) / 10.;
				if ( $qpctlen >= $spctlen ) {
					$$hit{pct_length} = $qpctlen;
				} else {
					$$hit{pct_length} = $spctlen;
				}
# write hit to temporary file
				$nhits++;
				$$hit{hsp_id} = $nhits;
				my @row = (
					&extractArrayFromHash( $hit, @rankcols ),
					&extractArrayFromHash( $hit, @hitcols )
					);
				print HIT join( "\t", @row ) . "\n";

# start a new hsp
				$hit = &_newHsp( $hit );
			}
		}
	}
	close( XML );
	close( HIT );
	close( SEQ );
	&closeSequence( "seq_id" );

# save subject sequences to db
	print &rpad(" save seqs",10) . &lpad(chr(8),10,chr(8));
	if ( $nseqs > 0 ) {
		if ( !open( SEQ, "<$tmpseqfile" ) ) {
			$errorMessage = "_loadBlastXML: " . $errorMessage;
			unlink $tmpseqfile;
			unlink $tmphitfile;
			return undef;
		}

		my @seqs = ();
		while ( my $line = <SEQ> ) {
			$line =~ s/[\r\n]//g;
			my @row = split( "\t", $line );
			push( @seqs, \@row );
			$nseqs++;
			if ( $nseqs >= $insertsize ) {
				if ( !defined &_bulkInsert2( $dbh, $seqtab, \@seqcols, \@seqs ) ) {
					$errorMessage = "_loadBlastXML: " . $errorMessage;
					unlink $tmpseqfile;
					unlink $tmphitfile;
					return undef;
				}
				@seqs = ();
				$nseqs = 0;
			}
		}		

		if ( $nseqs > 0 ) {
			if ( !defined &_bulkInsert2( $dbh, $seqtab, \@seqcols, \@seqs ) ) {
				$errorMessage = "_loadBlastXML: " . $errorMessage;
				unlink $tmpseqfile;
				unlink $tmphitfile;
				return undef;
			}
		}
	}
	close( SEQ );
	unlink $tmpseqfile;

# sort hits
	print &rpad(" sort",10) . &lpad(chr(8),10,chr(8));
	if ( $nhits>1 ) {
		sortBlastHits( $tmphitfile, $sortsize );
	}

# rank sorted hits
	print &rpad(" rank",10) . &lpad(chr(8),10,chr(8));
	my $total = 0;
	if ( $nhits > 0 ) {
		my $first_data_col = scalar @rankcols;
		my $last_data_col = scalar @hitcols + scalar @rankcols - 1;
		my $subject_id_col = &findArrayValue( "subject_id", \@rankcols );
		my $query_id_col = &findArrayValue( "query_id", \@rankcols );

		my $rank_col = &findArrayValue( "rank", \@hitcols );
		my $rank_vs_subject_col = &findArrayValue( "rank_vs_subject", \@hitcols );
		my $rank_vs_query_col = &findArrayValue( "rank_vs_query", \@hitcols );

		my $nbuf = 0;
		my @hits = ();
		if ( !open( HIT, "<$tmphitfile" ) ) {
			$errorMessage = "_loadBlastXML: could not open $tmphitfile for read.";
			unlink $tmphitfile;
			return undef;
		}
#		if ( !open( RANKED, ">$tmphitfile.ranked" ) ) {
#			$errorMessage = "_loadBlastXML: could not open $tmphitfile.ranked for write.";
#			unlink $tmphitfile;
#			return undef;
#		}
		while ( my $line = <HIT> ) {
			$line =~ s/[\r\n]//g;
			my @tmp = split( "\t", $line );
			my $subject_id = $tmp[$subject_id_col];
			my $query_id = $tmp[$query_id_col];

			my @row = @tmp[$first_data_col..$last_data_col];
		    
		    $row[$rank_col] = ++$total;
			
			my $hits = $$subjectSeqs{$subject_id}{hits};
			if ( ! exists ( $$hits{$query_id} ) ) {
				my $rank = 1 + scalar keys %$hits;
				$$hits{$query_id} = $rank;
				if ( $rank == 1 ) { $$subjectSeqs{$subject_id}{hits} = $hits }
			}
			$row[$rank_vs_subject_col] = $$hits{$query_id};
			
			$hits = $$querySeqs{$query_id}{hits};
			if ( ! exists ( $$hits{$subject_id} ) ) {
				my $rank = 1 + scalar keys %$hits;
				$$hits{$subject_id}{rank} = $rank;
				$$hits{$subject_id}{num_hsps} = 1;
				if ( $rank == 1 ) { $$querySeqs{$query_id}{hits} = $hits }
			} else {
				$$hits{$subject_id}{num_hsps}++;
			}
			$row[$rank_vs_query_col] = $$hits{$subject_id}{rank};
		
# save ranked hit
			push( @hits, \@row );
			$nbuf++;

# buffer is full, dump to db
			if ( $nbuf >= $insertsize ) {
				if ( !defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@hits ) ) {
					$errorMessage = "_loadBlastXML: " . $errorMessage;
					unlink $tmphitfile;
					return undef;
				}
				my $pct = int( 100. * $total / $nhits + 0.5 ) . "%";
				print &rpad(" save $pct",10) . &rpad(chr(8),10,chr(8));
				@hits = (); 
				$nbuf = 0;
			}
		}		

# flush remsining buffer to db
		if ( $nbuf > 0 ) {
			if ( !defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@hits ) ) {
				$errorMessage = "_loadBlastXML: " . $errorMessage;
				unlink $tmphitfile;
				return undef;
			}
		}
	}
	close( HIT );
	unlink $tmphitfile;

	if ( $total != $nhits ) {
		$errorMessage = "_loadBlastXML: lost hits - #read $nhits, #inserted $total";
		return undef;
	}

	print &rpad(" ",10) . &lpad(chr(8),10,chr(8));
#	close( RANKED );
#	$tmphitfile .= ".ranked";
#	
## import ranked hits into btab table
#	$dbh->commit;
#	my $dbfile = &getSessionDBFile( $dbh );	
#	my $import = `echo \".mode tabs\n.import $tmphitfile btab\" | sqlite3 $dbfile`;
#	if ( $import =~ /error/i ) {
#		$import =~ s/[\n\r]/ /g;
#		$errorMessage = "loadBlastXML (import): " . $import;
#		unlink $tmphitfile;
#		return undef; 
#	}
#	unlink $tmphitfile;
#	$dbh->begin_work;

# return number of hits
	return $total;	
}

sub _newHsp {
	my ( $oldhsp ) = @_;
	my %hsp;
	if ( defined $oldhsp ) {
		%hsp = %$oldhsp
	} else {
		$hsp{query_id} = "";
	}
	$hsp{num_gaps} = 0;
	
	return \%hsp;
}

# attempt to find best identifier in sequence identifier string
sub _parseBlastSeqId {
	my ( $rawid ) = @_;

	$rawid =~ s/,\s*$//; 
	$rawid =~ s/\|\s*$//; 
	my @id = split( /\|/, $rawid );
	if ( scalar @id < 2 ) { return $rawid }
	my $i = 0;
	while ( $i < scalar @id - 1 ) {
		if ( index( ".gi.gb.rf.emb.dbj.pir.prf.sp.ref.", "." . lc($id[$i]) . "." ) >= 0 ) {
			return lc($id[$i]) . "|" . $id[$i+1];
		}
		$i++;
	}
	return $rawid;
}

#
## load compute results from signalp
sub _loadSignalp {
    my ( $dbh, $compute ) = @_;
    my $path = $$compute{result_message};

    # get hash of existing query sequences
    my $querySeqs;
    if ( $$compute{query_db_id} > 0 ) {
        $querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
        if ( !defined $querySeqs ) {
            $errorMessage = "_loadSignalp: " . $errorMessage;
            return undef;
        } elsif ( $querySeqs == 0 ) {
            $querySeqs = undef;
        }
    }
 
    # define database
    my $hittab = "signalp";
	my @hitcols = split( /,/,"job_id,query_seq_id,hmm_flag,hmm_cmax,hmm_cmax_pos,hmm_cmax_flag,hmm_sprob,hmm_sprob_flag");

    # get signalp results
	my $total = 0;
	my @signalp_results = ();
	open( SIGNALP, "$path/signalp.out" );
	while ( my $line = <SIGNALP> ) {
        next if $line =~ /^\s*\#/; # skip comments
        if ( $line =~ /error/i ) {
            &executeSQL( $dbh,
                "update job set result_message=? where job_id=?",
                "warning: " . $line, $$compute{jobid} );
        } else {
            $line =~ s/\s+/\t/g;
            chomp $line;
            my ( $query_id, $hmm_flag, $hmm_cmax, $hmm_cmax_pos, $hmm_cmax_flag, $hmm_sprob, $hmm_sprob_flag ) = (split( /\t/, $line ))[14,15,16,17,18,19,20];
            my $querySeqId = $$querySeqs{$query_id}{seq_id};
            # skip empty rows
            if ( defined ($query_id) ) {
                my @row = ( $$compute{job_id}, $querySeqId, $hmm_flag, $hmm_cmax, $hmm_cmax_pos, $hmm_cmax_flag, $hmm_sprob, $hmm_sprob_flag);
                push @signalp_results, \@row;
                $total++;
            }
        }
	} 
	close SIGNALP;
	
    # insert data
	if ( !defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@signalp_results ) ) {
		$errorMessage = "_loadSignalp: " . $errorMessage;
		return undef;
	}

	print &rpad(" ",10) . &lpad(chr(8),10,chr(8));

    # return number of hits
    return $total;	
			
}

#
## load compute results from tmhmm
sub _loadTmhmm {
    my ( $dbh, $compute ) = @_;
    my $path = $$compute{result_message};

    # get hash of existing query seqs:
    my $querySeqs;
    if ( $$compute{query_db_id} > 0 ) {
        $querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
        if ( !defined $querySeqs ) {
            $errorMessage = "_loadTmhmm: " . $errorMessage;
            return undef;
        } elsif ( $querySeqs == 0 ) {
            $querySeqs = undef;
        }
    }

    # define database
    my $hittab = 'tmhmm';
    my @hitcols = split(',','job_id, query_seq_id, exp_aa, exp_first60, num_predicted_helixes, topology');
    
    my $total = 0;
    my @tmhmm_results = ();
    open ( TMHMM, "$path/tmhmm.out");
    
    while ( my $line = <TMHMM> ) {
        # skip first two lines (should begin with paths to the scripts...)
        next if ($line =~ /tmhmmformat/);
        next if ($line =~ /decodeanhmm/);
        chomp $line;
        my ( $query_id, $query_len, $expaa, $first60, $predhel, $topology );
        my @fields =  split( /\t/, $line );
        foreach my $field (@fields) {
            if ($field =~ /(.*)=(.*)/) {
                switch ($1) {
                    case "ExpAA"        { $expaa    = $2 }
                    case "First60"      { $first60  = $2 }
                    case "PredHel"      { $predhel  = $2 }
                    case "Topology"     { $topology = $2 }
                }
            } else {
                $query_id = $field;
            }
        }
		my $querySeqId = $$querySeqs{$query_id}{seq_id};
        my @row = ($$compute{job_id}, $querySeqId, $expaa, $first60, $predhel, $topology);
        push @tmhmm_results, \@row;
        $total++;
    }
    close TMHMM;

    # insert data
    if (!defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@tmhmm_results ) ) {
        $errorMessage = "_loadTmhmm: " . $errorMessage;
        return undef;
    }

    print &rpad(" ",10) . &lpad(chr(8),10,chr(8));

    # return number of hits
    return $total;

    
}

#
## load compute iprscan 
sub _loadIprscan{
    my ( $dbh, $compute ) = @_;
    my $path = $$compute{result_message};

    # get hash of existing query sequences
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$errorMessage = "_loadIprscan: " . $errorMessage;
			return undef;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}
    
    # define database
    my $hittab = "iprscan";
	my @hitcols = split( /,/,
		"job_id,query_seq_id,analysis_method,db_member_id,db_member_desc,dm_start,dm_end,evalue,status,interpro_id,interpro_desc,go_terms" );

    # get iprscan results
	my $total = 0;
	my @iprscan_results = ();
	open( IPRSCAN, "$path/interproscan.out" );
	while ( my $line = <IPRSCAN> ) {
		chomp $line;
		my ( $queryId, $analysis_method, $db_member_id, $db_member_desc,
		     $dm_start, $dm_end, $evalue, $status, $interpro_id, $interpro_desc,
		     $go_terms ) = (split (/\t/, $line))[0,3,4,5,6,7,8,9,11,12,13];
		my $querySeqId = $$querySeqs{$queryId}{seq_id};
		$interpro_id = '' if ($interpro_id eq 'NULL');
		$interpro_desc = '' if ($interpro_desc eq 'NULL'); 
		my @row = ( $$compute{job_id}, $querySeqId, $analysis_method, $db_member_id, $db_member_desc,
		              $dm_start, $dm_end, $evalue, $status, $interpro_id, $interpro_desc, $go_terms);
		push @iprscan_results, \@row;
		$total++;
	} 
	close IPRSCAN;
	
    # insert data
	if ( !defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@iprscan_results ) ) {
		$errorMessage = "_loadIprscan: " . $errorMessage;
		return undef;
	}

	print &rpad(" ",10) . &lpad(chr(8),10,chr(8));

    # return number of hits
    return $total;	
			
}

sub _loadPriam {
	my ( $dbh, $compute)= @_;

	my $path = $$compute{result_message};
	
# get hash of existing query sequences
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$errorMessage = "_loadPriam: " . $errorMessage;
			return undef;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}

# get EC definitions
	my %ecdef;
	open( ECDEF, "cut -f 6,12 $path/priam_ec_hits.ectab.parsed | sort | uniq |" );
	while ( my $line = <ECDEF> ) {
		chomp $line;
		my ( $ecDefinition, $ecNum ) = split /\t/, $line;
		$ecdef{$ecNum} = $ecDefinition;
	}
	close( ECDEF );
	
# get EC assignments
	my $total = 0;
	my @ecassignments = ();
	open( ECASSIGNMENTS, "$path/priam_ec_hits.ectab" );
	while ( my $line = <ECASSIGNMENTS> ) {
		chomp $line;
		my ( $queryId, $ecNum, undef, $evalue, $bitScore, $queryBegin, $queryEnd ) = split /\t/, $line;
		my $querySeqId = $$querySeqs{$queryId}{seq_id};
		my $ecDefinition = $ecdef{$ecNum};
		my @row = ( $$compute{job_id}, $querySeqId, $ecNum, $ecDefinition, $evalue, $bitScore, $queryBegin, $queryEnd );
		push @ecassignments, \@row;
		$total++;
	} 
	
# define database
# !!! to use sqlite import (currently commented out) hitcols order must match btab table !!!
	my $hittab = "priam";
	my @hitcols = split( /,/,
		"job_id,query_seq_id,ec_num,ec_definition,evalue,bit_score,query_begin,query_end" );

# insert data
	if ( !defined &_bulkInsert2( $dbh, $hittab, \@hitcols, \@ecassignments ) ) {
		$errorMessage = "_loadPriam: " . $errorMessage;
		return undef;
	}

	print &rpad(" ",10) . &lpad(chr(8),10,chr(8));

# return number of hits
	return $total;	
}

# insert array of data into a table
sub _bulkInsert {
	my ( $dbh, $data, $tablename, $cols ) = @_;

	my $columns = join( ",", @$cols );
	my $values = join( ",", split( //, &rpad( "?", scalar @$cols, "?" ) ) );
	my $stmt = "insert into " . $tablename . "(" . $columns . ") values (" . $values . ")";
	my $total = 0;

	my @buffer = ();
	foreach my $item ( @$data ) {
		my @row = &extractArrayFromHash( $item, @$cols );
		push( @buffer, \@row );
		if ( scalar @buffer >= 10000 ) {
#print "_bulkInsert: " . $tablename . ": " . scalar @buffer . " of " . scalar @$data . "\n";
			my $insert = &bulkInsertData( $dbh, $stmt, \@buffer );
			if ( !defined $insert ) {
				$errorMessage = "_bulkInsert (1): " . $errorMessage;
				return undef;
			}
			$total += $insert;
			@buffer = ();
		} 
	}
	if ( scalar @buffer > 0 ) {
#print "_bulkInsert: " . $tablename . ": " . scalar @buffer . " of " . scalar @$data . "\n";
		my $insert = &bulkInsertData( $dbh, $stmt, \@buffer );
		if ( !defined $insert ) {
			$errorMessage = "_bulkInsert (2): " . $errorMessage;
			return undef;
		}	
		$total += $insert;
	}

	return $total;
}

# insert array of data into a table
sub _bulkInsert2 {
	my ( $dbh, $tablename, $cols, $data ) = @_;


	my $total = 0;
	if ( scalar @$data > 0 ) {
		my $columns = join( ",", @$cols );
		my $values = join( ",", split( //, &rpad( "?", scalar @$cols, "?" ) ) );
		my $stmt = "insert into " . $tablename . "(" . $columns . ") values (" . $values . ")";

		my $insert = &bulkInsertData( $dbh, $stmt, $data );
		if ( !defined $insert ) {
			$errorMessage = "_bulkInsert (2): " . $errorMessage;
			return undef;
		}	
		$total += $insert;
	}

	return $total;
}


# run pepstats locally and load the results
sub runPepstats {
	my ( $dbh, $compute ) = @_;	

	$dbh->begin_work;
	if ( !defined &executeSQL( $dbh, "update job set date_submitted=? where job_id=?", &now, $$compute{job_id} ) ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}

# get query fasta
	my $querydataset = &getDatasetVersion( $dbh, $$compute{query_db_id} );
	if ( !defined $querydataset ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}
	
	my $queryfasta = &getDatasetVICSFasta( $querydataset );
#print "PEPSTATS query fasta: $queryfasta\n";
	if ( !defined $queryfasta ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}

# get query sequences
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$dbh->rollback;
			my $errortext = "runPepstats: " . $errorMessage;
			$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
			if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
			return $compute;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}

# run pepstats
#print "pepstats -sequence $queryfasta -outfile stdout 2> /dev/null |";
	if ( !open (PEPSTATS, "pepstats -sequence $queryfasta -outfile stdout 2> /dev/null |") ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}

# read results
	my ( $seq_id, $protein_id, $mol_wt, $ip, @results );
	while ( my $line = <PEPSTATS> ) {
		if ( $line =~ m/^PEPSTATS\sof\s(\S+)\sfrom/ ) {
			$protein_id = $1;
			if ( defined $seq_id ) {
				my @row = ( $$compute{job_id}, $seq_id, $mol_wt, $ip );
				push( @results, \@row );
				$ip = undef;
				$mol_wt = undef;
			}
			$seq_id = $$querySeqs{$protein_id}{seq_id};
		}
		if ( $line =~ m/^Isoelectric\sPoint\s=\s(\d+\.?\d*)\s*/ ) {
			$ip = $1;
		}
		if ( $line =~ m/^Molecular\sweight\s=\s(\d+\.?\d*)\s*/ ) {
			$mol_wt = $1;
		}
	}
	close(PEPSTATS);
	if ( defined $seq_id ) {
		my @row = ( $$compute{job_id}, $seq_id, $mol_wt, $ip );
		push( @results, \@row );
	}
	
# save results to database
	my $insert = &bulkInsertData( $dbh,
		"insert into pepstats(job_id,query_seq_id,molecular_weight,isoelectric_point) values(?,?,?,?)",
		\@results );
	if ( !defined $insert ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}
			
# update compute
	$$compute{status} = "loaded";
	$$compute{num_results} = $insert;
	$$compute{date_completed} = &now;

	my $update = &executeSQL( $dbh,
		"update job set status=?, num_results=?, date_completed=? where job_id=?",
		$$compute{status}, $$compute{num_results}, $$compute{date_completed}, $$compute{job_id} );
	if ( !defined $update ) {
		$dbh->rollback;
		my $errortext = "runPepstats: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPepstats: " .. $errorMessage }
		return $compute;
	}
	$dbh->commit;
	
	return $compute;
}


#
# print report
sub displayComputes{
	my ( $computes ) = @_;
	if (  scalar keys %$computes == 0 ) {
		print "No jobs.\n";
	} else {
		print &rpad("Job Name",18) . " "
			. &rpad("Status",9) . " "
			. &lpad("#Hits",8) . "  "
			. &cpad("Submitted",16) . " "
			. &lpad("Completed",11) . " | "
			. &rpad("Program -opts",22) . " | "
			. &rpad("Query",15) . " "
			. &lpad("Size",7) . " | "
			. &rpad("Subject",22) . " "
			. &lpad("Size",9) . "\n";
		print &rpad("-",18,"-") . "-"
			. &rpad("-",9,"-") . "-"
			. &lpad("-",8,"-") . "--"
			. &cpad("-",16,"-") . "-"
			. &lpad("-",11,"-") ."-+-"
			. &rpad("-",22,"-") . "-+-"
			. &rpad("-",15,"-") . "-"
			. &lpad("-",7,"-") . "-+-"
			. &rpad("-",22,"-") . "-"
			. &lpad("-",9,"-") . "\n";
		my $today = &today . ":";
		foreach my $compute ( sort { $$a{job_name} cmp $$b{job_name}} values %$computes ) {
			my ( $subject_db_name ) = split( "::", $$compute{subject_db_name} );
			
			my $status = $$compute{status};
			my $program = $$compute{program_name};
			my $options = $$compute{program_options};
			$options =~ s/\~\~/ /g; 
			if ( length( $options ) > 0 ) { $program .= " " . $options }

			my ( $cyear, $cdate, $ctime, $syear, $sdate, $stime );
			if ( length( $$compute{date_completed} == 0 ) ) {
				$ctime = "";
				$stime = $$compute{date_submitted};		
			} else {
				( $cdate, $ctime ) = split( / /, $$compute{date_completed} );
				if ( !defined $ctime ) {
					 ( $cdate, my $hr, my $min, my $sec ) = split( /:/, $$compute{date_completed} );
					 $ctime = $hr . ":" . $min . ":" . $sec;
				}
				( $cyear ) = split( /-/, $cdate );
				( $sdate, $stime ) = split( / /, $$compute{date_submitted} );
				if ( !defined $stime ) {
					 ( $sdate, my $hr, my $min, my $sec ) = split( /:/, $$compute{date_submitted} );
					 $stime = $hr . ":" . $min . ":" . $sec;
				}
				( $syear ) = split( /-/, $sdate );
				if ( $cdate ne $sdate ) {
					$ctime = substr($cdate,5) . " " . $ctime;
				}
				$stime = $sdate . " " . $stime;
			}
			$stime = substr($stime,0,length($stime)-3);						
			$ctime = substr($ctime,0,length($ctime)-3);						
			
			print &rpad($$compute{job_name},18) . " "
				. &rpad($status,9) . " "
				. &lpad( (defined $$compute{num_results} ? $$compute{num_results} : "NA"),8) . "  "
				. &cpad($stime,16) . " "
				. &lpad($ctime,11) . " | "
				. &rpad($program,22) . " | "
				. &rpad($$compute{query_db_name},15) . " "
				. &lpad($$compute{query_db}{content_count},7) . " | "
				. &rpad($subject_db_name,22) . " "
				. &lpad($$compute{subject_db}{content_count},9) . "\n";
		}
	}
}

sub runPriam {
	my ( $dbh, $compute ) = @_;

	my $total_results = 0;
	my $jobid = $$compute{job_id};
	my $options = $$compute{program_options};
	$options =~ s/\~\~/ /g;
	$dbh->begin_work;
	if ( !defined &executeSQL( $dbh, "update job set date_submitted=? where job_id=?", &now, $$compute{job_id} ) ) {
		$dbh->rollback;
		my $errortext = "runPriam: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

# get query fasta
	my $querydataset = &getDatasetVersion( $dbh, $$compute{query_db_id} );
	if ( !defined $querydataset ) {
		$dbh->rollback;
		my $errortext = "runPriam: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}
	
	my $queryfasta = &getDatasetVICSFasta( $querydataset );
	if ( !defined $queryfasta ) {
		$dbh->rollback;
		my $errortext = "runPriam: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

# get query sequences id map
	my $querySeqs;
	if ( $$compute{query_db_id} > 0 ) {
		$querySeqs = &getDatasetSeqAccs( $dbh, $$compute{query_db_id} );
		if ( !defined $querySeqs ) {
			$dbh->rollback;
			my $errortext = "runPriam: " . $errorMessage;
			$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
			if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
			return $compute;
		} elsif ( $querySeqs == 0 ) {
			$querySeqs = undef;
		}
	}

# perform rpsblast against priam database
	my $rpstab = "$scratchspace/priam.rps.tab.$$";
	system( "rpsblast -i $queryfasta -d /usr/local/db/calit_db/PRIAM/rpsblast/profile_EZ -m 8 -e 1e-10 > $rpstab");
	if ( ! -e $rpstab ) {
		$dbh->rollback;
		my $errortext = "runPriam: rpsblast failed.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

# analyze results and assign ec numbers
	my $ectab = "$scratchspace/priam.ec.tab.$$";
	system( "/usr/local/devel/ANNOTATION/CALIT2-CAMERA-2009-03-06/jboss-test/bin/create_ec_list --rps --hits $rpstab --output $ectab > /dev/null" );
	unlink $rpstab;
	if ( ! -e $ectab ) {update node set 
		$dbh->rollback;
		my $errortext = "runPriam: analysis failed.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

# get ec definitions
	my $ecdefraw = "$scratchspace/priam.ec.def.raw.$$";
	system( "/usr/local/devel/ANNOTATION/CALIT2-CAMERA-2009-03-06/jboss-test/bin/camera_parse_annotation_results_to_text_table --input_file $ectab --input_type=ECTable --output_file=$ecdefraw --work_dir=. > /dev/null" );
	if ( ! -e $ecdefraw ) {
		$dbh->rollback;
		my $errortext = "runPriam: could not generate raw definitions file.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

	my $ecdefunq = "$scratchspace/priam.ec.def.unq.$$";
	my $cmd = "cat $ecdefraw | awk ' BEGIN { FS=\"\\t\"; OFS=\"\\t\" }{ print \$6,\$12}' | sort | uniq > $ecdefunq";
	system ( $cmd );
	unlink $ecdefraw;
	if ( ! -e $ecdefunq ) {
		$dbh->rollback;
		my $errortext = "runPriam: could not generate unique definitions file.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}

# merge definitions with ec assignments
	my %ecdef;
	if ( ! open( DEF, "<$ecdefunq" ) ) {
		unlink $ecdefunq;
		$dbh->rollback;
		my $errortext = "runPriam: could not open unique definitions file.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}
	while ( my $line = <DEF> ) {
		$line =~ s/[\n\r]//g;
		my ( $ecdef, $ecnum ) = split( /\t/, $line );
		$ecdef{$ecnum} = $ecdef;
	} 
	close( DEF );	
	unlink $ecdefunq;

	my $total = 0;
	my $cnt = 0;
	my @results = ();
	if ( ! open( ECTAB, "<$ectab" ) ) {
		unlink $ectab;
		$dbh->rollback;
		my $errortext = "runPriam: could not open ec assigments.";
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}
	while ( my $line = <ECTAB> ) {
		$line =~ s/[\n\r]//g;
		my ( $query_id, $ecnum, undef, $evalue, $bit_score, $query_begin, $query_end ) = split( /\t/, $line );
		my $query_seq_id = $$querySeqs{$query_id}{seq_id};
		my $ec_definition = $ecdef{$ecnum};
		my @row = ( $$compute{job_id}, $query_seq_id, $ecnum, $ec_definition, $evalue, $bit_score, $query_begin, $query_end );
		push ( @results, \@row );
		$cnt++;

# save to db
		if ( $cnt >= 10000 ) {
			my $insert = &bulkInsertData( $dbh,
				"insert into priam(job_id,query_seq_id,ec_num,ec_definition,evalue,bit_score,query_begin,query_end )"
					."values(?,?,?,?,?,?,?,?)",
				\@results);
			if ( !defined $insert ) {
				unlink $ectab;
				$dbh->rollback;
				my $errortext = "runPriam: insert A: " . $errorMessage;
				$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
				if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
				return $compute;
			}
			$total += $cnt;
			@results = ();
			$cnt = 0;
		}
	}	
	close( ECTAB );
	unlink $ectab;

	if ( $cnt > 0 ) {
		my $insert = &bulkInsertData( $dbh,
			"insert into priam(job_id,query_seq_id,ec_num,ec_definition,evalue,bit_score,query_begin,query_end )"
				."values(?,?,?,?,?,?,?,?)",
			\@results);
		if ( !defined $insert ) {
			$dbh->rollback;
			my $errortext = "runPriam: insert B: " . $errorMessage;
			$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
			if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
			return $compute;
		}
		$total += $cnt;
		@results = ();
		$cnt = 0;
	}

# update compute
	$$compute{status} = "loaded";
	$$compute{num_results} = $total;
	$$compute{date_completed} = &now;

	my $update = &executeSQL( $dbh,
		"update job set status=?, num_results=?, date_completed=? where job_id=?",
		$$compute{status}, $$compute{num_results}, $$compute{date_completed}, $$compute{job_id} );
	if ( !defined $update ) {
		$dbh->rollback;
		my $errortext = "runPriam: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runPriam: " .. $errorMessage }
		return $compute;
	}
	$dbh->commit;
	
	return $compute;
}

sub runRpsBlast {
	my ( $dbh, $compute ) = @_;

	my $total_results = 0;
	my $jobid = $$compute{job_id};
	$dbh->begin_work;
	if ( !defined &executeSQL( $dbh, "update job set date_submitted=? where job_id=?", &now, $$compute{job_id} ) ) {
		$dbh->rollback;
		my $errortext = "runPriam: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}

# get subject database
	my $subjectdataset = &getDatasetVersion( $dbh, $$compute{subject_db_id} );
	if ( !defined $subjectdataset ) {
		$dbh->rollback;
		my $errortext = "runRpsBlast: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}
	my $subjectpath = &getDatasetNodePath( $subjectdataset );
	my $subjectdb = `ls -1 $subjectpath/*.rps`;
	$subjectdb =~ s/[\r\n]//g;
	$subjectdb =~ s/.rps$//;
	
# get query fasta
	my $querydataset = &getDatasetVersion( $dbh, $$compute{query_db_id} );
	if ( !defined $querydataset ) {
		$dbh->rollback;
		my $errortext = "runRpsBlast: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}
	
	my $queryfasta = &getDatasetVICSFasta( $querydataset );
	if ( !defined $queryfasta ) {
		$dbh->rollback;
		my $errortext = "runRpsBlast: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}

# get program options
	my $options = $$compute{program_options};
	$options =~ s/\~\~/ /g;
	$options =~ s/-e /-e 1e/;
	
# format rpsblast command
	my $command = "rpsblast -i $queryfasta -d $subjectdb $options -m 7 |";
	my $num_results = &_loadBlastXML( $dbh, $compute, $command );
	if ( !defined $num_results ) {
		$dbh->rollback;
		my $errortext = "runRpsBlast: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}

# update compute
	$$compute{status} = "loaded";
	$$compute{num_results} = $num_results;
	$$compute{date_completed} = &now;

	my $update = &executeSQL( $dbh,
		"update job set status=?, num_results=?, date_completed=? where job_id=?",
		$$compute{status}, $$compute{num_results}, $$compute{date_completed}, $$compute{job_id} );
	if ( !defined $update ) {
		$dbh->rollback;
		my $errortext = "runRpsBlast: " . $errorMessage;
		$compute = &updateComputeStatus( $dbh, $compute, "error", "Error Message", $errortext );
		if ( !defined $compute ) { $errorMessage = "runRpsBlast: " .. $errorMessage }
		return $compute;
	}
	$dbh->commit;
	
	return $compute;
}

sub _createJobPartition {
	my ( $dbh, $job ) = @_;

	my $dbfile = &getSessionDBFile( $dbh );
	my $partitionfile = $dbfile . "." . $$job{job_name} . "." . $$job{job_id};

	system("cp " . $dbfile . " " . $partitionfile );

	my $pdbh = &connectSQLite( $partitionfile, my $autocommit = 1 );
	$pdbh->begin_work;
	&executeSQL( $pdbh, "delete from job where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from btab where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from htab where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from pepstats where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from priam where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from signalp where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh, "delete from tmhmm where job_id <> ?", $$job{job_id} );
	&executeSQL( $pdbh,
		"delete from dataset_version where version_id not in (coalesce(?,0),coalesce(?,0),0)",
		$$job{subject_db_id}, $$job{query_db_id} );
	&executeSQL( $pdbh, "delete from dataset where dataset_name not in (select dataset_name from dataset_version)" );
	&executeSQL( $pdbh, "delete from dataset_seq where version_id not in (select version_id from dataset_version)" );
	&executeSQL( $pdbh, "delete from dataset_seq_attr where version_id not in (select version_id from dataset_version)" );
	&executeSQL( $pdbh, "delete from hmm_annotation where version_id not in (select version_id from dataset_version)" );
	$pdbh->commit;
	&executeSQL( $pdbh, "vacuum" );

	return $pdbh;
}

sub resetCompute {
	my ( $compute ) = @_;
	
	$$compute{status} = undef;
	$$compute{date_submitted} = undef;
	$$compute{date_completed} = undef;
	$$compute{result_type} = undef;
	$$compute{result_mesdsage} = undef;
	$$compute{num_results} = undef;
}

sub sortBlastHits {
	my ( $hitsfile, $slicesize ) = @_;
	
	my $sortkey = "-k1g -k2gr -T $scratchspace";
	
# break input into manageable slices
	if ( !defined $slicesize ) { $slicesize = 100000 }
	my $slicename = "$hitsfile.$$.slice";
	system "split -l $slicesize -a 5 -d  $hitsfile $slicename.";
	unlink $hitsfile;

# sort each slice
	my $tmp = `ls -1 $slicename.*`;
	my @tmpfiles = split( /\n/, $tmp );
	my @slices;
	foreach my $tmpfile ( @tmpfiles ) {
		my $sortfile = "$tmpfile.sorted";
		system "sort $sortkey $tmpfile -o $sortfile";
		unlink $tmpfile;
		push @slices, $sortfile;
	}

# merge slices
	my $iteration = 0;

# open next two files and read first line from each file
	while ( scalar @slices > 1 ) {
		my $file1 = shift @slices;
		open(FILE1, "<$file1");
		my $file2 = shift @slices;
		open(FILE2, "<$file2");

		my @values1;
		my $line1 = <FILE1>;
		if ( !defined $line1 ) {
			close FILE1;
		} else {
			@values1 = split /\t/, $line1;
		}

		my @values2;
		my $line2 = <FILE2>;
		if ( !defined $line2 ) {
			close FILE2;
		} else {
			@values2 = split /\t/, $line2;
		}

# open temporary file for merged results
		my $tmpfile = "$hitsfile.$$.merged." . $iteration++ ;
		open( TMP, ">$tmpfile" );
	
# compare current input lines
		while ( defined $line1 || defined $line2 ){

# if both files are exhausted, this iteration is done
			if ( !defined $line1 ) {
				while ( defined $line2 ) {
					print TMP $line2;
					$line2 = <FILE2>;
				}
				close FILE2;
				close( TMP );
				unlink $file2;
				push @slices, $tmpfile;

# if file2 is exhausted, write remaining lines from file1
			} elsif ( !defined $line2 ) {
				while ( defined $line1 ) {
					print TMP $line1;
					$line1 = <FILE1>;
				}
				close FILE1;
				close( TMP );
				unlink $file1;
				push @slices, $tmpfile;
			
# else write input line which sorts to top and read next line of input
			} else {
				my $first;
				if ( $values1[0] < $values2[0] ) {
					$first = 1;
				} elsif ( $values2[0] < $values1[0] ) {
					$first = 2;
				} elsif ( $values1[1] > $values2[1] ) {
					$first = 1;
				} elsif ( $values2[1] > $values1[1] ) {
					$first = 2;
				} elsif ( $line1 le $line2 ){
					$first = 1;
				} else {
					$first = 2;
				}
				if ( $first == 1 ) {
					print TMP $line1;
					$line1 = <FILE1>;
					if ( !defined $line1 ) {
						close FILE1;
						unlink $file1;
					} else {
						@values1 = split /\t/, $line1;
					}
				} else {
					print TMP $line2;
					$line2 = <FILE2>;
					if ( !defined $line2 ) {
						close FILE2;
						unlink $file2;
					} else {
						@values2 = split /\t/, $line2;
					}
				}
			}
		}
	}

# rename final result file to original input name
	rename $slices[0], $hitsfile;
	return 1;
}
1;
