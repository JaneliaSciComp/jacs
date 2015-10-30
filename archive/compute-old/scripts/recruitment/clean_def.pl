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
use warnings;
use Getopt::Long qw(:config no_ignore_case pass_through);
use Pod::Usage;



my $DEBUG = 0;

my ($input_file,$input_list,$blast_dir, $output_dir, $debug,$log_file);

my %opts= ();

my @files= ();

&GetOptions(\%opts, "input_file|i=s", "input_list|I=s",
	   "debug|d=s", "blast_dir|B=s","output_dir|O=s", "log|l=s", "help|h");
my $flag=0;
&parse_opts;

&get_clean_blast_file($blast_dir);
if ($flag ==0){
    &get_query_info_file($blast_dir);}

###### sub function##############

my $blast_file="";
sub get_query_info_file{

    opendir (DIR, $blast_dir)|| die "could not open for reading\n";

    while(defined(my $file=readdir(DIR))){
	if ($file =~/\.oos/){
	    next;
	}
	if ($file =~/blast\.out/){
	   
	    $blast_file= "$blast_dir/"."$file";
	   
	    &process_query($blast_file,$output_dir,$file);
	    $flag =1;
	    }
	
    }
    closedir(DIR);
}

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
    
    my $dir ="$output_dir/def";

    &create_dir($dir);
    
    my $temp_file="$dir/"."$file";
    
    open (OUTFILE, ">$temp_file");

    while(my $line =<FILE>){
	
	#if($line=~/\<BlastOutput_query-def>//){
	if($line=~/\/number_of_sites/){  
	   
	    ($line, my @juck)=split(/\/number_of_sites/,$line);  
	    chomp $line;
	   
	    $line=~ s/\s+$//;
	    
	    

 
	       
	       
		   print  OUTFILE "$line</Hit_def>\n";
	       
	   }
	   elsif ($line =~/Hit_hsps/){
		$line=~ s/\s+$//;
	    }

	    elsif ($line=~/Hsp_midline/){
		next;
	    }
	else{
	
	    print  OUTFILE "$line";
	}

	
    }
   
    close ($temp_file);
    close($blast_file);
    return $temp_file;
}
sub create_dir {
    my $dir = shift;
    if (!(-e $dir)) {
	mkdir ($dir, 0777);
	chmod (0777, $dir);
    }    
}
sub process_query

{ 
    my ($blast_file,$output_dir,$file)=@_;

    
    open (FILE, $blast_file);
    
    my $dir ="$output_dir/def";

    &create_dir($dir);
    
    my $temp_file="$dir/"."query_file";
    if (-s $temp_file){exit;}
    
    open (OUTFILE, ">$temp_file");

    while(my $line =<FILE>){
	my ($query_name, $len)="";
	
	if($line=~/\/BlastOutput_query-def/){
	    my ($temp, $rest)=split (/\<\/BlastOutput_query-def/,$line);
	    my ($junk, $query_name)=split(/\>/,$temp);
	    
	   
	    print OUTFILE "$query_name\t";
	
	}
	if ($line=~/\/BlastOutput_query-len/){
	    my ($temp, $rest)=split (/\<\/BlastOutput_query-len/,$line);
	    my ($junk, $query_len)=split(/\>/,$temp);
	    
	   
	    print OUTFILE "$query_len\n";
	
    }
    }
    close ($temp_file);
    close($blast_file);
    
}
