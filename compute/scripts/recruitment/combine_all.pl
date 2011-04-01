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

   combine_all.pl build the combinedPlusSitePlusMate.hits files for the recruitment viewer using ncbi xml format of blast output file

=head1 USAGE

    combine_all.pl -W <working directory> -D <blast search directory>


=head1 OPTIONS

    -h          prints this help message

    -O          output  directory

    -B          blast directory
    -P          path for perl module
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
use warnings;
require "getopts.pl";
use Getopt::Long qw(:config no_ignore_case pass_through);

my $DEBUG = 0;
my ($input_file,$input_list,$blast_dir, $output_dir,,$path, $debug);
my %opts= ();
my @files= ();

&GetOptions(\%opts, "input_file|i=s", "input_list|I=s",
	   "debug|d=s", "blast_dir|B=s","output_dir|O=s","path|P=s",  "log|l=s", "help|h");
&parse_opts;

my $temp_dir="$output_dir/def";

my $clean_program="$path/clean_def.pl";
my $blast_program="$path/blast_parser.pl";
my $rec_program="$path/recruitment_builder.pl";

my $combine_all_log_FH = ();
my $log = -e $output_dir ? "$output_dir/combine_all.log.$$" : "combine_all.log.$$";

open ($combine_all_log_FH, ">$log") or warn "Could not create log file '$log'\n";
select STDOUT;
$|=1;
open(STDERR, ">&STDOUT");

print "\nSTART: " . localtime(time) . "\n\n";
print $combine_all_log_FH "\t\tSTART: " . localtime(time) . "\n";

&system_calls();

print $combine_all_log_FH "\t\tFINISH: " . localtime(time) . "\n";

print "\nFINISH: " . localtime(time) . "\n\n";

######### build nr blast hits file ###############

sub system_calls{
    


    my $blast_comd="/usr/local/perl $blast_program -B $blast_dir  -O $output_dir";

    my $builder_comd="/usr/local/perl $rec_program -B $output_dir -O  $output_dir -P $path";

      
    system ($blast_comd);
    
    system ($builder_comd);
    
      

}

 

#####subs######################

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

	elsif ($key eq "help") {
	   &print_usage if $val;
	}
    }
   
}

sub print_usage
{
	pod2usage( {-exitval => 1, -verbose => 2, -output => \*STDOUT} );
}
sub remove_dir {
    my $dir = shift;
    if (-e $dir) {
	my($cmd) = "rm -f -r $dir";

	system($cmd);
    }
}
