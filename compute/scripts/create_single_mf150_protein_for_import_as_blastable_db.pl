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

my $publicDir="/data/blast/public";
my $mooreSpreadsheet="/home/extapp/moore/data/spreadsheet/MooreFoundation.txt";
my $outputFile="/home/extapp/tmp/mf150_camera_export/mf150_public_PEPTIDE_asof_20070221_cameraized_deflines.fasta";

open(SS, "<$mooreSpreadsheet") || die "Could not open $mooreSpreadsheet to read\n";
open(OUTPUT, ">$outputFile") || die "Could not open $outputFile to write\n";

my %uniqueCheck;
my %mooreTagOrganismHash;

my $count=0;

while(<SS>) {
    chomp;
    my @arr=split /\t/;
    my $organism=$arr[1];
    my $tag=$arr[5];
    $mooreTagOrganismHash{$tag}=$organism;
    my $file="$publicDir\/$tag\/protein\/peptides.fasta";
    if (-e $file) {
        $count++;
    }
}
close(SS);

print "Found $count valid public peptide files\n";

my $dirList=`ls $publicDir`;
my @dirArr=split /\s+/, $dirList;
foreach my $dirName (@dirArr) {
    my $proteinPath=$publicDir . "\/" . $dirName . "/protein/peptides.fasta";
    if (-e $proteinPath) {
        my $organismName=$mooreTagOrganismHash{$dirName};
        if (! defined $organismName) {
            die "Could not find organism name for $dirName\n";
        }
        open(INPUT, "<$proteinPath") || die "Could not open $proteinPath to read\n";
        while(<INPUT>) {
            if (/^\s*>/) {
                my $defline=$_;
                chomp $defline;
                my @dArr=split /\,/, $defline;
                my $newDefline=">JCVI_PEP_MF150_";
                for (my $i=0;$i<=$#dArr;$i++) {
                    $dArr[$i]=~/\s*(\S.+\S)\s*/;
                    my $dc=$1;
                    if ($i==1) {
                        $newDefline .= $dc . " \/organism=\"$organismName\" \/locus=\"$dirName\"";
                    }
                    elsif ($i==2) {
                        $newDefline .= " \/description=\"" . $dc . "\"";
                    }
                    elsif ($i==3) {
                        $newDefline .= " \/genome_coordinates=\"" . $dc . "\"";
                    }
                }
                $newDefline=~/>(\S+)/;
                if (defined $uniqueCheck{$1}) {
                    die "label $1 failed uniqueness check for newDefline $newDefline from original defline $defline in file $proteinPath\n";
                }
                $uniqueCheck{$1}=$1;
                # add organism tag is not already present
                if ($newDefline=~/organism/) {
                    # OK
                } else {
                    $newDefline .= "\/organism=\"$organismName\" ";
                }
                if ($unmatched ne "") {
                    $newDefline .= "\/tag=\"$unmatched\" ";
                }
                print OUTPUT "$newDefline\n";
            } else {
                print OUTPUT $_;
            }
        }
        close(INPUT);
    }
}

close(OUTPUT);

