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
#  Note: for proteins, this command line can be used rather than this script, which is for nucleotides:
#
# cat Xdata/release-source-moore/mf150_FromGbff_20070305.protein.fasta | perl -e 'while(<>) { if (/^>(\S+) \/organism.+\/locus\_tag=\"(\S+)\_\d+\"/) { print "$1,$2\n"; } }' > proteinLabelToSpeciesTagMap.txt
#
#
#

my $speciesTagMapFile="orderedMf150SpeciesDefToTagMap.txt";
my $mf150NucleotideFastaFile="Xdata/release-source-moore/mf150_FromGbff_20070305.nucleotide.fasta";

# Read in the species tag map

my %speciesTagMap;

open(STM, "<$speciesTagMapFile") || die "Could not open $speciesTagMapFile\n";

while(<STM>) {
    chomp;
    /(.+)\t(\S+)/;
    my $speciesString=$1;
    my $speciesTag=$2;
    $speciesTagMap{$speciesString}=$speciesTag;
}

close(STM);

open(MF, "<$mf150NucleotideFastaFile") || die "Could not open $mf150NucleotideFastaFile\n";
while(<MF>) {
    my $line=$_;
    if ($line=~/^>/) {
        if ($line=~/^>(\S+) \/accession.+\/definition=\"(.+\S)\s+\d+, .+\"/) {
            my $label=$1;
            my $definition=$2;
            my $tag=$speciesTagMap{$definition};
            print "$label,$tag\n";
        } elsif ($line=~/^>(\S+) \/accession.+\/definition=\"Alteromonas macleodii .+strain Deep ecotype\d+, .+\"/) {
            my $label=$1;
            my $definition="Alteromonas macleodii Deep ecotype";
            my $tag=$speciesTagMap{$definition};
            print "$label,$tag\n";
        } else {
            print "Could not match line=$line\n";
        }
    }
}

close(MF);
