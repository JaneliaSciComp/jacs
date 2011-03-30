#!/usr/bin/env perl

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

$|++;
use lib ( $ENV{"PERL_MOD_DIR"} );
no lib ".";

=head1  NAME 

createBsmlFromBlastForJaccard.pl  - convert info stored in ncbi tab files into BSML documents

=head1 SYNOPSIS

USAGE:  blasttab2bsml.pl [-F tab_dir || -f tab_file] -o blastp_bsmls

=head1 OPTIONS

B<--output_dir,-o> [REQUIRED] output dir for BSML files

B<--tab_file,-f> [REQUIRED]* blast tabular format file

B<--tab_dir,-F> [REQUIRED]* Dir containing tabular format files.

B<--query_file_path,-q> [REQUIRED]Path to the fasta file of query sequences.

B<--max_hsp_count,-m> Maximum number of HSPs stored per alignment.

B<--evalue,-p> Provide a cutoff value above which alignments are ignored. [DEFAULT = 10]
    
B<--class,-c> The ref/comp sequence type.  Default is 'polypeptide.'

B<--gzip,-z> Write bsml output as gzip files.

B<--help,-h> Display this help message.

* Note that only one of -F or -f may be used.

=head1   DESCRIPTION

createBsmlFromBlastForJaccard.pl is designed to convert information in tab files into BSML documents.

NOTE:  

Calling the script name with NO flags/options or --help will display the syntax requirement.

=cut

use strict;
use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
use English;
use File::Basename;
use File::Path qw(make_path);
use Pod::Usage;
use Ergatis::Logger;
use BSML::BsmlRepository;
use BSML::BsmlBuilder;
use BSML::BsmlParserTwig;

use Data::Dumper;

my %options = ();
my $results = GetOptions( \%options,        'tab_dir|F=s',
						  'tab_file|f=s',   'query_file_path|q=s',
						  'output_dir|o=s', 'max_hsp_count|m=s',
						  'evalue|p=s',     'log|l=s',
						  'debug=s',        'class|c=s',
						  'gzip|z=s',       'help|h'
) || pod2usage();

my $logfile = $options{log} || Ergatis::Logger::get_default_logfilename();
my $logger = new Ergatis::Logger( 'LOG_FILE'  => $logfile,
								  'LOG_LEVEL' => $options{'debug'} );
$logger = $logger->get_logger();

if ( $options{help} ) {
	pod2usage( { verbose => 2 } );
}

if ( $options{evalue} eq "" ) {
	$options{evalue} = 10;
}

my $class = 'polypeptide';
if ( defined( $options{class} ) ) {
	$class = $options{class};
}

# display documentation
if ( $options{help} ) {
	pod2usage( { -exitval => 0, -verbose => 2, -output => \*STDOUT } );
}

my $output_dir = '';

my $deflines_with_hits = {};

&check_parameters( \%options );

my $files = &get_tab_files( $options{tab_dir}, $options{tab_file} );

my $seq_data_import_file = $options{query_file_path};

$seq_data_import_file =~ s/\.gz$|\.gzip$//;

my $deflines = get_deflines( $options{query_file_path} );

my $seq_lengths = &find_seq_lengths( $options{query_file_path} );

my $tab_file_path = $options{tab_file};
my $tab_file_name = ( fileparse($tab_file_path) )[0];

#File handle for bsml list
my $bfh;
open( $bfh, ">$output_dir/bsml.list" )
	|| $logger->logdie("can't open bsml list $output_dir/bsml.list: $!");

my $tfh;
open( $tfh, "<$tab_file_path" ) || $logger->logdie("can't open tab file $tab_file_path: $!");

my $query_id = 0;
my @hits;
my $previous_query = '';
my $seq_id;

while (<$tfh>) {
	my @row = split( /\t/, $_ );
	$seq_id = $row[0];
	$deflines_with_hits->{$seq_id} = 1;

	$previous_query = $seq_id if ( $previous_query eq '' );

	if ( $seq_id eq $previous_query ) {
		push( @hits, $_ );
	}
	else {
		&creatBsmlDoc( $previous_query, \@hits );

		@hits = '';
		push( @hits, $_ );
		$previous_query = $seq_id;
	}
}
&creatBsmlDoc( $seq_id, \@hits );

#Creates bsml files for deflines with no blast hits
foreach my $query ( keys %$deflines ) {
	unless ( defined $deflines_with_hits->{$query} ) {
		&creatBsmlDoc( $query );
	}
}

exit(0);

# Subs follow: #
sub creatBsmlDoc {
	my ( $seq_id, $hits ) = @_;

	#generate lookups
	my $doc;

	$doc = new BSML::BsmlBuilder();
	$doc->makeCurrentDocument();

	parse_blast_tabs( $hits, $doc ) if ($hits);

	## create sequence stubs for input sequences with null output
	if ( !( $doc->returnBsmlSequenceByIDR($seq_id) ) ) {
		my $seq = $doc->createAndAddSequence( $seq_id,
											  $seq_id,
											  '',        # length
											  '',        # molecule
											  '',        # class
		);

		$seq->addBsmlLink( 'analysis', '#' . $options{analysis_id}, 'input_of' );
		$doc->createAndAddBsmlAttribute( $seq, 'defline', $deflines->{$seq_id} );
		$doc->createAndAddSeqDataImport( $seq, 'fasta', $seq_data_import_file, '', $seq_id );
	}

	( my $name = $seq_id ) =~ s/\|/_/g;

	&addAnalysis( $doc, $name );
	&writeDoc( $doc, "$output_dir/$name.bsml" );
	print $bfh "$output_dir/$name.bsml\n";
}

sub addAnalysis {
	## add the analysis element

	my ( $doc, $name ) = @_;
	my $program = $options{analysis_id};
	$program =~ s/_analysis//;
	$doc->createAndAddAnalysis( id         => $options{analysis_id},
								sourcename => $name,
								algorithm  => $program,
								program    => $program,
	);

}

sub writeDoc {

	my ( $doc, $name ) = @_;
	if ( $options{'gzip'} ) {
		$doc->write( $name, undef, 1 );
	}
	else {
		$doc->write($name);
	}

}

sub parse_blast_tabs {

	my ( $hits, $doc ) = @_;

	my $btab;
	my $hsplookup;

	foreach my $line (@$hits) {

		chomp($line);

		my @tab = split( "\t", $line );

		if ( ( $tab[10] <= $options{'evalue'} ) && ( $tab[0] ne "" ) && ( $tab[1] ne "" ) ) {
			if ( !( exists $hsplookup->{ $tab[0] }->{ $tab[1] } ) ) {
				$hsplookup->{ $tab[0] }->{ $tab[1] } = [];
			}
			push @{ $hsplookup->{ $tab[0] }->{ $tab[1] } },
				{ 'evalue' => $tab[10],
				  'line'   => $line
				};
		}
		else {
			$logger->debug(
				"Skipping $tab[0] $tab[5] evalue $tab[10] above evalue cutoff of $options{'evalue'}"
			) if ( $logger->is_debug() );
		}
	}

	## This block prepares an array of HSP data structured so that we can use
	## existing subs taken from Sybil::Util to calculate coverage/identity info
	my $cov_qual_stats = {};
	my @hsp_ref_array  = ();
	my $new_subject    = '';

	my $i = 0;

LOOP: foreach my $query ( keys %$hsplookup ) {

		last LOOP if ( $i > 5 );

		foreach my $subject ( keys %{ $hsplookup->{$query} } ) {
			my @hsps
				= sort { $a->{'evalue'} <=> $b->{'evalue'} } @{ $hsplookup->{$query}->{$subject} };
			my $maxhsp;
			if ( $options{'max_hsp_count'} ne "" ) {
				$maxhsp
					= ( $options{'max_hsp_count'} < scalar(@hsps) )
					? $options{'max_hsp_count'}
					: scalar(@hsps);
			}
			else {
				$maxhsp = scalar(@hsps);
			}
			my $queryid;
			for ( my $i = 0; $i < $maxhsp; $i++ ) {
				my $line = $hsps[$i]->{'line'};
				my @tab = split( "\t", $line );
				$queryid = $tab[0] if ( $tab[0] && ( !$queryid ) );
				$queryid =~ s/(\w+).*/$1/;

				for ( my $i = 0; $i < scalar(@tab); $i++ ) {
					if ( $tab[$i] eq 'N/A' ) {
						$tab[$i] = undef;
					}
				}

				my $orig_dbmatch_accession = $tab[1];
				$orig_dbmatch_accession =~ s/(\w+).*/$1/;
				$tab[1] =~ s/[^a-z0-9\_\.\-]/_/gi;

				$new_subject = $tab[1];

				my $qfmin   = $tab[6];
				my $qfmax   = $tab[7];
				my $qstrand = 0;
				my $tfmin   = $tab[8];
				my $tfmax   = $tab[9];
				my $tstrand = 0;

				## if query positions are on the reverse strand
				if ( $tab[6] > $tab[7] ) {
					$qfmin   = $tab[7];
					$qfmax   = $tab[$6];
					$qstrand = 1;
				}

				## if target positions are on the reverse strand
				if ( $tab[8] > $tab[9] ) {
					$tfmin   = $tab[9];
					$tfmax   = $tab[8];
					$tstrand = 1;
				}

				## transform the start positions to interbase
				$qfmin = $qfmin - 1;
				$tfmin = $tfmin - 1;

				my $hsp_ref = {
					'query_protein_id'  => $tab[0],
					'target_protein_id' => $tab[1],
					'significance'      => $tab[10],
					'percent_identity'  => $tab[2],
					'query_seqlen'      => $seq_lengths->{$queryid},
					'target_seqlen'     => $seq_lengths->{$orig_dbmatch_accession},
					'query_fmin'        => $qfmin,
					'query_fmax'        => $qfmax,
					'query_strand'      => "",
					'target_fmin'       => $tfmin,
					'target_fmax'       => $tfmax,
					'target_strand' => $tstrand,    ## target strand is not captured in tab
				};
				push( @hsp_ref_array, $hsp_ref );
			}
			if ( !defined( $cov_qual_stats->{$query} ) ) {
				$cov_qual_stats->{$query} = {};
			}

			my $coverage_arr_ref = &getAvgBlastPPctCoverage( \@hsp_ref_array );

			$cov_qual_stats->{$query}->{$new_subject} = {
							'percent_coverage_refseq'  => sprintf( "%.1f", $coverage_arr_ref->[0] ),
							'percent_coverage_compseq' => sprintf( "%.1f", $coverage_arr_ref->[1] ),
			};
		}

		$i = 0;

		foreach my $query ( keys %$hsplookup ) {

			foreach my $subject ( keys %{ $hsplookup->{$query} } ) {
				my @hsps = sort { $a->{'evalue'} <=> $b->{'evalue'} }
					@{ $hsplookup->{$query}->{$subject} };
				my $maxhsp;
				if ( $options{'max_hsp_count'} ne "" ) {
					$maxhsp
						= ( $options{'max_hsp_count'} < scalar(@hsps) )
						? $options{'max_hsp_count'}
						: scalar(@hsps);
				}
				else {
					$maxhsp = scalar(@hsps);
				}
				my $queryid;
				for ( my $i = 0; $i < $maxhsp; $i++ ) {
					my $line = $hsps[$i]->{'line'};
					my @tab = split( "\t", $line );
					$logger->debug("Storing HSP $tab[0] $tab[1] $tab[10]")
						if ( $logger->is_debug() );

					$queryid = $tab[0] if ( $tab[0] && ( !$queryid ) );

					for ( my $i = 0; $i < scalar(@tab); $i++ ) {
						if ( $tab[$i] eq 'N/A' ) {
							$tab[$i] = undef;
						}
					}

					## dbmatch_accession needs to be alphanumeric or _-.
					##  but the original needs to be passed to createAndAddBtabLine so it can
					##  be recognized and parsed
					my $orig_dbmatch_accession = $tab[1];
					$tab[1] =~ s/[^a-z0-9\_\.\-]/_/gi;
				
					#$tab[0] =~ s/(\w+).*/$1/;
									
					my $align =
						&createAndAddBtabLine(
						doc               => $doc,
						query_name        => $tab[0], 
						date              => "unknown date",
						query_length      => $seq_lengths->{ $tab[0] },
						blast_program     => "unknown blast",
						search_database   => $options{query_file_path},
						dbmatch_accession => $tab[1],
						start_query       => $tab[6],
						stop_query        => $tab[7],
						start_hit         => $tab[8],
						stop_hit          => $tab[9],
						percent_identity  => $tab[2],
						bit_score         => $tab[11],
						hit_length        => $tab[3],
						e_value           => $tab[10],
						p_value           => $tab[10],
						percent_coverage_refseq =>
							$cov_qual_stats->{ $tab[0] }->{ $tab[1] }->{'percent_coverage_refseq'},
						percent_coverage_compseq =>
							$cov_qual_stats->{ $tab[0] }->{ $tab[1] }->{'percent_coverage_compseq'},
						class                  => $class,
						orig_dbmatch_accession => $orig_dbmatch_accession
						);

					my $seq = $doc->returnBsmlSequenceByIDR( $tab[1] );
				}
				if ($queryid) {
					my $seq = $doc->returnBsmlSequenceByIDR($queryid);
				}
			}
		}
	}
}

sub get_tab_files {
	my ( $directory, $file ) = @_;
	my @files;
	if ( -d $directory ) {
		opendir( DIR, $directory ) or die "Unable to access $directory due to $!";
		while ( my $filename = readdir(DIR) ) {
			if ( $filename =~ /(.+)\.tab$/ ) {
				push( @files, "$directory/$filename" );
			}
		}
	}
	if ( defined($file) && $file ne "" ) {
		push @files, $file;
	}
	return \@files;
}

sub check_parameters {

	my ($options) = @_;

	my $errors = '';

	unless ( $options{tab_dir} || $options{tab_file} ) {
		$errors .= "Must use either --tab_dir (-F) or --tab_file (-f)\n";
	}

	if ( $options{tab_dir} && $options{tab_file} ) {
		$errors .= "Please provide only one or the other of --tab_dir (-F) and tab_file (-f)\n";
	}

	if ( ( !-d $options{'tab_dir'} ) && ( !-e $options{'tab_file'} ) ) {
		$errors .= "Couldn't find input dir or file\n";
	}

	unless ( $options{query_file_path} ) {
		$errors .= "Please provide a --query_file_path. (-q)\n";
	}

	## handle some defaults
	if ( !$options{analysis_id} ) {
		$options{analysis_id} = 'unknown_analysis';
	}

	$output_dir = ( $options{output_dir} ) ? $options{output_dir} : '.';
	if ( -d $output_dir ) {

		unless ( -w $output_dir ) {
			$errors .= "Output directory $output_dir exists but is not writeable\n";
		}

	}
	else {
		$errors .= "Could not create outptu_dir $output_dir\n"
			unless ( make_path($output_dir) );
	}

	$logger->logdie("$errors") if $errors;

}

sub createAndAddBtabLine {

	my %args = @_;

	my $doc = $args{'doc'};

	#    my $orig_dbmatch_accession = $args{dbmatch_accession};
	#    $args{dbmatch_accession} =~ s/[^a-z0-9\_]/_/gi;

	#determine if the query name and the dbmatch name are a unique pair in the document
	my $alignment_pair_list = BSML::BsmlDoc::BsmlReturnAlignmentLookup( $args{'query_name'},
																	   $args{'dbmatch_accession'} );

	my $alignment_pair = '';
	if ($alignment_pair_list) {
		$alignment_pair = $alignment_pair_list->[0];
	}

	if ($alignment_pair) {

		#add a new BsmlSeqPairRun to the alignment pair and return
		my $seq_run
			= $alignment_pair->returnBsmlSeqPairRunR( $alignment_pair->addBsmlSeqPairRun() );

		if ( $args{'start_query'} > $args{'stop_query'} ) {
			$seq_run->setattr( 'refpos',        $args{'stop_query'} - 1 );
			$seq_run->setattr( 'runlength',     $args{'start_query'} - $args{'stop_query'} + 1 );
			$seq_run->setattr( 'refcomplement', 1 );
		}
		else {
			$seq_run->setattr( 'refpos',        $args{'start_query'} - 1 );
			$seq_run->setattr( 'runlength',     $args{'stop_query'} - $args{'start_query'} + 1 );
			$seq_run->setattr( 'refcomplement', 0 );
		}

		#the database sequence is always 5' to 3'
		$seq_run->setattr( 'comppos', $args{'start_hit'} - 1 )
			if ( defined( $args{'start_hit'} ) );
		$seq_run->setattr( 'comprunlength', $args{'stop_hit'} - $args{'start_hit'} + 1 )
			if ( ( defined( $args{'start_hit'} ) ) and ( defined( $args{'stop_hit'} ) ) );
		$seq_run->setattr( 'compcomplement', 0 );
		$seq_run->setattr( 'runscore',       $args{'bit_score'} )
			if ( defined( $args{'bit_score'} ) );
		$seq_run->setattr( 'runprob', $args{'e_value'} ) if ( defined( $args{'e_value'} ) );
		$seq_run->addBsmlAttr( 'class',            'match_part' );
		$seq_run->addBsmlAttr( 'percent_identity', $args{'percent_identity'} )
			if ( defined( $args{'percent_identity'} ) );
		$seq_run->addBsmlAttr( 'percent_similarity', $args{'percent_similarity'} )
			if ( defined( $args{'percent_similarity'} ) );
		$seq_run->addBsmlAttr( 'percent_coverage_refseq', $args{'percent_coverage_refseq'} )
			if ( defined( $args{'percent_coverage_refseq'} ) );
		$seq_run->addBsmlAttr( 'percent_coverage_compseq', $args{'percent_coverage_compseq'} )
			if ( defined( $args{'percent_coverage_compseq'} ) );
		$seq_run->addBsmlAttr( 'p_value', $args{'p_value'} ) if ( defined( $args{'p_value'} ) );
		$seq_run->addBsmlAttr( 'e_value', $args{'e_value'} ) if ( defined( $args{'e_value'} ) );

		return $alignment_pair;
	}

	#no alignment pair matches, add a new alignment pair and sequence run
	#check to see if sequences exist in the BsmlDoc, if not add them with basic attributes
	my $seq;

	if ( !( $doc->returnBsmlSequenceByIDR( $args{query_name} ) ) ) {
		$seq = $doc->createAndAddSequence( "$args{'query_name'}", "$args{'query_name'}",
										   $args{'query_length'}, '', $args{'class'} );
			   
		$seq->addBsmlLink( 'analysis', '#' . $options{analysis_id}, 'input_of' );

		my $defline = $deflines->{ $args{'query_name'} };
		$doc->createAndAddBsmlAttribute( $seq, 'defline', $defline );

		if ( $options{'query_file_path'} ) {
			$doc->createAndAddSeqDataImport( $seq, 'fasta', $seq_data_import_file, '',
											 "$args{'query_name'}" );
		}

	}

	if ( !( $doc->returnBsmlSequenceByIDR("$args{'dbmatch_accession'}") ) ) {
		$seq =
			$doc->createAndAddSequence( "$args{'dbmatch_accession'}", "$args{'dbmatch_header'}",
										( $args{'hit_length'} || 0 ),
										'', $args{'class'} );
		$doc->createAndAddBsmlAttribute( $seq, 'defline',
										 "$args{orig_dbmatch_accession} $args{dbmatch_header}" );

		## see if the dbmatch_header format is recognized.  if so, add some cross-references
		if ( defined $args{'dbmatch_header'} ) {
			$doc->createAndAddCrossReferencesByParse( sequence => $seq,
													  string   => $args{orig_dbmatch_accession} );
		}
	}

	$alignment_pair = $doc->returnBsmlSeqPairAlignmentR( $doc->addBsmlSeqPairAlignment() );

	## to the alignment pair, add a Link to the analysis
	$alignment_pair->addBsmlLink( 'analysis', '#' . $options{analysis_id}, 'computed_by' );

	$alignment_pair->setattr( 'refseq', "$args{'query_name'}" )
		if ( defined( $args{'query_name'} ) );
	$alignment_pair->setattr( 'compseq', "$args{'dbmatch_accession'}" )
		if ( defined( $args{'dbmatch_accession'} ) );

	BSML::BsmlDoc::BsmlSetAlignmentLookup( "$args{'query_name'}", "$args{'dbmatch_accession'}",
										   $alignment_pair );

	$alignment_pair->setattr( 'refxref', ':' . $args{'query_name'} )
		if ( defined( $args{'query_name'} ) );
	$alignment_pair->setattr( 'refstart', 0 );
	$alignment_pair->setattr( 'refend',   $args{'query_length'} )
		if ( defined( $args{'query_length'} ) );
	$alignment_pair->setattr( 'reflength', $args{'query_length'} )
		if ( defined( $args{'query_length'} ) );
	$alignment_pair->setattr( 'method', $args{'blast_program'} )
		if ( defined( $args{'blast_program'} ) );
	$alignment_pair->setattr( 'class', 'match' );
	$alignment_pair->setattr( 'compxref',
							  $args{'search_database'} . ':' . $args{'dbmatch_accession'} )
		if (     ( defined( $args{'search_database'} ) )
			 and ( defined( $args{'dbmatch_accession'} ) ) );

	my $seq_run = $alignment_pair->returnBsmlSeqPairRunR( $alignment_pair->addBsmlSeqPairRun() );

	if ( $args{'start_query'} > $args{'stop_query'} ) {
		$seq_run->setattr( 'refpos',        $args{'stop_query'} - 1 );
		$seq_run->setattr( 'runlength',     $args{'start_query'} - $args{'stop_query'} + 1 );
		$seq_run->setattr( 'refcomplement', 1 );
	}
	else {
		$seq_run->setattr( 'refpos',        $args{'start_query'} - 1 );
		$seq_run->setattr( 'runlength',     $args{'stop_query'} - $args{'start_query'} + 1 );
		$seq_run->setattr( 'refcomplement', 0 );
	}

	#the database sequence is always 5' to 3'
	$seq_run->setattr( 'comppos', $args{'start_hit'} - 1 ) if ( defined( $args{'start_hit'} ) );
	$seq_run->setattr( 'comprunlength', ( $args{'stop_hit'} - $args{'start_hit'} + 1 ) )
		if ( ( defined( $args{'start_hit'} ) ) and ( defined( $args{'stop_hit'} ) ) );
	$seq_run->setattr( 'compcomplement', 0 );
	$seq_run->setattr( 'runscore',       $args{'bit_score'} ) if ( defined( $args{'bit_score'} ) );
	$seq_run->setattr( 'runprob',        $args{'e_value'} ) if ( defined( $args{'e_value'} ) );
	$seq_run->addBsmlAttr( 'class',            'match_part' );
	$seq_run->addBsmlAttr( 'percent_identity', $args{'percent_identity'} )
		if ( defined( $args{'percent_identity'} ) );
	$seq_run->addBsmlAttr( 'percent_similarity', $args{'percent_similarity'} )
		if ( defined( $args{'percent_similarity'} ) );
	$seq_run->addBsmlAttr( 'percent_coverage_refseq', $args{'percent_coverage_refseq'} )
		if ( defined( $args{'percent_coverage_refseq'} ) );
	$seq_run->addBsmlAttr( 'percent_coverage_compseq', $args{'percent_coverage_compseq'} )
		if ( defined( $args{'percent_coverage_compseq'} ) );
	$seq_run->addBsmlAttr( 'chain_number', $args{'chain_number'} )
		if ( defined( $args{'chain_number'} ) );
	$seq_run->addBsmlAttr( 'segment_number', $args{'segment_number'} )
		if ( defined( $args{'segment_number'} ) );
	$seq_run->addBsmlAttr( 'p_value', $args{'p_value'} ) if ( defined( $args{'p_value'} ) );

	return $alignment_pair;
}

## Returns an array reference where
##     [0] = query percent coverage,
## and [1] = target percent coverage
sub getAvgBlastPPctCoverage {
	my ($hsps)  = @_;
	my $qsum    = 0;
	my $tsum    = 0;
	my $numHsps = 0;

	# Group by query and target id
	my $hspsByQuery = &groupByMulti( $hsps, [ 'query_protein_id', 'target_protein_id' ] );

	foreach my $queryId ( keys %$hspsByQuery ) {
		my $hspsByTarget = $hspsByQuery->{$queryId};

		foreach my $subjId ( keys %$hspsByTarget ) {
			++$numHsps;
			my $shsps        = $hspsByTarget->{$subjId};
			my $querySeqLen  = $shsps->[0]->{'query_seqlen'};
			my $targetSeqLen = $shsps->[0]->{'target_seqlen'};

			my @queryIntervals = map {
				{  'fmin'   => $_->{'query_fmin'},
				   'fmax'   => $_->{'query_fmax'},
				   'strand' => $_->{'query_strand'}
				}
			} @$shsps;
			my @targetIntervals = map {
				{  'fmin'   => $_->{'target_fmin'},
				   'fmax'   => $_->{'target_fmax'},
				   'strand' => $_->{'target_strand'}
				}
			} @$shsps;

			my $mergedQueryIntervals  = &mergeOverlappingIntervals( \@queryIntervals );
			my $mergedTargetIntervals = &mergeOverlappingIntervals( \@targetIntervals );

			my $queryHitLen  = 0;
			my $targetHitLen = 0;

			map { $queryHitLen  += ( $_->{'fmax'} - $_->{'fmin'} ); } @$mergedQueryIntervals;
			map { $targetHitLen += ( $_->{'fmax'} - $_->{'fmin'} ); } @$mergedTargetIntervals;

			$qsum += $queryHitLen / $querySeqLen;
			$tsum += $targetHitLen / $targetSeqLen;
		}
	}

	if ( $numHsps == 0 ) {
		return undef;
	}
	else {
		return [ ( $qsum / $numHsps * 100.0 ), ( $tsum / $numHsps * 100.0 ) ];
	}

	#return ($numHsps > 0) ? ($sum/($numHsps * 2) * 100.0) : undef;
}

# Generalized version of groupBy
sub groupByMulti {
	my ( $arrayref, $keyFields ) = @_;
	my $nKeys  = scalar(@$keyFields);
	my $groups = {};

	foreach my $a (@$arrayref) {
		my @keyValues = map { $a->{$_} } @$keyFields;
		my $hash = $groups;

		for ( my $i = 0; $i < $nKeys; ++$i ) {
			my $kv = $keyValues[$i];

			if ( $i < ( $nKeys - 1 ) ) {
				$hash->{$kv} = {} if ( !defined( $hash->{$kv} ) );
				$hash = $hash->{$kv};
			}
			else {
				$hash->{$kv} = [] if ( !defined( $hash->{$kv} ) );
				push( @{ $hash->{$kv} }, $a );
			}
		}
	}
	return $groups;
}

# Generate a new set of intervals by merging any that overlap in the original set.
#
sub mergeOverlappingIntervals {
	my ($intervals) = @_;

	# result set of intervals
	my $merged = [];

	# sort all intervals by fmin
	my @sorted = sort { $a->{'fmin'} <=> $b->{'fmin'} } @$intervals;

	# current interval
	my $current = undef;

	foreach my $i (@sorted) {
		if ( !defined($current) ) {

			# case 1: no current interval
			$current = $i;
		}
		else {

			# case 2: compare current interval to interval $i
			if ( $i->{'fmin'} > $current->{'fmax'} ) {

				# case 2a: no overlap
				push( @$merged, $current );
				$current = $i;
			}
			elsif ( $i->{'fmax'} > $current->{'fmax'} ) {

				# case 2b: overlap, with $i ending to the right of $current
				$current->{'fmax'} = $i->{'fmax'};
			}
		}
	}
	push( @$merged, $current ) if ( defined($current) );

	return $merged;
}

sub find_seq_lengths {

	my $fasta_file     = shift;
	my $seq_length_hsh = {};

	my @seq_lengths = split( "\n", `residues $fasta_file` );

	foreach my $seq (@seq_lengths) {
		my ( $seq_name, $seq_len ) = split( /\s/, $seq );
		$seq_name =~ s/(\w+).*/$1/;
		$seq_length_hsh->{$seq_name} = $seq_len;
	}

	return $seq_length_hsh;
}

## retrieve deflines from a fasta file
sub get_deflines {

	my ($fasta_file) = @_;
	my $deflines = {};
	my $ifh;

	if ( !-e $fasta_file ) {
		if ( -e $fasta_file . ".gz" ) {
			$fasta_file .= ".gz";
		}
		elsif ( -e $fasta_file . ".gzip" ) {
			$fasta_file .= ".gzip";
		}
	}

	if ( $fasta_file =~ /\.(gz|gzip)$/ ) {
		open( $ifh, "<:gzip", $fasta_file )
			|| $logger->logdie("can't open input file '$fasta_file': $!");
	}
	else {
		open( $ifh, $fasta_file )
			|| $logger->logdie("Failed opening '$fasta_file' for reading: $!");
	}

	while (<$ifh>) {
		unless (/^>/) {
			next;
		}
		chomp;
		if (/^>((\S+).*)$/) {
			$deflines->{$2} = $1;
		}
	}
	close $ifh;

	if ( scalar( keys( %{$deflines} ) ) < 1 ) {
		$logger->warn("defline lookup failed for '$fasta_file'");
	}

	return $deflines;
}

sub split_tab_file {

	my $tab_file_path = shift;
	my $tab_file_name = ( fileparse($tab_file_path) )[0];
	my $temp_path     = "/usr/local/scratch/createBsmlFromBlastForJaccard/$$/$tab_file_name";
	make_path($temp_path);

	my $tfh;
	open( $tfh, "<$tab_file_path" )
		|| $logger->logdie("can't open tab file $tab_file_path: $!");

	my $query_id = 0;
	my $seq_id   = '';
	my $lines    = '';

	my @names;

	while (<$tfh>) {

		if (/BLASTP/) {
			if ($query_id) {
				push( @names, &create_small_tab_file( $seq_id, $query_id, $temp_path, $lines ) );
			}
			$query_id = 0;
			$lines    = '';
			next;
		}

		if (/^# Query: (\S+)/) {
			$seq_id = $1;
			( $query_id = $1 ) =~ s/\|/_/;
			next;
		}

		$lines .= $_ unless (/^#/);

	}

	push( @names, &create_small_tab_file( $seq_id, $query_id, $temp_path, $lines ) )
		;    # get last one out, too.

	return (@names);

}

sub create_small_tab_file {

	my ( $seq_id, $query_id, $temp_path, $lines ) = @_;

	my $tab_obj = ();
	my $stfh;

	open( $stfh, ">$temp_path/$query_id.tab" )
		|| $logger->logdie("can't write to $temp_path/$query_id.tab: $_");

	print $stfh $lines;

	$tab_obj->{tab_path} = "$temp_path/$query_id.tab";

	$tab_obj->{seq_id} = $seq_id;

	return $tab_obj;

}
