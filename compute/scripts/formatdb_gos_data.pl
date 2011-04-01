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

# NOTE: THIS SCRIPT IS DEPRECATED BY "create_blastable_dataset.pl"

my $sourceTopDir="/home/data1/GOS/release/0";
my $blastTopDir="/home/data1/GOS/blast";
my $nucDir="$blastTopDir/nucleotide";
my $proDir="$blastTopDir/protein";
my $nucBlockSize=100000000; # one-hundred million bp
my $proBlockSize=10000000;  # ten-million peptides

my %blastSourceDirMap = ( "$sourceTopDir/Assembly/Combined/*.fasta" => "$nucDir/AsmCombined" ,
                          "$sourceTopDir/Assembly/SiteSpecific/*.fasta" => "$nucDir/AsmSiteSpecific" ,
                          "$sourceTopDir/ORFs/Sequences/all.gos.orf.dna.fasta" => "$nucDir/GosOrfs" ,
                          "$sourceTopDir/ORFs/Sequences/all.ncbiMicrobialGenomes.org.dna.fasta" => "$nucDir/NCBIMicrobialOrfs" ,
                          "$sourceTopDir/ORFs/Sequences/all.TIGR.orf.dna.fasta" => "$nucDir/TIGROrfs" ,
                          "$sourceTopDir/ORFs/Sequences/viral.orf.dna.fasta" => "$nucDir/ViralOrfs" ,
                          "$sourceTopDir/ORFs/Underlying/all.tgi.fastas" => "$nucDir/TGIOrfs" ,
                          "$sourceTopDir/ORFs/16s_rDNA/Sequences/site_specific_16s.fasta" => "$nucDir/SiteSpecific16sOrfs" ,
                          "$sourceTopDir/Peptides/Sequences/all.gos.orf.pep.fasta" => "$proDir/GosOrfPeptides" ,
                          "$sourceTopDir/Peptides/Sequences/all.ensembl.pep.fasta" => "$proDir/EnsemblPeptides" ,
                          "$sourceTopDir/Peptides/Sequences/all.ncbiMicrobialGenomes.orf.pep.fasta" => "$proDir/NCBIMicrobialOrfPeptides" ,
                          "$sourceTopDir/Peptides/Sequences/all.TIGR.orf.pep.fasta" => "$proDir/TIGROrfPeptides" ,
                          "$sourceTopDir/Peptides/Sequences/all.nr.pep.fasta" => "$proDir/NRPeptides" ,
                          "$sourceTopDir/Peptides/Sequences/viral.orf.pep.fasta" => "$proDir/ViralOrfPeptides" ,
                          "$sourceTopDir/Clusters/RepresentativeSequences/cluster_reps.fasta" => "$proDir/ClusterRepPeptides" ,
                          "$sourceTopDir/Clusters/CoreClusterSequences/renamed_core_cluster_multifastas" => "$proDir/CoreClusterPeptides" ,
                          "$sourceTopDir/ProteinKinases/Sequences/*.pep" => "$proDir/ProteinKinases" ,
                          "$sourceTopDir/Viral/Scaffolds/*.fa" => "$nucDir/ViralScaffolds" ,
                          "$sourceTopDir/MicrobialReferences/Draft/*.fasta" => "$nucDir/MicrobialReferenceDraft" ,
                          "$sourceTopDir/MicrobialReferences/Complete/ncbiCompleteMicrobialGenomes.fa" => "$nucDir/NCBIMicrobialReference" );

if (! -d $nucDir) {
    &doSystem("mkdir $nucDir");
}

if (! -d $proDir) {
    &doSystem("mkdir $proDir");
}

# We want to step through each member of the hash and do the formatdb

foreach my $key (keys %blastSourceDirMap) {
    print "Processing $key...\n";
    my @sourceFiles;
    my $baseDir;
    if (-d $key) {
        $baseDir=$key;
    } else {
        $baseDir=&getBaseDir($key);
    }
    if (-d $key || $key=~/\*/) {
        # ls has dynamic behavior depending on whether dir, file, or dir w/ 1 file, dir w many etc.
        $sourceList = &doSystem("ls $key");
        my @sourceArr=split /\s+/, $sourceList;
        foreach my $sourceMem (@sourceArr) {
            my @pathTestArr=split '\/', $sourceMem;
            if (@pathTestArr > 1) {
                # assume this is a full-path
                push (@sourceFiles, $sourceMem);
            } else {
                # must add path information
                my $sourceFile = $baseDir . "\/" . $sourceMem;
                push (@sourceFiles, $sourceFile);
            }
        }
    } else {
       next;
        if (! -e $key) {
            die "Could not find source file $sourceFiles\n";
        }
        push (@sourceFiles, $key);
    }
    my $targetDir = $blastSourceDirMap{$key};
    my $dataType=&getDataTypeFromTargetDir($targetDir);
    if (! -d $targetDir) {
        &doSystem("mkdir $targetDir");
    }
    my $targetDirName=&getFileNameFromPath($targetDir);
    my $targetFasta="$targetDir\/$targetDirName.fasta";
    # Create target fasta file
    foreach my $sourceFile (@sourceFiles) {
        &doSystem("cat $sourceFile >> $targetFasta");
    }
    # Split the files
    if ($dataType eq "nucleotide") {
        &splitAndFormatFastaFile($targetFasta, $nucBlockSize, $dataType);
    } elsif ($dataType eq "protein") {
        &splitAndFormatFastaFile($targetFasta, $proBlockSize, $dataType);
    } else {
        die "Do not recognize data type " . $dataType . "\n";
    }
    &doSystem("rm $targetFasta");
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

sub getDataTypeFromTargetDir {
    my $targetDir=$_[0];
    if ($targetDir=~/$nucDir/) {
        return "nucleotide";
    } elsif ($targetDir=~/$proDir/) {
        return "protein";
    } else {
        die "Do not recognize data type in path " . $targetDir;
    }
}

sub doSystem {
    my $cmd=$_[0];
    print $cmd . "\n";
    my $value = `$cmd`;
    if ($?!=0) {
        die "Non-zero error for command $cmd: $?\n";
    }
    return $value;
}

sub splitAndFormatFastaFile {
    my $file=$_[0];
    my $blockSize=$_[1];
    my $dataType=$_[2];
    my @fileComponents=split '\.', $file;
    if (@fileComponents>=3) {
        die "Could not parse prefix from filename $file\n";
    }
    my $filePrefix=$fileComponents[0];
    open(INPUT, "<$file") || die "Could not open file $file to read\n";
    my $count=0;
    my $index=0;
    print "Creating $filePrefix\_$index\.fasta\n";
    open(OUTPUT, ">$filePrefix\_$index\.fasta") || die "Could not open file $filePrefix\_$index\.fasta to write\n";
    while(<INPUT>) {
        if (/\s*>/) {
            if ($count >= $blockSize) {
                close OUTPUT; # close current file
                &doFormat("$filePrefix\_$index\.fasta", $dataType);
                $index++;
                print "Creating $filePrefix\_$index\.fasta\n";
                open(OUTPUT, ">$filePrefix\_$index\.fasta") || die "Could not open file $filePrefix\_$index\.fasta to write\n";
                $count=0;
            }
        } else {
            $count += (length $_) - 1;
        }
        print OUTPUT $_;
    }
    close(OUTPUT);
    &doFormat("$filePrefix\_$index\.fasta", $dataType);
    close(INPUT);
}

sub getFileNameFromPath {
    my $path=$_[0];
    my @components=split '\/', $path;
    my $name=$components[@components-1];
    return $name;
}

sub doFormat {
    my $file = $_[0];
    my $dataType = $_[1];
    my $baseDir=&getBaseDir($file);
    my $filename=&getFileNameFromPath($file);
    if ($dataType eq "nucleotide") {
        `cd $baseDir; formatdb -p F -i $filename`;
    } elsif ($dataType eq "protein") {
        `cd $baseDir; formatdb -p T -i $filename`;
    } else {
        die "Do not recognize data type " . $dataType . "\n";
    }
}
