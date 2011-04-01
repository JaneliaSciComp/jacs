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

=head1 NAME

Need to parse the following information from xml  blast and put into tab deliminated file

##   1) query id

##    2) score

##    3) query begin

##    4) query end

##    5) query strand

##    6) subject id

##    7) subject begin

##    8) subject end

##    9) subject strand

##    10) note

##    11) number of identities

##    12) number of similarities

##    13) length of alignment

##    14) number of gap characters in query

##    15) number of gap characters in subject

##    16) query length

##    17) subject length

##    18) number of non-alignable characters in query (N\u2019s or X\u2019s)

##    19) number of non-alignable characters in subject (N\u2019s or X\u2019s)

##    20) search type

##    21) number of stops in query

##    22) number of stops in subject

##    23) number of gaps in query

##    24) number of gaps in subject


=head1 USAGE

    blast_parser.pl -B <xml format blast result dir> -O <output dir>

=head1  DESCRIPTION
    This program takes ncbi xml format blast search result using bio perl module to parse the file and builds tab-deliminated file for further processing
    
=head1  OUTPUT
=head1  CONTACT

    Qi Yang
    qyang@jcvi.org

=begin comment
    ## legal values for status are active, inactive, hidden, unstable
    status: active
    keywords:xml  ncbi blast
=end comment
=cut


use strict;
use warnings;
use Getopt::Long qw(:config no_ignore_case pass_through);
use Pod::Usage;
use DBI;
use Bio::SearchIO;
use XML::Twig;
my $DEBUG = 0;
my ($input_file,$input_list,$blast_dir, $output_dir, $debug,$log_file,$delete_def_file);
my %opts= ();
my @files= ();

&GetOptions(\%opts, "input_file|i=s", "input_list|I=s","remove_blast_def|D=s",
	   "debug|d=s", "blast_dir|B=s","output_dir|O=s", "log|l=s", "help|h");
&parse_opts;


my $blast_parser_log_FH = ();


my $blast_comb_file="$output_dir/"."blast_comb_file";


open(OUTPUT ,">$blast_comb_file")|| die "can't open $blast_comb_file for writing \n";
my $count=0;

&get_blast_file($blast_dir);





###### sub function##############


my $blast_file="";


sub get_blast_file{

    my $total=0;
    
    opendir (DIR, "$blast_dir")|| die "could not open for reading\n";

    while(defined(my $file=readdir(DIR))){
	if ($file =~/oos/){next;}
	elsif ($file =~/blast\.out/){
	   
	    $blast_file= "$blast_dir/"."$file";
	    
	    
	    $count= &parse_blast_file($blast_file,$blast_dir,$file);
	  
	    $total =$total + $count;
	    
	   
	    }
	    
	}
    closedir(DIR);
    
   
}

my %record=();



sub parse_blast_file{
   
    my ($file)=@_;
    
  
    my $count=0;
    my $q_count=0;
    my $sb=0;
    my ($result,$query_name,$query_length,$num_hits,$query_id,@record,$search_type)="";
    my $searchin = new Bio::SearchIO(-format => 'blastxml',
				     tempfile => 1,
				     -file  =>"$file");
    
   
    while ( $result = $searchin->next_result() ) {
   
    my $query_name=$result->query_name;
   
    my $query_length=$result->query_length;
    my $num_hits=$result->num_hits;
    my $search_type=$result->algorithm;
  
    $query_id =$query_name;
    (@record)=split(/\|/,$query_name);

    if ($record[0] && $record[1]){
	  
	$query_id ="$record[0]"."|"."$record[1]";
	
    }
   
    while( my $hit = $result->next_hit ) {
	    
	    
	my $subject_name=$hit->name;
	    
	my $subject_id=$subject_name;
	
	if (!$subject_id){next;}
	while (my $hsp = $hit->next_hsp){
	    
	    my $score= $hsp->score;
	    my $query_start=$hsp->start('query');
	    my $query_end=$hsp->end('query');
	    my $query_strand=$hsp->query->frame;
	    
	    my $subject_start=$hsp->start('hit');
	    my $subject_end=$hsp->end('hit');
	    my $subject_strand=$hsp->hit->frame;
	    my $identities=$hsp->percent_identity;
	    my $num_identical=$hsp->num_identical;
	    my $num_conserved=$hsp->num_conserved; 
	   
	    my $len_align=$hsp->length('hit');
	    my $subject_length=$hsp->length('total');

	    
	    $count++;
	    my $note ="NA";
	    my $q_num_gap=0;
	    my $s_num_gap=0;
	    my $q_num_non_alig=0;
	    my $s_num_non_alig=0;
	    my $s_num_stop=0;
	    my $q_num_stop=0;
	    
            if ( $query_id){

	    my $key="$query_id\t$score\t$query_start\t$query_end\t$query_strand\t$subject_id\t$subject_start\t$subject_end\t$subject_strand\t$note\t$num_identical\t$num_conserved\t$len_align\t$q_num_gap\t$s_num_gap\t$query_length\t$subject_length\t$q_num_non_alig\t$s_num_non_alig\t$search_type\t$q_num_stop\t$s_num_stop\t$q_num_gap\t$q_num_stop";
	 
	    print  OUTPUT "$key\n";

	}
	}
    
    } 


}
 
   
    return $count;

}

sub parse_opts
{
    
    while (my($key, $val) = each (%opts)) {

	if ($key eq "input_file") {
	    push @files, $val;
	}
	elsif ($key eq "input_list") {
	    open(my $fh, "<$val") || die "Error accessing input list $opts{input_list}: $!";
	    while (my $file = <$fh>) {
		chomp $file;
		next if $file =~ /^\s*$/;
		push @files, $file;
	    }
	}

	elsif ($key eq "blast_dir") {
	    $blast_dir = $val;
	}
	elsif ($key eq "output_dir") {
	    $output_dir = $val;
	}
	elsif ($key eq "debug") {
	    $debug = $val;
	}
	elsif ($key eq "log") {
	    $log_file = $val;
	}
	elsif ($key eq "remove_blast_def") {
	    $delete_def_file=$val;
	}
	elsif ($key eq "help") {
	   &print_usage if $val;
	}
    }
   
}

sub print_usage
{
    pod2usage( {-exitval => 1, -verbose => 2, -output => \*STDOUT} );
}
