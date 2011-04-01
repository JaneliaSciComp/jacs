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

$| = 1;
=head1 NAME

    get_annotation.pl - Reads evidence from sqlite3 database and assigns annotation

=head1 SYNOPSIS

USAGE: get_annotation.pl
    --database=/database/location/sqlite.db
    --output=/output/directory/
  [ --gzip ]

=head1 OPTIONS

B<--database,-d>
    REQUIRED. Full location to sqlite3 database

B<--output,-o>
    REQUIRED. The directory you would like your output files written to.

B<--gzip,-g>
    OPTIONAL.  Setting gzip flag will write to compressed files. By default this is set to 0.

B<--help,-h>
    Print this message

=head1  DESCRIPTION

    This scripts reads in a sqlite3 database file which contains various evidence searches
    for a viral library. The script then assigns annotation based on the search results.

=head1 OUTPUT

    prefix.annotation[.gz] - File contains assigned annotation for the ORFs found in the sqlite3 db.
    prefix.evidence[.gz] - File contains all the evidence for each ORF as queried out from the sqlite3 db.
    prefix.tab[.gz] - File contains all assigned annotation for the ORFs in METAREP format
    prefex.annotation_log - Shows progress of what ORF is being annotated as the script is run.


=head1  CONTACT

    Jason Inman
	jinman@jcvi.org
	
	Erin Beck
	ebeck@jcvi.org
=cut

use warnings;
use strict;
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');

use EAP::db;
use EAP::parse;
use Config::IniFiles;
use Getopt::Long;
use Data::Dumper;
use TIGR::Foundation;
use Pod::Usage;

use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
my $pgmdir = dirname($program);
our $pfam2gotxt = $pgmdir . "/data/pfam2go.txt";
our $TF = new TIGR::Foundation;

my %opts = ();  # Will store command line options
my $conf;       # Will store the configuration, initially filled via config_file
my ( $database, $coverage, $percid, $evalue, $output, $db_dir, $gzip, $env_nt_split);
my $job_id;

GetOptions( \%opts, 'database|d=s', 'output|o=s', 'help|h', 'gzip|g', 'env_nt_split' ) || &_pod;;

# Check the options and read in the config
&check_options;

#Generate pfam2go lookup hash for later use
my $pfam2go_hsh = &make_pfam2go_hash;

my @file_prefix = split(/\//,$database);
my $prefix = $file_prefix[-1];

if (!$prefix) {die "Can not find a .db file in database path"};

$TF->setLogFile("$output/$prefix.annotation_log");
$TF->setDebugLevel(0);

$TF->logLocal("Connecting to database",0);

my $dbh = connectSQLite($database) || die "Error connecting.\n";

my $job_sth = &get_job_ids($dbh);
my $job_ids = $job_sth->fetchall_arrayref();

#get all valid/current query ids
$TF->logLocal("Getting all query ids",0);

my $query_ids_sth;

if($env_nt_split){
	$query_ids_sth = &get_query_ids_env($dbh);
}else{
	$query_ids_sth = &get_query_ids($dbh);
}

$TF->logLocal("Finished getting all query ids",0);

my $query_ids;

my $ofh = open_filehandle("$output/$prefix.evidence",$gzip,"OUT");
print $ofh scalar(localtime),"\n";

my $afh = open_filehandle("$output/$prefix.annotation",$gzip,"OUT");
print $afh scalar(localtime),"\n";

my $mfh = open_filehandle("$output/$prefix.tab",$gzip,"OUT");

print $ofh <<"EOF";
CDD_RPS\tquery_id\tsubject_definition\tcoverage\tpct_identity\tevalue
ALLGROUP_PEP\tquery_id\tsubject_id\tsubject_definition\tquery_length\tsubject_length\tcoverage\tpct_identity\tevalue
ACLAME_PEP\tquery_id\tsubject_id\tsubject_definition\tquery_length\tsubject_length\tcoverage\tpct_identity\tevalue
SANGER_PEP\tquery_id\tsubject_id\tsubject_definition\tquery_length\tsubject_length\tcoverage\tpct_identity\tevalue
ENV_NT\tquery_id\tsubject_id\tsubject_definition\tquery_length\tsubject_length\tcoverage\tpct_identity\tevalue
ENV_NR\tquery_id\tsubject_id\tsubject_definition\tquery_length\tsubject_length\tcoverage\tpct_identity\tevalue
FRAG_HMM\tquery_seq_id\thmm_begin\thmm_end\tcoverage\ttotal_evalue\thmm_acc\thmm_description\thmm_len
PFAM/TIGRFAM_HMM\tquery_seq_id\thmm_begin\thmm_end\tcoverage\ttotal_evalue\thmm_acc\thmm_description\thmm_len
PRIAM\tec_num\tevalue
ACLAME_HMM\tquery_seq_id\thmm_begin\thmm_end\tcoverage\ttotal_evalue\thmm_acc\thmm_description\thmm_len
PRIAM_GENE_RPS\trank\tquery_id\tsubject_definition\tevalue
PEPSTATS\tmolecular_weight\tisoelectric_Point
TMHMM\tnum_predicted_helixes
SIGNALP\tquery_seq_id\thmm_sprob_flag\thmm_cmax_pos
IPRSCAN\tquery_id\tdb_member_id\tdb_member_desc\tinterpro_desc\tinterpro_id
//
EOF

#foreach query id get the associated job hits and find best hit
while ( $query_ids = $$query_ids_sth->fetchrow_arrayref() ) {
	my $orf          = {};
	my ($query_seq_id, $query_id, $query_seq_acc, $query_primary_seq_id,$query_secondary_seq_id) =('','','','','');
		

	if($env_nt_split){
		$query_seq_acc= @$query_ids[0];
		$query_primary_seq_id = @$query_ids[1];
		$query_secondary_seq_id = @$query_ids[2];
	
	}else{
		$query_seq_id = @$query_ids[0];
		$query_id     = @$query_ids[1];
	}
	
	my $print_key_list;
	#print "$query_seq_id, $query_id, $query_seq_acc, $query_primary_seq_id,$query_secondary_seq_id\n";

#	if($query_seq_acc =~ /img1.model.1652_000041/){
#	#if($query_seq_acc =~ /2722.m00026/){
#	    #print "here\n";
#	    
#	}else{
#
#	    #next;
#	}
#

	$TF->logLocal("Starting @$query_ids[1]",0);
	$TF->logLocal("Getting Evidence",0);
	#print Dumper($job_ids),"\n";
	
	foreach my $id (@$job_ids) {
		my $job_id   = @$id[1];
		my $job_name = @$id[0];
			 
		my ($hits,$print_list) = &get_best_evidence( $dbh, $output, $job_id, $job_name, $query_seq_id, $query_seq_acc, $query_primary_seq_id, $query_secondary_seq_id );
	
	    
		if ( scalar(@{$hits})) {
			 if($env_nt_split){
		    	( $orf->{$query_seq_acc}->{$job_name}->{hits}, $orf->{$query_seq_acc}->{$job_name}->{print_list}) = ($hits,$print_list);
			 }else{
				( $orf->{$query_id}->{$job_name}->{hits}, $orf->{$query_id}->{$job_name}->{print_list}) = ($hits,$print_list);
			 }
		}else{
			if($env_nt_split){
		    	 $orf->{$query_seq_acc}->{$job_name}->{print_list} = $print_list;
			 }else{
				 $orf->{$query_id}->{$job_name}->{print_list} = $print_list;
			 }
		}
	}
	
	$TF->logLocal("Finished Evidence",0);
	$TF->logLocal("Getting Annotation",0);
	#print Dumper($orf);
	
    if($env_nt_split){
	#print "kkkk\n";
	#print Dumper($orf);
	#print "\n",Dumper($orf->{$query_seq_acc}),"\n";
	
   		&create_annotation($orf->{$query_seq_acc},$pfam2go_hsh);
    }else{
	#print "nnnnn\n";
	#print Dumper($orf);
	#print "\n",Dumper($orf->{$query_seq_acc}),"\n";
	

     	&create_annotation($orf->{$query_id},$pfam2go_hsh);
    }

    $TF->logLocal("Finished Annotation",0);

	&print_evidence_results($orf,$ofh);
	&print_annotation_results($orf,$afh);

    # Get the set name and use it for "library_id" field in metarep output
    my $library_name = &get_dataset_name($dbh);
    &print_metarep_results($orf,$mfh, $library_name);
	
	$TF->logLocal("Finished $query_id",0);	

}

$job_sth->finish();
$$query_ids_sth->finish();
$dbh->disconnect();

print $ofh scalar(localtime),"\n";
close $ofh;
close $afh;

exit(0);

##############################################################
sub check_options {

# Parse the options.  Override the configs with command-line options, if they've been given
# Make sure we have the required options by the end of this subprocedure.
	my $errors = '';

	if ( $opts{'help'}){
		&_pod;
		exit(0);
	}
	
	# get the options from the ini file.
	if ( $opts{'database'}){
	    if (-s $opts{database}) {
            $database = $opts{database};
	    } else {
	        die "\n$opts{database} does not exist or is an empty file\n";
	    }
	}else{
		print STDERR "\nDatabase is required. -d \n\n";
		print STDOUT "Usage: ./get_annotation.pl -d /database/location/sqlite.db -o /output_dir/file_prefix -gzip(optional)\n\n";
	
		exit(0);
	}
	
	if ( $opts{'output'}){
		$output = $opts{output};
	}else{
		print STDERR "\nOutput directory is required. -o /output_dir/ \n\n";
		print STDOUT "Usage: ./get_annotation.pl -d /database/location/sqlite.db -o /output/directory/ -gzip(optional)\n";
			
		exit(0);
	}
	
	
	$gzip = $opts{'gzip'} || 0;   
	
	$env_nt_split = 1; 
		
}

sub open_filehandle {
# returns a filehandle in either gzip or normal binmode, based on the gzip
# option as specifed (or not) by the user

    my ($filename, $gzip, $direction) = @_;
    
    my $fh;
    my $fhdir = ($direction eq 'IN') ? '<' : '>'; 
    
    if ($gzip) {
        open($fh,"$fhdir:gzip","$filename.gz") || die "Can't open $filename: $!\n";
    } else {
        open($fh,"$fhdir"."$filename") || die "Can't open $filename: $!\n";
    }    
    
    return $fh;
}

sub _pod {
    pod2usage( {-exitval => 0, -verbose => 2, -output => \*STDERR} );
}
