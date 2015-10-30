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

   recruitment_builder.pl build the combinedPlusSitePlusMate.hits files for the recruitment viewer

=head1 USAGE

    recruitmet_builder.pl -B <blast  directory> -O <output  directory>


=head1 OPTIONS

    -h          prints this help message

    -O          output  directory

    -B          blast directory
    -P          module path
    -I          list of blast  files
    -i          single blast file  name
    
     
=head1 DESCRIPTION

=head1 INPUT
=head1 OUTPUT

   ## flatfiles are generated, one pertaining to start site analysis, the other to overlap analysis.

=head1 CONTACT

    Qi Yang
    qyang@jcvi.org
    
  
=begin comment
    ## legal values for status are active, inactive, hidden, unstable
  status: active
  keywords: blast, read, recruitment
=end comment
    
=cut
    1;


eval "exec /usr/local/perl -S $0 $*"
    if $running_under_some_shell;


use strict;
#require "$ENV{'SGC_SCRIPTS'}/sgc_library.dbi";
require "getopts.pl";
use Getopt::Long qw(:config no_ignore_case pass_through);

my $DEBUG = 0;
my ($input_file,$input_list,$blast_dir, $output_dir,$path, $debug);
my %opts= ();
my @files= ();


&GetOptions(\%opts, "input_file|i=s", "input_list|I=s",
	   "debug|d=s", "blast_dir|B=s","output_dir|O=s","path|P=s", "log|l=s", "help|h");
&parse_opts;

my $com_site_file="$output_dir/"."combinedPlusSite.hits";
my $mate_file="$output_dir/"."mate.list";
my $bad_file ="$output_dir/"."bad_file";
my $nr_file ="$output_dir/"."nr.file";
my $easy_file ="$output_dir/"."easy.file";
my $rescue_file ="$output_dir/"."rescue.file";
my $com_site_mate_file="$output_dir/"."combinedPlusSitePlusMate.hits";

my $header_file ="$output_dir/all.headers";

my $log_file = "$output_dir/${$}"."_builder.log";

select STDOUT;
$|=1;
open(STDERR, ">&STDOUT");


my %record=();

my %nr_record=();
my %bad_record=();

my %combined_record=();
my %map=();
my %good=();

my $recruitment_builder_log_FH = ();


&removeOverlapping();


######### build nr blast hits file ###############


############# Find small and easy contained hits##############

my $count=0;
my $key_c=0;
foreach my $key (sort keys(%nr_record)){
    
    if ($key){
	$key_c++;
    }
    my $flap = 25;
    my $overlap = 300;
    my $pid = 0.3;
    my $l = $key;
    chomp;
    my @d = (split /\s+/,$key);
    next if $d[0] eq $d[5];
    
    next if ($d[12] - $d[13]) < $overlap;
   
    next if ($d[10] + $d[17]) / $d[12] < $pid;

   
    my $qcontained = $d[2] < $flap && ($d[15] - $d[3]) < $flap;
    my $scontained = $d[6] < $flap && ($d[16] - $d[7]) < $flap;
    next unless $qcontained || $scontained;
  
    $combined_record{$l}=1;
    $map{$d[5]}=1;
    $count++;
  

    ##### For finding the missing data#####

    $good{$d[5]}{$d[0]}=1;

   
}

foreach my $key (sort keys(%nr_record)){

   
    if (!exists $combined_record{$key}){

	$bad_record{$key}=1;

    }
}




foreach my $key (sort keys(%bad_record)){
    
   
    my $flap = 20;
    my $overlap = 100;
    my $pid = 0.3;
    my $l = $key;
    chomp;
    my @d = (split /\s+/,$key);
    next if $d[0] eq $d[5];
    
    next if ($d[12] - $d[13]) < $overlap;
    
    next if ($d[10] + $d[17]) / $d[12] < $pid;
    
    my $qcontained = $d[2] < $flap && ($d[15] - $d[3]) < $flap;
    my $scontained = $d[6] < $flap && ($d[16] - $d[7]) < $flap;
    next unless $qcontained || $scontained;
   
    $combined_record{$l}=1;
    $map{$d[5]}=1;
    $count++;

    ##### For finding the missing data#####
    $good{$d[5]}{$d[0]}=1;

   
}


############### Build mate_list record hash################

my %seen=();
my %mate_list=();

open (INFILE, $header_file)|| die "could not open $header_file  for reading\n";

while (<INFILE>) {
  
  
    if (/^>(\S+)/ && ($map{$1})) {  
	
	
	my $id = $1;
   
	my $src = "-";
	if (/\/src=(\S+)/) {
	    $src = $1;
      
	}
	if (/mate=(\S+)/) {
	    $map{$id} = "$1\t$src";
      
	} else {
	    $map{$id} = "0\t$src";
	}

    }
}  

close($header_file);
foreach my $id (keys %map) {
   
    next if $seen{$id};
    my $v = $map{$id};
    my @d = split /\t/,$v;
   
    $d[1] = "-" unless defined($d[1]);
    my $list1="$id\t$d[0]\t$d[1]";
    my $list2="$d[0]\t$id\t$d[1]";
 
    $mate_list{$list1}=1;
    $mate_list{$list2}=1;
  
  

    $seen{$id} = 1;
    $seen{$map{$id}} = 1;
    
}




#close(OUTFILE);

############## Find missing mate  and add to the contained_record hash ###########

foreach my $record (keys %mate_list){
    my @d = (split /\s+/,$record);
    if ($d[1] && $d[0]) {
	$map{$d[0]} = $d[1];
	#print "$map{$d[0]}\n";
	$map{$d[1]} = $d[0];
    }

}

foreach my $key (keys %good){
    

}


foreach my $key (keys %bad_record){
    chomp $key;
  
    my @d = (split /\s+/, $key);
    my $l=$key;
  
    if (!exists($good{$d[5]}) && exists($good{$map{$d[5]}})) {
       
	if (exists($good{$map{$d[5]}}{$d[0]})) {  ### if the mate exists on the same seq
	    my $delta = $d[6] + ($d[16] - $d[7]);
	    if ($d[16]<=0) { print "Subject length for read for $d[5] is $d[16]\n"; }
	    if ($d[16]>0 && ($delta / $d[16]) < 0.2){

		$combined_record{$l}=1;
	    }
	    
	    $good{$d[5]} = 1;
	}
    }


}




###########Add site information to the contained file########

my %mate=();

foreach my $key( keys(%mate_list)){
   
    if ($key){
	chomp $key;
	my @d = (split /\s+/, $key);

	if ($d[0]) {
	    $map{$d[0]} = $d[2];
	    if ($d[1]) {
		$mate{$d[0]} = $d[1];
		$mate{$d[1]} = $d[0];
	    } else {
		$mate{$d[0]} = "-";
      }
	}
	if ($d[1]) {
	    $map{$d[1]} = $d[2];
	    if ($d[0]) {
		$mate{$d[0]} = $d[1];
		$mate{$d[1]} = $d[0];
	    } else {
		$mate{$d[1]} = "-";
	    }
	}
    }
	
 
}
open (OUTPUT, ">$com_site_file") || die "could not open $com_site_file for writing\n";
my $record=0;
foreach my $key(keys %combined_record){
 
  chomp;
  my @d = (split /\s+/,$key);
 
  next unless exists($map{$d[5]});
  
  $d[22] = $map{$d[5]};
  $d[23] = $mate{$d[5]};
  my $l = join "\t",@d;
  $record++;
  print OUTPUT $l,"\n";
 
}

close(OUTPUT);

my $cmd ="/usr/local/perl $path/mate_check.pl -site $path/sample.info -layout $com_site_file -btab - | sort -T $output_dir -k 3,3n > $output_dir/combinedPlusSitePlusMate.hits";

system ($cmd);




#####subs######################


sub removeOverlapping{
    
    opendir (DIR, $blast_dir)|| die "could not open $blast_dir  for reading\n";

    while(defined(my $zip_file=readdir(DIR))){
	my $file="";

       	if ($zip_file =~/\.bz2/){

	    ($file,my @junk)=split(/\.bz2/,$zip_file);
	    my $file ="$blast_dir/"."$file";
	    my $zip_file ="$blast_dir/"."$zip_file";
	    system ("bunzip2 $zip_file");
	}


	    elsif ($zip_file =~/blast/){

	    $file =$zip_file;
	    $file ="$blast_dir/"."$file";
	 
	    open (FILE, $file) || die "can't open $file for reading \n";
	    
	    while (<FILE>){
		$record{$_}=1;
		      	
	    }
	       
	}
    }
    my @l;
    my $l;
    
    foreach my $key (sort keys(%record)){
	
	my $d = $key;

	chomp $key;
    
	my $k;

	my @d = (split /\s+/, $_);
  
	if (@l && $d[5] eq $l[5]) {
             
	if ($d[2] == $l[2]) {
	    if ($d[3] < $l[3]) {
		$k = 1;
	    } else {
		$k = 0;
	    }
	} elsif ($d[3] == $l[3]) {
	    if ($d[2] > $l[2]) {
		$k = 1;
	    } else {
		$k = 0;
	    }
	} else {
	    $k = -1;
	}
	if ($k) {
	    print OUTPUT $l;
	  
	    $nr_record{$l}=1;
	    if ($k == 1) {
		@d = @l;
		$d = $l;
	    }
	}
	else{
	     $bad_record{$l}=1;
	}

	}
    else {

	$nr_record{$l}=1;
    }
	$l = $d;
	@l = @d;
   
    }

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
	elsif ($key eq "path") {
	    $path = $val;
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
