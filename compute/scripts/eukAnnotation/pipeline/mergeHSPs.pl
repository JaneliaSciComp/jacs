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

#
# merge tab-delimited HSP files sorted by evalue (1st column asc) and bit_score (2nd column desc)
use strict;
our $errorMessage;

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
our $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::dataset;
use EAP::compute;
$| = 1;

my $outfile = splice @ARGV, 0, 1;
my @inputfiles = @ARGV;
my @tmpflags = ();
foreach my $inputfile ( @inputfiles ) {
	push @tmpflags, 0;
}
my $iteration = 0;

# open next two files
while ( scalar @inputfiles > 1 ) {
	my ( $file1, $file2 ) = splice @inputfiles, 0, 2;
	my ( $tmp1, $tmp2 ) = splice @tmpflags, 0, 2;
	if ( ! -e $file1 ) { die "\nFile \"$file1\" does not exist.\n" }
	if ( ! -e $file2 ) { die "\nFile \"$file2\" does not exist.\n" }

	if ( !open(FILE1, "<$file1") ) { die "\nCould not open \"$file1\".\n" }
	if ( !open(FILE2, "<$file2") ) { die "\nCould not open \"$file2\".\n" }


# read first line from each file
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

# open temporary file for output
	my $tmpfile = "$outfile.$$.tmp" . $iteration++ ;
	unlink $tmpfile;
	if ( !open( TMP, ">$tmpfile" ) ) { die "\nCould not open temporary file '|$tmpfile\".\n" }
	
# compare lines
# write lowest sorted line to STDOUT and read next line from its file
# stop when both files exhausted
	while ( defined $line1 || defined $line2 ){

# if both files are exhausted we are done
		if ( !defined $line1 && ! defined $line2 ) {

# if file1 exhausted, write remaining lines from file2
		} elsif ( !defined $line1 ) {
			while ( defined $line2 ) {
				print TMP $line2;
				$line2 = <FILE2>;
			}
			close FILE2;
			if ( $tmp2 ) { unlink $file2 }
			
			close( TMP );
			@inputfiles = ( @inputfiles, $tmpfile );
			@tmpflags = ( @tmpflags, 1 );

# if file2 exhausted, write remaining lines from file1
		} elsif ( !defined $line2 ) {
			while ( defined $line1 ) {
				print TMP $line1;
				$line1 = <FILE2>;
			}
			close FILE1;
			if ( $tmp1 ) { unlink $file1 }
			close( TMP );
			@inputfiles = ( @inputfiles, $tmpfile );
			@tmpflags = ( @tmpflags, 1 );
			
# else write line with lowest sort key
		} elsif ( $values1[0] < $values2[0] ) {
			print TMP $line1;
			$line1 = <FILE1>;
			if ( !defined $line1 ) {
				close FILE1;
				if ( $tmp1 ) { unlink $file1 }
			} else {
				@values1 = split /\t/, $line1;
			}
		} elsif ( $values2[0] < $values1[0] ) {
			print TMP $line2;
			$line2 = <FILE2>;
			if ( !defined $line2 ) {
				close FILE2;
				if ( $tmp2 ) { unlink $file2 }
			} else {
				@values2 = split /\t/, $line2;
			}
		} elsif ( $values1[1] >= $values2[1] ) {
			print TMP $line1;
			$line1 = <FILE1>;
			if ( !defined $line1 ) {
				close FILE1;
				if ( $tmp1 ) { unlink $file1 }
			} else {
				@values1 = split /\t/, $line1;
			}
		} else {
			print TMP $line2;
			$line2 = <FILE2>;
			if ( !defined $line2 ) {
				close FILE2;
				if ( $tmp2 ) { unlink $file2 }
			} else {
				@values2 = split /\t/, $line2;
			}
		}
	}
}
if ( ! rename $inputfiles[0], $outfile ) { die "\nCould not write to output file \"$outfile\".\n" }
exit(0);

