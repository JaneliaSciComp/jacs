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

use warnings;
use strict;

use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');

use EAP::compute;
use EAP::db;
use Data::Dumper;
use TIGR::Foundation;

our $pfam2gotxt;
our $errorMessage;    # db.pm 'throws' this around, often with something informative inside.

my $DEFAULT_NAME = 'hypothetical protein';
our $TF;

sub get_best_evidence {

	my ( $dbh, $output, $job_id, $job_name, $query_id, $query_seq_acc, $query_primary_seq_id,
		 $query_secondary_seq_id )
		= @_;

	my $best_hits;    # store best results for all ORFs here

	my ( $query_key_list, $print_key_list, $query_table, $coverage )
		= &get_query_print_key_list($job_name);

	my $evidence_query;

	if ($query_primary_seq_id) {
		$evidence_query =
			&get_evidence_query_split( $query_key_list, $query_seq_acc, $job_id, $query_table,
									   $job_name, $query_primary_seq_id, $query_secondary_seq_id );
	}
	else {
		$evidence_query
			= &get_evidence_query( $query_key_list, $query_id, $job_id, $query_table, $job_name );
	}

	my $sth = $dbh->prepare($evidence_query);

	$sth->execute();

	my %print_row;
	my %fetchRow;
	my @evidence;
	my $sorted_array;

	push @$query_key_list, "iso_type" if ( $job_name eq 'PFAM/TIGRFAM_HMM' );
	push @$query_key_list, "ec_num"   if ( $job_name eq 'PFAM/TIGRFAM_HMM' );

	while ( @fetchRow{@$query_key_list} = $sth->fetchrow_array() ) {
		my %row = %fetchRow;

		#Calculate coverage when necessary
		if ( $coverage eq 'blast' ) {
			$row{coverage}
				= &calculate_blast_coverage( $row{alignment_length}, $row{query_length} );
		}
		elsif ( $coverage eq 'hmm' ) {
			$row{coverage}
				= &calculate_hmm_coverage( $row{hmm_begin}, $row{hmm_end}, $row{hmm_len} );
		}
		push @evidence, \%row;
	}

	$sorted_array = &select_best_hit( \@evidence );

	$sth->finish();

	if ( scalar $sorted_array ) {
		return ( $sorted_array, $print_key_list );
	}
	else {
		return ( undef, $print_key_list );
	}

}

sub create_annotation {

	# Given the hash reference returned by get best evidence, select the
	# best possible name, etc., from the provided searches/computes.

	my ( $orf, $pfam2go ) = @_;

	my $temp_name;

	( $temp_name, $orf->{ANNOTATION}->{name_ev} ) = &apply_naming_rules($orf);

	$orf->{ANNOTATION}->{name} = &cleanup_name( $temp_name, $orf->{ANNOTATION}->{name_ev} );

	( $orf->{ANNOTATION}->{ec_number}, $orf->{ANNOTATION}->{ec_number_ev} )	= &apply_ec_assignment($orf);

	( $orf->{ANNOTATION}->{go_terms} ) = &apply_go_term_assignment( $orf, $pfam2go );

	( $orf->{ANNOTATION}->{env_nt} ) = &apply_env_nt_assignments($orf);

	&find_all_pfams_above_cutoff($orf);

}

sub apply_naming_rules {

	# Given an orf hash, look at the various evidences involved in the
	# naming of this orf, and go through a set of rules to do so.
	# Avoid seleting names like 'unknown', hypothetical, etc.
	my $orf  = shift;
	my $name = $DEFAULT_NAME;    # all ORFS begin as this.
	my $reference;

	# 1.  Equivalog TIGRfam HMM hits above trusted cutoff, full-length HMM
	# (100% coverage). Add ", putative" at the end of the name
	if ( &get_equivalog_tigrfam_hmm( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 2.  High confidence blastp hits against AllGroup.niaa, coverage >= 80%,
		# %-identity >= 50%, e-value <= 1e-10.  Add ", putative" at the end of the
		# name.
	}
	elsif ( &get_high_conf_blastp( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 3. High quality ACCLAME-HMM matches.  >90% coverage, e-value < 1e-5.
	}
	elsif ( &get_aclame_hmm( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 4. Non-equivalog TIGRfam/PFAM HMM hits above trusted cutoff, 100% coverage
		# of HMM (priorities: subfamily > superfamily > domain-contatingin protein).
		# Accordingly, add "[sub|super]family protein" / "domain-containing protein"
		#  at the end of the name.
	}
	elsif ( &get_non_equivalog_tigrfam_hmm( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 5. CDD hits, coverage >= 90% of the CDD-domain, e-value <= 1e-10,
		# %-identity >= 35%.  Add "domain-containing protein" at the end of the name.
	}
	elsif ( &get_CDD_hits( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 6. FragHMM hits, ignoring coverage, but e-value < 1e-5
	}
	elsif ( &get_frag_hmm( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 7. Low confidence blastp hits against AllGroup.niaa, coverage >= 70%,
		# %-identity >= 30%, e-value <= 1e-5
	}
	elsif ( &get_low_conf_blastp( $orf, \$name, \$reference ) ) {

		return ( $name, $reference );

		# 8. Any other evidence -> "hypothetical protein".
	}
	elsif ( &get_other_evidence( $orf, \$name ) ) {

		return ( $name, "Other" );

		# 9. No evidence => "unknown protein".
	}
	else {
		return ( "unknown protein", "No Evidence" );
	}

}

sub cleanup_name {

	# Perform several checks on names to make sure that they meet a certain
	# level of quality before ushering them out into the annotation file.

	my ( $name, $name_ev ) = @_;

	# From Hernan:
	#
	# For those names ending in "activity" replace that word by "protein,
	# putative" instead of just ", putative", except when the word preceeding
	# activity ends with "ase" (enzyme name). In that case add just ", putative".
	# For the remaining names, add "protein, putative".
	#
	# In other words: remove trailing activity.  Add ' protein, putative' unless
	# it now ends in 'ase', in which case add only ', putative'
	if ( $name_ev eq 'ACLAME' ) {
		$name =~ s/ activity$//;
		$name .= ( $name =~ /(protein|ase)$/ ) ? ', putative' : ' protein, putative';
	}

	# Append "-containing protein" when name ends with "domain" or "repeat"
	if ( $name =~ /(domain|repeat)$/ ) {
		$name .= '-containing protein';
	}

	# if name ends with "repeat (20 copies)" then remove "(X copies)" and
	# add "containing protein"
	$name =~ s/\(\d+ copies\)$/-containing protein/;

	# If first word of name has 4 or more characters (including only [a-z] and
	# [A-Z]) and first letter is uppercase and the rest lowercase then convert
	# all to lowercase.
	if ( $name =~ /^([a-zA-Z]+)\s/ ) {

		my $first_word = $1;

		if ( length($first_word) >= 4 ) {

			if ( ( substr( $first_word, 0, 1 ) eq uc( substr( $first_word, 0, 1 ) ) )
				 && ( substr( $first_word, 1, length($first_word) - 1 ) eq
					  lc( substr( $first_word, 1, length($first_word) - 1 ) ) )
				)
			{

				$name = lc($name);

			}

		}

	}

	# If name doesn't end with '\w*family','\w{3,}ase','subunit', 'protein',
	# 'domain', 'repeat'
	# ... add putative?
	if ( $name !~ /(\w*family|\w{3,}ase|subunit|protein|domain|repeat|putative)$/ ) {
		$name .= ', putative';
	}

	return $name;

}

sub get_equivalog_tigrfam_hmm {

	# select the best available tigrfam that has an isology type of equivalog
	my ( $orf, $name, $reference ) = @_;


	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {
		my @hit_sort = sort sort_tigrfam @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} };

		foreach my $hit (@hit_sort) {

			next unless $hit->{'iso_type'};    # skip empties.  we don't care for those here.

			# In fact, we only care about these:
			if (    ( $hit->{'iso_type'} eq "equivalog" )
				 || ( $hit->{'iso_type'} eq "equivalog_domain" )
				 || ( $hit->{'iso_type'} eq "PFAM_equivalog" )
				 || ( $hit->{'iso_type'} eq "PFAM_equivalog_domain" ) )
			{

				if (    ( $hit->{coverage} == 1 )
					 && ( $hit->{total_score} >= $hit->{trusted_cutoff} ) )
				{

					$$name      = $hit->{hmm_description};
					$$reference = "PFAM/TIGRFAM:$hit->{hmm_acc}";
					return ( $$name, $$reference );

				}

			}
		}
	}
	return 0;
}

sub get_high_conf_blastp {

	# Select the best blastp that satisfies 'high' confidence conditions
	my ( $orf, $name, $reference ) = @_;

	if ( exists $orf->{'ALLGROUP_PEP'}->{hits} ) {
		foreach my $hit ( @{ $orf->{'ALLGROUP_PEP'}->{hits} } ) {

			unless ( $hit->{subject_definition} eq $hit->{subject_id} ) {

				if (    ( $hit->{coverage} >= .8 )
					 && ( $hit->{pct_identity} >= 50 )
					 && ( $hit->{evalue} <= 1e-10 ) )
				{

					$$name      = &parse_subject($hit);
					$$reference = "AllGroup High:$hit->{subject_id}";
					if ( ( $$name !~ /hypothetical|putative|unknown/ ) && ( $$name !~ /^\d+$/ ) ) {
						( return $$name, $$reference );
					}

					$$name = $DEFAULT_NAME;
					next;
				}
			}
		}
	}

	return 0;

}

sub get_aclame_hmm {

	# Select name from ACLAME-HMM hits
	my ( $orf, $name, $reference ) = @_;

	if ( exists $orf->{'ACLAME_HMM'}->{hits} ) {

		foreach my $hit ( @{ $orf->{'ACLAME_HMM'}->{hits} } ) {

			if (    ( $hit->{coverage} >= .9 )
				 && ( $hit->{total_evalue} <= 1e-5 ) )
			{

				$$name      = &parse_subject($hit);
				$$reference = "ACLAME:$hit->{hmm_acc}";
				if ( $$name !~ /hypothetical|putative|unknown/ ) {
					return ( $$name, $$reference );
				}

				$$name = $DEFAULT_NAME;
				next;

			}
		}
	}

	return 0;

}

sub get_non_equivalog_tigrfam_hmm {

	# Check for tigrfams again, this time, for non-equivalogs.
	my ( $orf, $name, $reference ) = @_;
	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {

		my @hmm_sort = sort sort_tigrfam @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} };

		foreach my $hit ( @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} } ) {

			if (    ( $hit->{coverage} == 1 )
				 && ( $hit->{total_score} >= $hit->{trusted_cutoff} ) )
			{

				# begin pre-formatting by stripping words that will look funny
				$$name = $hit->{hmm_description};
				$$name =~ s/ putative$//;
				$$name =~ s/ family$//;
				$$name =~ s/ protein$//;
				$$name =~ s/ domain$//;

				$$reference = "PFAM/TIGRFAM:$hit->{hmm_acc}";

				if ( $hit->{'iso_type'} eq "subfamily" || $hit->{'iso_type'} eq "subfamily_domain" )
				{

					$$name .= " subfamily protein";
					return ( $$name, $$reference );

				}
				elsif ( $hit->{'iso_type'} eq "superfamily" ) {

					$$name .= " superfamily protein";
					return ( $$name, $$reference );

				}
				elsif ( $hit->{'iso_type'} eq "domain" ) {

					$$name .= " domain-containing protein";
					return ( $$name, $$reference );

				}
			}
		}
	}

	# reassign back to default since we had an unsatisfying time above :(
	$$name = $DEFAULT_NAME;
	return 0;

}

sub find_all_pfams_above_cutoff {

	my ($orf) = @_;

	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {

		my @pfams = ();
		foreach my $hit ( @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} } ) {

			if (    ( $hit->{coverage} == 1 )
				 && ( $hit->{total_score} >= $hit->{trusted_cutoff} ) )
			{

				push( @pfams, $hit->{hmm_acc} );
			}
		}

		$orf->{'PFAM/TIGRFAM_HMM'}->{'all_hits'} = \@pfams;
	}

	return 0;

}

sub sort_tigrfam{
    my %map = ( 'equivalog' => 0,
                'equivalog_domain' => 1,
                'PFAM_equivalog' => 2,
                'PFAM_equivalog_domain' => 3,
                'subfamily' => 4,
                'domain' => 5,
                'subfamily_domain' => 6,
                'superfamily' => 7
                );

    my $c = (defined $a->{iso_type}) ? $map{$a->{iso_type}} : 8;
    my $d = (defined $b->{iso_type}) ? $map{$b->{iso_type}} : 8;
 
    $c = 8 unless $c;
    $d = 8 unless $d;
    
    return $c <=> $d;   
        
}

sub get_CDD_hits {

	# If there are any CDD hits of sufficient quality, use the best one
	# as the name.
	my ( $orf, $name, $reference ) = @_;

	if ( exists $orf->{'CDD_RPS'}->{hits} ) {

		foreach my $hit ( @{ $orf->{'CDD_RPS'}->{hits} } ) {

			if (    ( $hit->{coverage} >= 9 )
				 && ( $hit->{pct_identity} >= 35 )
				 && ( $hit->{evalue} <= 1e-10 ) )
			{

				$$name      = &parse_subject($hit);
				$$reference = "CDD:$hit->{subject_id}";

				if ( $$name !~ /hypothetical|putative|unknown/ ) {

					$$name .= " domain-containing protein";

					return ( $$name, $$reference );
				}

				$$name = $DEFAULT_NAME;
				next;

			}

		}
	}

	return 0;

}

sub get_frag_hmm {

	# Get name from frag hmm hits.
	my ( $orf, $name, $reference ) = @_;

	if ( exists $orf->{'FRAG_HMM'}->{hits} ) {

		foreach my $hit ( @{ $orf->{'FRAG_HMM'}->{hits} } ) {

			if ( $hit->{total_evalue} <= 1e-5 ) {

				$$name      = $hit->{hmm_description};
				$$reference = "FRAG_HMM:$hit->{hmm_acc}";

				if ( $$name !~ /hypothetical|putative|unknown/ ) {
					return ( $$name, $$reference );
				}

				$$name = $DEFAULT_NAME;
				next;

			}
		}
	}

	return 0;
}

sub get_low_conf_blastp {

	# If there are any low conf blastp hits, retrieve the best one
	# to use as the name.

	my ( $orf, $name, $reference ) = @_;

	if ( exists $orf->{'ALLGROUP_PEP'}->{hits} ) {

		foreach my $hit ( @{ $orf->{'ALLGROUP_PEP'}->{hits} } ) {

			unless ( $hit->{subject_definition} eq $hit->{subject_id} ) {

				if (    ( $hit->{coverage} >= .7 )
					 && ( $hit->{pct_identity} >= 30 )
					 && ( $hit->{evalue} <= 1e-5 ) )
				{

					$$name      = &parse_subject($hit);
					$$reference = "ALLGROUP Low:$hit->{subject_id}";
					if ( $$name !~ /hypothetical|putative|unknown/ && ( $$name !~ /^\d+$/ ) ) {
						return ( $$name, $$reference );
					}

					$$name = $DEFAULT_NAME;
					next;

				}
			}
		}
	}

	return 0;

}

sub get_other_evidence {

	# Get any evidence types not yet looked at for the orf.  return 0 if
	# none has been found
	my ( $orf, $name ) = @_;
	if (    ( exists( $orf->{'ALLGROUP_PEP'}->{hits} ) )
		 || ( exists( $orf->{'ENV_NR'}->{hits} ) )
		 || ( exists( $orf->{'ENV_NT'}->{hits} ) )
		 || ( exists( $orf->{'ACLAME_HMM'}->{hits} ) )
		 || ( exists( $orf->{'ACLAME_PEP'}->{hits} ) )
		 || ( exists( $orf->{'CDD_RPS'}->{hits} ) )
		 || ( exists( $orf->{'FRAG_HMM'}->{hits} ) )
		 || ( exists( $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) )
		 || ( exists( $orf->{'SANGER_PEP'}->{hits} ) ) )
	{

		$$name = 'hypothetical protein';
		return $name;
	}
	return 0;

}

sub apply_ec_assignment {

	# Given an orf hash, look at the various evidences involved in the
	# assignment of an ec number to this orf, and go through a set of
	# rules to do so.

	my $orf = shift;
	my $ec  = "ec number";

	# 1. Equivalog TIGRfams above trusted cutoff, HMM coverage = 100%
	if ( &get_ec_from_equivalog( $orf, \$ec ) ) {
		return ( $ec, "Equivalog TIGRFam" );

		# 2. PRIAM hits, e-values <= 1e-10
	}
	elsif ( &get_ec_from_priam( $orf, \$ec ) ) {
		return ( $ec, "PRIAM" );

		# 3. PFAM / non-equivalog TIGRfams above trusted cutoff, HMM-coverage = 100%.
	}
	elsif ( &get_ec_from_nonequivalog( $orf, \$ec ) ) {
		return ( $ec, "Non-Equivalog TIGRFam" );
	}
	else {
		return;
	}

}

sub get_ec_from_equivalog {

	# look for an ec numbr in tigrfams qith equivalog type isology
	my ( $orf, $ec ) = @_;

	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {
		foreach my $hit ( @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} } ) {
			if ( defined $hit->{'iso_type'} ) {
				if (    ( $hit->{'iso_type'} eq "equivalog" )
					 || ( $hit->{'iso_type'} eq "PFAM_equivalog" ) )
				{

					if (    ( $hit->{coverage} == 1 )
						 && ( $hit->{total_score} >= $hit->{trusted_cutoff} ) )
					{

						if ( $hit->{ec_num} ) {
							$$ec = $hit->{ec_num};
							return $ec;
						}

					}
				}
			}
			else {
				$TF->logLocal(
						  "PFAM/TIGRFAM_HMM $hit->{'hmm_acc'} does not have an associated iso_type",
						  0 );
			}
		}
	}
	return 0;

}

sub get_ec_from_priam {

	# look for an ec_number in PRIAM hits
	my ( $orf, $ec ) = @_;

	if ( exists $orf->{'PRIAM'}->{hits} ) {
		foreach my $hit ( @{ $orf->{'PRIAM'}->{hits} } ) {

			if ( $hit->{evalue} <= 1e-10 ) {

				if ( $hit->{ec_num} ) {
					$$ec = $hit->{ec_num};
					return $ec;
				}

			}

		}
	}
	return 0;

}

sub get_ec_from_nonequivalog {

	# Look for an ec number in any tigrfam non-equivalogs
	my ( $orf, $ec ) = @_;

	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {
		foreach my $hit ( @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} } ) {
			if ( defined $hit->{'iso_type'} ) {
				if (    ( $hit->{'iso_type'} eq "subfamily" )
					 || ( $hit->{'iso_type'} eq "superfamily" )
					 || ( $hit->{'iso_type'} eq "domain" ) )
				{

					if (    ( $hit->{coverage} == 1 )
						 && ( $hit->{total_score} >= $hit->{trusted_cutoff} ) )
					{

						if ( $hit->{ec_num} ) {
							$$ec = $hit->{ec_num};
							return $ec;
						}

					}
				}
			}
			else {

			}
		}
	}
	return 0;

}

sub apply_env_nt_assignments {

	# put together a little hash summarizing the env_nt hits
	my ($orf) = @_;
	if ( exists $orf->{'ENV_NT'}->{hits} ) {

		my $env_hash;

		foreach my $hit ( @{ $orf->{'ENV_NT'}->{hits} } ) {
			my $lib_name = &parse_env_lib_name( $hit->{subject_definition} );

			if ( $env_hash->{$lib_name}->{best_evalue} ) {
				$env_hash->{$lib_name}->{best_evalue}
					= &min( $env_hash->{$lib_name}->{best_evalue}, $hit->{evalue} );
			}
			else {
				$env_hash->{$lib_name}->{best_evalue} = $hit->{evalue};
			}
			$env_hash->{$lib_name}->{frequency}++;

		}

		return $env_hash;
	}
	return undef;
}

sub min {

	# return the smaller of two numbers

	my ( $num1, $num2 ) = @_;
	return ( $num1 < $num2 ) ? $num1 : $num2;

}

sub parse_env_lib_name {

	# parse the lib name from a given subject definition (expected to be frmo env_nt)
	# From Hernan:
	# Library names are part of the subject fasta headers and are located at the end
	# of the description separated by a "|" character. For example, for the header:
	#
	# gi|133673305|gb|AACY020037789.1| Marine metagenome 1096626033418, whole
	# genome shotgun sequence | Global Ocean Sampling Expedition Metagenome
	#
	# The library name would be:
	#
	# Global Ocean Sampling Expedition Metagenome

	my $whole_name = shift;

	my @name_bits = split( '\|', $whole_name );

	return $name_bits[-1];

}

sub make_pfam2go_hash {
	my $pfam2go = {};

	#Open pfam2go lookup file
	open( PFAM, "<$pfam2gotxt" ) or die "Can not open pfam2go file\n";

	my @pfam_file = <PFAM>;

	close PFAM;

	my $previous_pfam = '';
	my $multiple_go;

	foreach my $line (@pfam_file) {
		my $go_terms;

		if ( $line =~ /^!/ ) {
			next;
		}
		else {
			my $pfam    = $1 if $line =~ /(PF\d{5})/;
			my $go_term = $1 if $line =~ /(GO:\d{7})/;

			if ( $pfam ne $previous_pfam ) {

				if ( $previous_pfam ne '' ) {
					$pfam2go->{$previous_pfam} = $multiple_go;
				}

				$multiple_go = undef;

				push @$multiple_go, $go_term;

				$previous_pfam = $pfam;
			}
			else {
				push @$multiple_go, $go_term;
				$previous_pfam = $pfam;
			}
		}
	}

	$pfam2go->{$previous_pfam} = $multiple_go;

	return $pfam2go;
}

sub apply_go_term_assignment {

	# Given an orf hash, look at the various evidences involved in the
	# assignment of a GO-term to this orf, and go through a set of rules
	# to do so.

	my ( $orf, $pfam2go ) = @_;
	my %go_terms = ();
	my $go_source;

	# 1. GO/PhiGO information extracted from fasta headers from blastp top-hit
	# against ACLAME hmm hits
	&get_go_from_aclame_hmm( $orf, \%go_terms );

	# 2. All GO terms associated with PFAM/TIGRfam-HMMs above trusted cutoff and
	# full-length HMMs usign PFAM2GO tool and curated entries from
	# egad..hmm_go_link table info.  Remove redundant GO terms
	&get_go_from_tigrfam( $orf, \%go_terms, $pfam2go );

	return ( \%go_terms );

}

sub get_go_from_aclame_hmm {

	# Get the go ids from the ACLAME hmm hits.
	my ( $orf, $go_terms ) = @_;

	if ( exists $orf->{'ACLAME_HMM'}->{hits} ) {

		foreach my $hit ( @{ $orf->{'ACLAME_HMM'}->{hits} } ) {

			if (    ( $hit->{coverage} >= .9 )
				 && ( $hit->{total_evalue} <= 1e-5 ) )
			{

				if ( $hit->{hmm_description} =~ /((go|phi):\d{7})/ ) {

					my $parsed_go_terms = &parse_go_phi_go( $hit->{hmm_description} );

					if ($parsed_go_terms) {
						foreach my $go_term (@$parsed_go_terms) {
							$go_term = lc($go_term);

							if ( exists $go_terms->{$go_term} ) {
								my $source = $go_terms->{$go_term};
								$source .= "&$hit->{hmm_acc}";
							}
							else {
								$go_terms->{$go_term} = "$hit->{hmm_acc}";
							}

						}

						return ($go_terms);

					}
					else {
						return 0;
					}

				}
				else {

					#may have a hit but no GO/PHI term in the description
					return 0;
				}

			}
		}
	}

	return 0;

}

sub get_go_from_tigrfam {
	my ( $orf, $go_terms, $pfam2go ) = @_;

	if ( exists $orf->{'PFAM/TIGRFAM_HMM'}->{hits} ) {
		foreach my $hit ( @{ $orf->{'PFAM/TIGRFAM_HMM'}->{hits} } ) {

			if ( $hit->{coverage} == 1 ) {

				if ( $hit->{hmm_acc} ) {
					if ( $pfam2go->{ $hit->{hmm_acc} } ) {
						foreach my $go_term ( @{ $pfam2go->{ $hit->{hmm_acc} } } ) {

							$go_term = lc($go_term);

							if ( exists $go_terms->{$go_term} ) {
								my $source = $go_terms->{$go_term};
								$source .= "&$hit->{hmm_acc}";
							}
							else {
								$go_terms->{$go_term} = "$hit->{hmm_acc}";
							}
						}

						return ($go_terms);
					}
				}
			}
		}
	}

	return 0;
}

sub get_query_print_key_list {
	my ($job_name) = @_;

	my ( @query_key_list, @print_key_list );
	my ( $table,          $coverage );
	$coverage = 0;

	if ( $job_name eq 'PEPSTATS' ) {

		@query_key_list = qw( query_seq_id molecular_weight isoelectric_Point job_name);
		@print_key_list = qw( molecular_weight isoelectric_Point );
		$table          = "compute_pepstats";

	}
	elsif ( $job_name eq 'PRIAM' ) {

		@query_key_list = qw( query_seq_id ec_num evalue ec_definition job_name);
		@print_key_list = qw( ec_num evalue ec_definition );
		$table          = "compute_priam";

	}
	elsif ( $job_name eq 'TMHMM' ) {

		@query_key_list = qw( query_seq_id num_predicted_helixes job_name );
		@print_key_list = qw( num_predicted_helixes );
		$table          = "compute_tmhmm";

	}
	elsif ( $job_name eq 'SIGNALP' ) {

		@query_key_list = qw( query_seq_id hmm_sprob_flag hmm_cmax_pos job_name );
		@print_key_list = qw( hmm_sprob_flag hmm_cmax_pos );
		$table          = "compute_signalp";

	}
	elsif ( $job_name eq 'ACLAME_HMM' ) {

		@query_key_list
			= qw( query_seq_id hmm_begin hmm_end total_evalue hmm_acc hmm_description hmm_len job_name );
		@print_key_list
			= qw( hmm_begin hmm_end coverage total_evalue hmm_acc hmm_description hmm_len  );
		$table    = "compute_htab";
		$coverage = 'hmm';

	}
	elsif ( $job_name eq 'PRIAM_GENE_RPS' ) {

		@query_key_list = qw( rank query_id subject_definition evalue job_name);
		@print_key_list = qw( rank query_id subject_definition evalue );
		$table          = "compute_btab";

	}
	elsif ( $job_name eq 'SANGER_PEP' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw( subject_id subject_definition query_length
			subject_length coverage pct_identity evalue);
		$table    = "compute_btab";
		$coverage = 'blast';

	}
	elsif ( $job_name eq 'FRAG_HMM' ) {

		@query_key_list
			= qw(  query_seq_id hmm_begin hmm_end total_evalue hmm_acc hmm_description hmm_len job_name);
		@print_key_list
			= qw( hmm_begin hmm_end coverage total_evalue hmm_acc hmm_description hmm_len );
		$table    = "compute_htab";
		$coverage = 'hmm';

	}
	elsif ( $job_name eq 'ACLAME_PEP' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw(subject_id subject_definition query_length
			subject_length coverage pct_identity evalue);
		$table    = "compute_btab";
		$coverage = 'blast';

	}
	elsif ( $job_name eq 'PFAM/TIGRFAM_HMM' ) {

		@query_key_list = qw( query_seq_id hmm_begin hmm_end total_evalue
			hmm_acc hmm_description hmm_len job_name trusted_cutoff total_score );
		@print_key_list = qw( iso_type hmm_begin hmm_end coverage total_evalue
			hmm_acc hmm_description hmm_len trusted_cutoff total_score );
		$table    = "compute_htab";
		$coverage = 'hmm';

	}
	elsif ( $job_name eq 'CDD_RPS' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw( subject_definition coverage pct_identity evalue);
		$table          = "compute_btab";
		$coverage       = 'blast';

	}
	elsif ( $job_name eq 'ALLGROUP_PEP' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw( subject_id subject_definition query_length subject_length
			coverage pct_identity evalue);
		$table    = "compute_btab";
		$coverage = 'blast';

	}
	elsif ( $job_name eq 'ENV_NT' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw(subject_id subject_definition query_length subject_length
			coverage pct_identity evalue);
		$table    = "compute_btab";
		$coverage = 'blast';

	}
	elsif ( $job_name eq 'ENV_NR' ) {

		@query_key_list = qw( rank query_id alignment_length query_length pct_identity
			evalue job_name subject_length subject_id subject_definition
			job_name);
		@print_key_list = qw( subject_id subject_definition query_length subject_length
			coverage pct_identity evalue);
		$table    = "compute_btab";
		$coverage = 'blast';

	}

	return ( \@query_key_list, \@print_key_list, $table, $coverage );
}

sub select_best_hit {

	# Given an array of blast hit data, sort by coverage and return the best hit

	my ($evidence) = @_;

	# Sort.  We want the end results to be sorted by desending coverage, then by
	# descending pct_identity, then by ascending evalue.  We can accomplish it
	# this way since perl5 uses the stable mergesort algorithm:
	my @arr1 = @$evidence;
	if ( exists $evidence->[0]->{evalue} ) {
		@arr1 = sort { $a->{evalue} <=> $b->{evalue} } @{$evidence};
	}
	if ( exists $evidence->[0]->{pct_identity} ) {
		@arr1 = sort { $b->{pct_identity} <=> $a->{pct_identity} } @arr1;
	}
	if ( exists $evidence->[0]->{coverage} ) {
		@arr1 = sort { $b->{coverage} <=> $a->{coverage} } @arr1;
	}

	return \@arr1;

	# THIS SECTION NO LONGER NEEDED
	#	my @to_return;
	#
	#	# now select the top N hits...
	#	foreach my $hit (@arr1) {
	#		push @to_return, $hit;
	#
	#		#this line deemed necessary:
	#		#last if ( scalar @to_return == $tophit );
	#	}
	#
	#	return \@to_return;

}

sub parse_subject {

	# given a descriptive subject, parse the required information, according to the
	# type of job and information we expect to see therein.
	my $hit  = shift;
	my $name = '';

	if ( $hit->{job_name} eq 'ALLGROUP_PEP' ) {

		if ( $hit->{subject_definition} =~ /\S+\s(.*?) taxon/ ) {
			$name = $1;
		}
		else {
			$name = $hit->{subject_definition};
		}

	}
	elsif ( $hit->{job_name} eq 'ACLAME_HMM' ) {

		if ( $hit->{hmm_description} =~ /\|(.*?)\|/ ) {
			$name = $1;
		}
		else {
			$name = $hit->{hmm_description};
		}

	}
	elsif ( $hit->{job_name} eq 'CDD_RPS' ) {

		$name = $hit->{subject_definition};
		$name =~ s/$hit->{subject_id}, //g;

	}

	return $name;

}

sub is_descriptive {

	# given a reference to a hit, check its subject_id and subject_description
	# to see if they are the same, in which case return 0.
	# If they are different, there is something exciting in subject_description
	# so we'll return 1.

	my $row = shift;
	if ( $row->{subject_id} ) {
		return ( $row->{subject_id} eq $row->{subject_definition} ) ? 0 : 1;
	}

}

sub calculate_blast_coverage {

	# given the alignment length and query length, compute coverage as:
	#       alignment_length
	#       ---------------- = coverage
	#         query_length

	my ( $alignment_length, $query_length ) = @_;

	my $coverage = $alignment_length / $query_length;

	return sprintf( "%.2f", $coverage );

}

sub calculate_hmm_coverage {

	# given the beginning and end of the hmm match and the total length of
	# the hmm, compute coverage as:
	#   (hmm_end - hmm_begin + 1) / hmm_length
	my ( $hmm_beg, $hmm_end, $hmm_len ) = @_;

	my $coverage = ( $hmm_end - $hmm_beg + 1 ) / $hmm_len;

	return sprintf( "%.2f", $coverage );

}

#### Query Subs

sub get_query_ids {
	my ($dbh) = @_;

	my $query
		= "SELECT ds.seq_id, ds.seq_acc "
		. "FROM job j, dataset_seq ds "
		. "WHERE j.status = 'loaded' "
		. "AND j.is_obsolete = 0 "
		. "AND j.query_db_id = ds.version_id "
		. "group by ds.seq_acc";

	my $sth = $dbh->prepare($query) or die "$DBI::errstr\n";
	$sth->execute();

	return ( \$sth );
}

sub get_query_ids_env {
	my ($dbh) = @_;

	my $query
		= "SELECT seq_acc, min(seq_id) as primary_seq_id, case "
		. "when max(seq_id)<>min(seq_id) then max(seq_id) "
		. "else -99 end as secondary_seq_id "
		. "from dataset_seq "
		. "where version_id in(select query_db_id "
		. "from job where is_obsolete=0) "
		. "group by seq_acc";

	my $sth = $dbh->prepare($query) or die "$DBI::errstr\n";
	$sth->execute();

	return ( \$sth );
}

sub get_query_ids_for_job {
	my ( $dbh, $job_id ) = @_;

	my $query
		= "SELECT distinct ds.seq_acc "
		. "FROM job j, dataset_seq ds "
		. "WHERE j.status = 'loaded' "
		. "AND j.is_obsolete = 0 "
		. "AND j.query_db_id = ds.version_id "
		. "AND j.job_id = $job_id";

	my $sth = $dbh->prepare($query) or die "$DBI::errstr\n";
	$sth->execute();

	return $sth;
}

sub get_job_ids {
	my $dbh = shift;

	# Did substr because without it query returns the job
	# id as a scientific number which is not useful
	my $query
		= "SELECT job_name, substr(job_id,1,20) "
		. "FROM job where status = 'loaded' "
		. "AND is_obsolete = 0";

	my $sth = $dbh->prepare($query);

	$sth->execute();

	return $sth;
}

sub get_evidence_query {

	my ( $query_key_list, $query_id, $job_id, $table, $job_name ) = @_;

	my $query = "SELECT " . join( ",", @$query_key_list );
	if ( $job_name eq "PFAM/TIGRFAM_HMM" ) {
		$query .= ", (select iso_type from hmm2 h2 where h2.hmm_acc=c.hmm_acc) as iso_type";
		$query .= ", (select ec_num from hmm2 h2 where h2.hmm_acc=c.hmm_acc) as ec_num";
	}

	$query .= " FROM $table c WHERE job_id = $job_id and query_seq_id= $query_id";

	return $query;
}

#Used when env_nt datasets were split into four datasets
sub get_evidence_query_split {

	my ( $query_key_list, $query_seq_acc, $job_id, $table, $job_name, $query_primary_seq_id,
		 $query_secondary_seq_id )
		= @_;

	my $query = "SELECT " . join( ",", @$query_key_list );
	if ( $job_name eq "PFAM/TIGRFAM_HMM" ) {
		$query .= ", (select iso_type from hmm2 h2 where h2.hmm_acc=c.hmm_acc) as iso_type";
		$query .= ", (select ec_num from hmm2 h2 where h2.hmm_acc=c.hmm_acc) as ec_num";
	}

	$query
		.= " FROM $table c WHERE job_id = $job_id and query_seq_id in($query_primary_seq_id,$query_secondary_seq_id)";

	return $query;
}

sub parse_go_phi_go {

	# given a string, return the first GO/PhiGO-like string in the form:
	# go:1234567 || phi:1234567
	my $query_string = shift;

	my $go_ids = ();

	while ( $query_string =~ /((go|phi):\d{7})/g ) {

		#want to ignore phage function uknown
		#if($1 ne 'phi:0000326'){
		push @$go_ids, $1;

		#}
	}

	if ( defined $go_ids ) {
		return $go_ids;
	}
	else {
		return 0;
	}
}

sub print_evidence_results {

	my ( $orf, $ofh ) = @_;
	my ($orf_id) = keys %$orf;

	select $ofh;
	# print all the evidence for each job
	foreach my $job_name ( qw(CDD_RPS ALLGROUP_PEP ACLAME_PEP SANGER_PEP
						   ENV_NT ENV_NR FRAG_HMM PFAM/TIGRFAM_HMM PRIAM
						   ACLAME_HMM PRIAM_GENE_RPS PEPSTATS TMHMM SIGNALP)
		)
	{

		# for all jobs with at least 1 hit
		#unitiliazed value error
		if ( exists $orf->{$orf_id}->{$job_name}->{hits} ) {

			# for each element in the array of this job's hits
			foreach my $row ( @{ $orf->{$orf_id}->{$job_name}->{hits} } ) {
				print "$orf_id\t$job_name\t";

				# print the data for this hit
				foreach my $key ( @{ $orf->{$orf_id}->{$job_name}->{print_list} } ) {
					if ( defined $row->{$key} ) {
						print "$row->{$key}\t";
					}
					else {
						print "\t";
					}

				}

				print "\n";

			}
		}

	}

	print "//\n";

}

sub print_annotation_results {

	my ( $orf, $afh ) = @_;

	my ($orf_id) = keys %$orf;

	select $afh;

	print "$orf_id\t";
	print "common_name\t$orf->{$orf_id}->{ANNOTATION}->{name}\t";
	print "$orf->{$orf_id}->{ANNOTATION}->{name_ev}\t";

	print "GO\t";
	if ( defined $orf->{$orf_id}->{ANNOTATION}->{go_terms} ) {

		my @go_term     = ();
		my @go_evidence = ();

		foreach my $go_term ( keys %{ $orf->{$orf_id}->{ANNOTATION}->{go_terms} } ) {
			push( @go_term,     $go_term );
			push( @go_evidence, $orf->{$orf_id}->{ANNOTATION}->{go_terms}->{$go_term} );
		}

		print join( " || ", @go_term );
		print "\t";
		print join( " || ", @go_evidence );
		print "\t";

	}
	else {
		print "\t\t";
	}

	print "EC\t";
	if ( defined $orf->{$orf_id}->{ANNOTATION}->{ec_number_ev} ) {
		print "$orf->{$orf_id}->{ANNOTATION}->{ec_number}\t";
		print "$orf->{$orf_id}->{ANNOTATION}->{ec_number_ev}\t";
	}
	else {
		print "\t\t";
	}

	print "ENV_NT\t";
	if ( defined $orf->{$orf_id}->{ANNOTATION}->{env_nt} ) {

		my @env_libs;
		my @env_best_evalue;
		my @env_freqs;

		foreach my $lib ( keys %{ $orf->{$orf_id}->{ANNOTATION}->{env_nt} } ) {

			push( @env_libs, $lib );
			push( @env_best_evalue,
				  $orf->{$orf_id}->{ANNOTATION}->{env_nt}->{$lib}->{best_evalue} );
			push( @env_freqs, $orf->{$orf_id}->{ANNOTATION}->{env_nt}->{$lib}->{frequency} );

		}

		print join( " || ", @env_libs ),        "\t";
		print join( " || ", @env_best_evalue ), "\t";
		print join( " || ", @env_freqs ),       "\t";

	}
	else {
		print "\t\t\t";
	}

	print "PFAM/TIGRFAM_HMM\t";
	if ( defined $orf->{$orf_id}->{'PFAM/TIGRFAM_HMM'}->{all_hits} ) {
		print join( " || ", @{ $orf->{$orf_id}->{'PFAM/TIGRFAM_HMM'}->{all_hits} } );
		print "\t";
	}
	else {
		print "\t";
	}
	
	foreach my $job_name (qw(SIGNALP TMHMM PEPSTATS)) {

		# for all jobs with at least 1 hit
		if ( exists $orf->{$orf_id}->{$job_name}->{hits} ) {
				print "$job_name\t";

				# for each element in the array of this job's hits
				foreach my $row ( @{ $orf->{$orf_id}->{$job_name}->{hits} } ) {
					# print the data for this hit
					foreach my $key ( @{ $orf->{$orf_id}->{$job_name}->{print_list} } ) {
						print "$row->{$key}\t";
					}
				}
		}else{
			print "$job_name\t";
			
			foreach my $key ( @{ $orf->{$orf_id}->{$job_name}->{print_list} } ) {
				print "\t";
			}
			
		}
	}

	print "\n";

}

1;
