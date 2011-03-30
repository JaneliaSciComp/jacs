#!/usr/local/bin/perl

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

my $deflineMapFile=$ARGV[1];
my $inputEcFile=$ARGV[2];
my $outputEcFile=$ARGV[3];

my $argCount = scalar @ARGV;

if ($argCount < 6) {
    die "Usage: -m <defline map> -i <input ec results> -o <output expanded ec results>\n";
}

my $i=0;
while ($i<$argCount) {
    if ($ARGV[$i] eq "-m") {
        $i++;
        $deflineMapFile=$ARGV[$i];
    } elsif ($ARGV[$i] eq "-i") {
        $i++;
        $inputEcFile=$ARGV[$i];
    } elsif ($ARGV[$i] eq "-o") {
        $i++;
        $outputEcFile=$ARGV[$i];
    }
    $i++;
}

if (! (defined $deflineMapFile)) {
    die "Defline map file must be given with -m option\n";
}

if (! (defined $inputEcFile)) {
    die "Input EC file must be given with -i option\n";
}

if (! (defined $outputEcFile)) {
    die "Output EC file must be given with -o option\n";
}

my %defline_hash;

open(DEFLINE, "<$deflineMapFile") || die "Could not open $deflineMapFile to read\n";
while(<DEFLINE>) {
    my @arr=split /\s+/;
    my $pri=$arr[0];
    my $defline=$arr[1];
    $defline =~ s/\/ecinfo=//g;
    my @ecarr=split /\|/, $defline;
    my @result=();
    my $emptyCheck=0;
    foreach my $ec (@ecarr) {
        my @darr=split /\:/, $ec;
        my $ecnum=$darr[0];
        push @result, $ecnum;
        $emptyCheck=1;
    }
    if ($emptyCheck==0) {
       die "Could not parse any ec nums for priam label=$pri\n";
    }
    $defline_hash{$pri}=\@result;
}
close(DEFLINE);

open(INPUT, "<$inputEcFile") || die "Could not open $inputEcFile\n";
open(OUTPUT, ">$outputEcFile") || die "Could not open $outputEcFile\n";
while (<INPUT>) {
    /(\S+)\s+(\S+)\s+(.+)/;
    my $label=$1;
    my $pri=$2;
    my $restofline=$3;
    my $ecref=$defline_hash{$pri};
    if (! (defined $ecref)) {
        die "Could not find ec arr for priam label=$pri\n";
    }
    foreach my $ecm (@$ecref) {
        print OUTPUT "$label\t$ecm\t$restofline\n";
    }
}
close(INPUT);
close(OUTPUT);
