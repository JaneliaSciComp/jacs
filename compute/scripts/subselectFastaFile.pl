#/usr/local/perl

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
use Getopt::Std;

my %options=();
getopts("l:i:o:",\%options);

my $listFile;
my $inputFile;
my $outputFile;

if (defined $options{l}) {
    $listFile=$options{l};
} else {
    &usage;
}

if (defined $options{i}) {
    $inputFile=$options{i};
} else {
    &usage;
}

if (defined $options{o}) {
    $outputFile=$options{o};
} else {
    &usage;
}

# First we read the labels into a hash
my %labelsToFind;

open(LABELS, "<$listFile") or die "Could not open label file $listFile to read\n";
while(<LABELS>) {
    /(\S+)/;
    $labelsToFind{$1}=$1;
}
close(LABELS);

# Next, we step throught the fasta file and keep those sequences we want to keep
my $currentSequence="";
open(OUTPUT, ">$outputFile") or die "Could not open output file $outputFile to write\n";
open(FASTA, "<$inputFile") or die "Could not open input file $inputFile to read\n";
while(<FASTA>) {
    my $currentLine=$_;
    if (/^>(\S+)/) {
        if ($currentSequence ne "") {
            # Output what we have so far
            print OUTPUT $currentSequence;
            $currentSequence="";
        }
        if (defined $labelsToFind{$1}) {
            $currentSequence = $currentLine;
        }
    } else {
        if ($currentSequence ne "") {
            $currentSequence .= $currentLine;
        }
    }
}
close(FASTA);
# Check for final sequence
if ($currentSequence ne "") {
    print OUTPUT $currentSequence;
}
close(OUTPUT);

sub usage {
    die "Usage: -l <list file> -i <input file> -o <output file>\n";
}
