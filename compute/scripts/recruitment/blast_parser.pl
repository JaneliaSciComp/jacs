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



my $DEBUG = 0;

my ($input_file,$input_list,$blast_dir, $output_dir, $debug,$log_file);

my %opts= ();

my @files= ();
my $hmm_ref;
my $hit_ref;
my %hit=();
my $hsp_count=0;
my $hit_count=0;
my $read_count=0;
#print "\nSTART: " . localtime(time) . "\n\n";
&GetOptions(\%opts, "input_file|i=s", "input_list|I=s",
	   "debug|d=s", "blast_dir|B=s","output_dir|O=s", "log|l=s", "help|h");
my $flag=0;

&parse_opts;

my $blast_comb_file="$output_dir/"."blast_comb_file";


open(OUTPUT ,">$blast_comb_file")|| die "can't open $blast_comb_file for writing \n";
&get_clean_blast_file($blast_dir);

close(OUTPUT);
#print "\nFINISH: " . localtime(time) . "\n\n";

###### sub function##############

my $blast_file="";


sub get_clean_blast_file{

    opendir (DIR, $blast_dir)|| die "could not open for reading\n";

    while(defined(my $file=readdir(DIR))){
	
	if ($file =~/\.oos/){
	    next;
	}
	if ($file =~/blast\.out/){
	   
	    $blast_file= "$blast_dir/"."$file";
	   
	    my $temp_file=&process_def($blast_file,$output_dir,$file);
	   	   	   
	    }
    }
    closedir(DIR);
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

	elsif ($key eq "help") {
	   &print_usage if $val;
	}
    }
   
}

sub print_usage
{
	pod2usage( {-exitval => 1, -verbose => 2, -output => \*STDOUT} );
}
sub process_def

{ 
    my ($blast_file,$output_dir,$file)=@_;

        
    open (FILE, $blast_file);
    
      
    my %hit=();

   
    my ($query_name,$query_length,$search_type,$subject_name,$subject_length,$hsp_num ,$score,$query_start ,$query_end ,$query_strand,$subject_start,$subject_end,$subject_strand,$num_identical,$num_conserved,$len_align,$other,$rest,$junk,@rest)="";
    my $num=0;
    my $note ="NA";
    my $q_num_gap=0;
    my $s_num_gap=0;
    my $q_num_non_alig=0;
    my $s_num_non_alig=0;
    my $s_num_stop=0;
    my $q_num_stop=0;
    
    while(my $line =<FILE>){
	$line=~ s/\s+$//;
	$line=~ s/^\s+//;
	if($line=~/\<BlastOutput_program\>/){
	    ($other,$rest)=split (/\<BlastOutput_program\>/,$line);
	    ($search_type,$junk)=split (/\<\/BlastOutput_program\>/,$rest);
	    $search_type=~ s/\s+$//;
	    
	    $search_type=~ s/^\s+//;
	    
	}
	
	if($line=~/\<BlastOutput_query-def\>/){

	    ($other,$rest)=split (/\<BlastOutput_query-def\>/,$line);
	    
	    ($query_name ,$junk)=split (/\<\/BlastOutput_query-def>/,$rest);
	    $query_name=~ s/\s+$//;
	    
	    $query_name=~ s/^\s+//;
	   
	}
	if($line=~/\<BlastOutput_query-len\>/){
	    ($other,$rest)=split (/\<BlastOutput_query-len\>/,$line);
	    
	    ($query_length ,$junk)=split (/\<\/BlastOutput_query-len>/,$rest);
	    $query_length=~ s/\s+$//;
	    $query_length=~ s/^\s+//;
	}
	if($line=~/\<Hit_def\>/){
	    
	    $hit_count++;
	    $line =~s/^\s+//;
	    
	    ($other, @rest)=split (/\//, $line);
	   
	    ($junk, $subject_name)=split (/\<Hit_def\>/,$other);
	    $subject_name=~ s/\s+$//;
	    $subject_name=~ s/^\s+//;
	}
	if($line=~/\<Hit_len\>/){
	    chomp $line;
	   
	    $line =~s/^\s+//;
	    ($other, $rest)=split(/\<Hit-len\>/, $line);
	    
	    ($subject_length,$junk)=split (/\<\/Hit_len\>/,$other);
	    
	    ($junk, $subject_length)=split (/\<Hit_len\>/,$subject_length);
	    $subject_length=~ s/^\s+//;
	    $subject_length=~ s/\s+$//;
	}
	if($line=~/\<Hsp_num\>/){
	   ($other,$rest)=split (/\<Hsp_num\>/,$line);
	   ($hsp_num ,$junk)=split (/\<\/Hsp_num\>/,$rest);
	  
	   $hit_ref->{$hsp_count}->{'hsp_num'}=$hsp_num;
	   $hsp_count++;
	   $hsp_num=~ s/^\s+//;
	   $hsp_num=~ s/\s+$//;  	   
	}

	if($line=~/\<Hsp_score\>/){
	   ($other,$rest)=split (/\<Hsp_score\>/,$line);
	    
	   ($score ,$junk)=split (/\<\/Hsp_score\>/,$rest);
	   $score=~ s/^\s+//;
	   $score=~ s/\s+$//;
	   $hit{$subject_name}{$hsp_num}{'hsp_score'}=$score;
	   $hit{$subject_name}{$hsp_num}{'subject_length'}=$subject_length;
	}

	if($line=~/\<Hsp_query-from>/){
	    ($other,$rest)=split (/\<Hsp_query-from\>/,$line);
	    
	    ($query_start ,$junk)=split (/\<\/Hsp_query-from\>/,$rest);
	    $query_start=~ s/^\s+//;
	    $query_start=~ s/\s+$//;
	}
	if($line=~/\<Hsp_query-to>/){
	   ($other,$rest)=split (/\<Hsp_query-to\>/,$line);
	    
	   ($query_end ,$junk)=split (/\<\/Hsp_query-to\>/,$rest);
	   $query_end=~ s/^\s+//;
	   $query_end=~ s/\s+$//;
	   
	   
	   if ($query_start>$query_end){
	       my $temp =$query_start;
	       $query_start=$query_end;
	       $query_end=$temp;
	   }
	   $hit{$subject_name}{$hsp_num}{'Hsp_query-from'}=$query_start;
	   $hit{$subject_name}{$hsp_num}{'Hsp_query-to'}=$query_end;
	   
	}


	if($line=~/\<Hsp_hit-from\>/){
	   ($other,$rest)=split (/\<Hsp_hit-from\>/,$line);
	    
	   ($subject_start ,$junk)=split (/\<\/Hsp_hit-from\>/,$rest);
	  $subject_start=~ s/\s+$//;
	  $subject_start=~s/^\s+//;
	    
	}
	if($line=~/\<Hsp_hit-to\>/){
	    ($other,$rest)=split (/\<Hsp_hit-to\>/,$line);
	    
	    ($subject_end ,$junk)=split (/\<\/Hsp_hit-to\>/,$rest);
	    $subject_end =~ s/\s+$//;
	    $subject_end =~s/^\s+//;
	    if ($subject_start>$subject_end){
		my $temp =$subject_start;
		$subject_start=$subject_end;
		$subject_end=$temp;
	    }
	    $hit{$subject_name}{$hsp_num}{'Hsp_hit-from'}=$subject_start;
	    $hit{$subject_name}{$hsp_num}{'Hsp_hit-to'}=$subject_end;
	    
	}
	
  
	
	if($line=~/\<Hsp_query-frame\>/){
	   ($other,$rest)=split (/\<Hsp_query-frame\>/,$line);
	    
	   ($query_strand ,$junk)=split (/\<\/Hsp_query-frame\>/,$rest);

	   $query_strand=~ s/\s+$//;
	   $query_strand=~ s/^\s+//;
	   $hit{$subject_name}{$hsp_num}{'Hsp_query-frame'}=$query_strand;

       }
	if($line=~/\<Hsp_hit-frame\>/){
	   ($other,$rest)=split (/\<Hsp_hit-frame\>/,$line);
	    
	  ($subject_strand ,$junk)=split (/\<\/Hsp_hit-frame\>/,$rest);
	   $subject_strand=~ s/\s+$//;
	   $subject_strand=~ s/^\s+//;                 
	   $hit{$subject_name}{$hsp_num}{'Hsp_hit-frame'}=$subject_strand;
       }
	
	if($line=~/\<Hsp_identity\>/){
	   ($other,$rest)=split (/\<Hsp_identity\>/,$line);
	    
	   ($num_identical ,$junk)=split (/\<\/Hsp_identity\>/,$rest);
	   $num_identical=~ s/\s+$//;
	    $num_identical=~ s/^\s+//;                 
	   $hit{$subject_name}{$hsp_num}{'Hsp_identity'}= $num_identical;                   

       }
	if($line=~/\<Hsp_positive\>/){
	   ($other,$rest)=split (/\<Hsp_positive\>/,$line);
	    
	   ($num_conserved,$junk)=split (/\<\/Hsp_positive\>/,$rest);
	   $num_conserved=~ s/^\s+//;   
	   $num_conserved=~ s/\s+$//;                 
	   $hit{$subject_name}{$hsp_num}{'Hsp_positive'}=$num_conserved;
       }
	if($line=~/\<Hsp_align-len\>/){
	    ($other,$rest)=split (/\<Hsp_align-len\>/,$line);
	    
	    ( $len_align,$junk)=split (/\<\/Hsp_align-len\>/,$rest);
	 
	    $len_align=~ s/^\s+//;
	    $len_align=~ s/\s+$//;  
	    $hit{$subject_name}{$hsp_num}{'Hsp_align-len'}=$len_align;  
	    
	  
	}
	


    }
    
    close($blast_file);
   
  
    

     foreach my $subject_name (keys %hit){

	foreach my $score(keys %{$hit{$subject_name}}){
	    
	    $read_count++;
	    my $read="$query_name\t$hit{$subject_name}{$score}{'hsp_score'}\t$hit{$subject_name}{$score}{'Hsp_query-from'}\t$hit{$subject_name}{$score}{'Hsp_query-to'}\t$hit{$subject_name}{$score}{'Hsp_query-frame'}\t$subject_name\t$hit{$subject_name}{$score}{'Hsp_hit-from'}\t$hit{$subject_name}{$score}{'Hsp_hit-to'}\t$hit{$subject_name}{$score}{'Hsp_hit-frame'}\t$note\t$hit{$subject_name}{$score}{'Hsp_identity'}\t$hit{$subject_name}{$score}{'Hsp_positive'}\t$hit{$subject_name}{$score}{'Hsp_align-len'}\t$q_num_gap\t$s_num_gap\t$query_length\t$hit{$subject_name}{$score}{'subject_length'}\t$q_num_non_alig\t$s_num_non_alig\t$search_type\t$q_num_stop\t$s_num_stop\t$q_num_gap\t$q_num_stop";
	   
	    print OUTPUT "$read\n";
  
  
	}

    }


  
  }  

sub create_dir {
    my $dir = shift;
    if (!(-e $dir)) {
	mkdir ($dir, 0777);
	chmod (0777, $dir);
    }    
}


