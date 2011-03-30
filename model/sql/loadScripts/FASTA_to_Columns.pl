#!/usr/bin/perl

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

###############################################################################

use strict;
use Getopt::Std;
use vars qw($opt_d);

getopts("d");

my $usage = "usage: 
$0 
	-d {optional flag to put defline in second column between id and sequence.}

	< [FASTA file]
	> [seq_id\\tsequence]
";

my $include_defline=0;
if($opt_d){
	$include_defline=1;
}
###############################################################################

print STDERR "Trying to read FASTA file through STDIN.\n";
my ($defline, $prev_defline, $sequence);
while(<STDIN>){
	chomp;
	
	if(/^>/){
		$defline=$_;
		if($sequence ne ""){
			process_record($prev_defline, $sequence);
			$sequence="";
		}
		$prev_defline=$defline;
	}else{
		$sequence.=$_;
	}
}
process_record($prev_defline, $sequence);

print STDERR "Completed.\n";

###############################################################################

sub process_record{
	my $defline = shift;
	my $sequence = shift;

	my $seq_uid;
	if($defline=~/^>(\S+)/){
		$seq_uid=$1;
	}else{
		print STDERR "Defline: \"$defline\"\n";
		die "Could not parse defline for sequence id.\n";
	}

	if($include_defline){
		$defline=~s/^>//;
		print "$seq_uid\t$defline\t$sequence\n";
	}else{
		print "$seq_uid\t$sequence\n";
	}
}

