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

my $publicDir="/data/blast/public";
my $mooreSpreadsheet="/home/extapp/moore/data/spreadsheet/MooreFoundation.txt";
my $outputFile="/home/extapp/tmp/mf150_camera_export/mf150_public_asof_20070221_cameraized_deflines.fasta";

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
    my $file="$publicDir\/$tag\/nucleotide\/nucleotides.fasta";
    if (-e $file) {
        $count++;
    }
}
close(SS);

print "Found $count valid public nucleotide files\n";

my $dirList=`ls $publicDir`;
my @dirArr=split /\s+/, $dirList;
foreach my $dirName (@dirArr) {
    my $nucleotidePath=$publicDir . "\/" . $dirName . "/nucleotide/nucleotides.fasta";
    if (-e $nucleotidePath) {
        my $organismName=$mooreTagOrganismHash{$dirName};
        if (! defined $organismName) {
            die "Could not find organism name for $dirName\n";
        }
        open(INPUT, "<$nucleotidePath") || die "Could not open $nucleotidePath to read\n";
        while(<INPUT>) {
            if (/^\s*>/) {
                my $defline=$_;
                chomp $defline;
                my @dArr=split /\[/, $defline;
                my $newDefline="";
                foreach my $d (@dArr) {
                    my $key;
                    my $value;
                    if ($d=~/^>(\d+)/) {
                        if ($newDefline ne "") {
                            die "JCVI lablel must be first component from parsing $defline\n";
                        }
                        $newDefline .= ">JCVI_MF150_$1 \/locus=\"$dirName\" ";
                    } elsif ($d=~/^>JCVI_(\S+)/) {
                        if ($newDefline ne "") {
                            die "JCVI lablel must be first component from parsing $defline\n";
                        }
                        my $labelSection=$1;
                        my $numberComponent;
                        if ($labelSection=~/\|/) {
                            my @labelArr=split /\|/, $labelSection;
                            if (@labelArr==1) {
                                $numberComponent=$labelArr[0];
                            } elsif (@labelArr>1) {
                                $numberComponent=$labelArr[@labelArr-1];
                            } else {
                                die "Could not get label component from $d\ of defline $defline\n";
                            }
                        } else {
                            $numberComponent=$1;
                        }
                        $newDefline .= ">JCVI_MF150_$numberComponent \/locus=\"$dirName\" ";
                    } elsif ($d=~/(^\S+)=(.+\S)\s*\]\s*/) {
                        $key=$1;
                        $value=$2;
                    } elsif ($d=~/(.+\ssp\.\s.+\S)\s*\]/) {
                        $key="organism";
                        $value=$1;
                    } elsif ($d=~/(\S.+\S)\s*\]/) {
                        #unknown contents
                        $key="tag";
                        $value=$1;
                    } else {
                        die "Could not parse dArr component $d from defline $defline\n";
                    }
                    if (defined $key && defined $value) {
                        if ($key eq "organism") {
                            $value=$organismName;
                        }
                        $newDefline .= "\/$key=\"$value\" ";
                    }
                    if (!$newDefline=~/^>(\S+)/) {
                        die "new defline $newDefline must start with proper label\n";
                    }
                }
                $newDefline=~/>(\S+)/;
                if (defined $uniqueCheck{$1}) {
                    die "label $1 failed uniqueness check for defline $defline\n";
                }
                $uniqueCheck{$1}=$1;
                # add organism tag is not already present
                if ($newDefline=~/organism/) {
                    # OK
                } else {
                    $newDefline .= "\/organism=\"$organismName\" ";
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
