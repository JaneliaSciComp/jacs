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

my $pandaSourceDir = "/home/ccbuild/tmp/pandaImport_20070227/data"; # CHANGED TO: /project/camera/data/release-source-panda/data
my $cameraSourceDir = "/project/camera/data/release-production/data";
my $targetDir = "/home/ccbuild/tmp/pandaImport_20070227/forFilestore";

# BELOW, moore directory changed to: /project/camera/data/release-source-moore
my $mooreGenomeNucleotideFile = "$pandaSourceDir\/moore\/mf150_FromGbff_20070305.nucleotide.fasta";
#my $mooreGenomePeptideFile = "$pandaSourceDir\/moore\/mf150_FromGbff_20070305.peptide.fasta";
my $mooreGenomePeptideFile = "/home/ccbuild/tmp/mf150_import/mf150_public_PEPTIDE_asof_20070221_cameraized_deflines.fasta";

my %sourceFiles = (
   "nonIdenticalPeptides.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/AllGroup.niaa",
                        "$pandaSourceDir\/pep\/AllGroup_WGS.niaa" ],
   "nonIdenticalNucleotides.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/AllGroup_CDS.fasta" ],
   "allMicrobialGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/Microbial_genomic.fasta",
                        "$pandaSourceDir\/nuc\/Microbial_WGS.fasta",
                        "$mooreGenomeNucleotideFile" ],
   "finishedMicrobialGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/Microbial_genomic.fasta" ],
   "finishedMicrobialProteins.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/Microbial.niaa" ],
   "draftMicrobialGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/Microbial_WGS.fasta",
                        "$mooreGenomeNucleotideFile" ],
   "draftMicrobialProteins.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/Microbial_WGS.niaa",
                        "$mooreGenomePeptideFile" ],
   "viralGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/Viral_genomic.fasta",
                        "$pandaSourceDir\/nuc\/Viral_WGS.fasta" ],
   "viralProteins.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/Viral.niaa" ],
   "fungalGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/Fungal_genomic.fasta",
                        "$pandaSourceDir\/nuc\/Fungal_WGS.fasta" ],
   "fungalGenomes.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/Fungal.niaa",
                        "$pandaSourceDir\/pep\/Fungal_WGS.niaa" ],
   "microbialEukaryoteGenomes.nucleotide.fasta"
                   => [ "$pandaSourceDir\/nuc\/MicrobialEuk_genomic.fasta",
                        "$pandaSourceDir\/nuc\/MicrobialEuk_WGS.fasta" ],
   "microbialEukaryoteProteins.peptide.fasta"
                   => [ "$pandaSourceDir\/pep\/MicrobialEuk.niaa",
                        "$pandaSourceDir\/pep\/MicrobialEuk_WGS.fasta" ],
   "allMetagenomicShotgunReads.nucleotide.fasta"
                   => [ "$cameraSourceDir\/gos\/reads\/gos-all.fasta",
                        "$cameraSourceDir\/stratified\/shotgun\/stratified_shotgun-all.fasta",
                        "$cameraSourceDir\/four_ocean_viromes\/four_ocean_viromes-all.fasta",
                        "Xdata/release-0-formatted/spanish_brine/FLAS_SMPL_CR30-2002.fasta" ],   # added 7/13/07
   "allMetagenomic16sReads.nucleotide.fasta"
                   => [ "$cameraSourceDir\/stratified\/16s\/stratified_16s-all.fasta" ],
   "allMetagenomicOrfNucleotides.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.orf.fasta",
                        "$pandaSourceDir\/annotation\/gos\/*.orf.fasta" ],
   "allMetagenomicOrfPeptides.peptide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.pep.fasta",
                        "$pandaSourceDir\/annotation\/gos\/*.pep.fasta" ],
   "allMetagenomicRNAs.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.ncRNA.fasta",
                        "$pandaSourceDir\/annotation\/gos\/*.ncRNA.fasta" ],
   "gosOrfNucleotides.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/gos\/*.orf.fasta" ],
   "gosOrfPeptides.peptide.fasta"
                   => [ "$pandaSourceDir\/annotation\/gos\/*.pep.fasta" ],
   "marineViromeReads.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/virome\/*.read.fasta" ],
   "hotOrfNucleotides.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.orf.fasta" ],
   "hotOrfPeptides.peptide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.pep.fasta" ],
   "gosPredictedRNAs.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/gos\/*.ncRNA.fasta" ],
   "hotPredictedRNAs.nucleotide.fasta"
                   => [ "$pandaSourceDir\/annotation\/hot\/*.ncRNA.fasta" ],
                   );

foreach my $target (keys %sourceFiles) {
    my $fullTarget="$targetDir\/$target";
    print "Making target $fullTarget...";
    # Do not re-do if it already exists
    if (! -e $fullTarget) {
        my @sourceArr=@{$sourceFiles{$target}};
        if (defined @sourceArr && (@sourceArr>0)) {
            foreach my $source (@sourceArr) {
                my $sourceListRef = &getFileListFromWildcards($source);
                foreach my $source (@{$sourceListRef}) {
                    print "Processing $source...\n";
                    if (-e $source) {
                        if ($source=~/\s*(\S+)\.gz$/) {
                            my $modSource=$1;
                            `gzip -d $source`;
                            $source=$modSource;
                        }
                        `cat $source >> $fullTarget`;
                    } else {
                        print "Skipping $source\n";
                    }
                }
            }
        }
    } else {
        "Target $fullTarget already exists - please delete to regenerate\n";
    }
    print "done.\n";
}

sub getFileListFromWildcards {
    my $path = $_[0];
    my $baseDir = &getBaseDir($path);
    print "Getting list from path $path...\n";
    my @list = `ls $path`;
    my @gzlist = `ls $path\.gz`;
    foreach my $gz (@gzlist) {
        push @list, $gz;
    }
    my @result = [];
    foreach my $path (@list) {
        $path=~/\s*(\S.+\S)\s*/;
        $path=$1;
        if (! -e $path) {
            my $decompressed;
            if ($path=~/(.+)\.gz/) {
                $decompressed=$1;
            }
            if (! -e $path) {
                if ((defined $decompressed) && (! -e $decompressed)) {
                    die "Error in getFileListFromWildcards: could not find $path\n";
                } else {
                    $path=$decompressed;
                }
            }
        }
        push @result, $path;
    }
    return \@result;
}

sub getBaseDir {
    my $path=$_[0];
    my @segments=split '\/', $path;
    my $baseDir="";
    for (my $i=0;$i<@segments-1;$i++) {
        if ($i<(@segments-2)) {
            $baseDir .= $segments[$i] . "/";
        } else {
            $baseDir .= $segments[$i];
        }
    }
    return $baseDir;
}
