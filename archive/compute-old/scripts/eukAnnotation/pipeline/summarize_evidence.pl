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

use warnings;
use strict;
$|++;

use PerlIO::gzip;

# get file params
my ($fasta_file, $ev_file) = @ARGV;

# get number of fasta seqs:
my $num_seqs = `grep -c '>' $fasta_file`;

die "Couldn't count sequences in $fasta_file\n" unless $num_seqs;

my @file_prefix = split(/\//,$fasta_file);
my $prefix = $file_prefix[-1];
my @library = split(/\./,$prefix);


my ($efh,$sfh);
if ($ev_file =~ /gz$/) {
    open ($efh,"<:gzip", $ev_file) || die "Couldn't open $ev_file: $!\n";
} else {
    open ($efh, "< $ev_file") || die "Couldn't open $ev_file: $!\n";
}

open ($sfh, ">$library[0].summary");

my %counts;
while (<$efh>) {

    # first word is an accession if and only it has a digit in it.
    # (lazy way of skipping past the headers and record seperators.)
    if (/(\S+\d+\S+)\t(\S+)/) {
        $counts{$2}->{$1}++
     }

}

# print out the junks
print $sfh "Library:$library[0]\n";
print $sfh "Total Proteins:$num_seqs\n";

foreach my $jobname (keys %counts) {

    #compute the percentages
    my $num_hits = scalar(keys %{$counts{$jobname}});
    my $percent = $num_hits / $num_seqs * 100;

    printf $sfh ("%s\t %d (%.2f%%) \n", $jobname,$num_hits,$percent);

}

close $efh;
close $sfh;
exit(0);
