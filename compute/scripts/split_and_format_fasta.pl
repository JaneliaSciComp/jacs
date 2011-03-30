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

use Getopt::Std;

my %options=();
getopts("f:t:d:",\%options);

my $formatDbCmd="/usr/local/blast-2.2.15/bin/formatdb";
my $blastTopDir="/home/ccbuild/filestore/system";
my $nucBlockSize=100000000; # one-hundred million bp
my $proBlockSize=10000000;  # ten-million peptides

my $sourceFastaPath;
my $targetDirectory;
my $sourceType;

if (defined $options{f}) {
    $sourceFastaPath=$options{f};
} else {
    &usage();
}

if (defined $options{d}) {
    $targetDirectory=$options{d};
} else {
    &usage();
}

if (defined $options{t}) {
    $sourceType=$options{t};
} else {
    &usage();
}

my $dataType;
if ($sourceType=~/N/) {
    $dataType="nucleotide";
} elsif ($sourceType=~/P/) {
    $dataType="protein";
} else {
    die "Could not parse data type from key $value\n";
}
if (! -d $targetDirectory) {
    die "Could not find target directory $targetDirectory\n";
}

if ($dataType eq "nucleotide") {
    $size=&splitAndFormatFastaFile($sourceFastaPath, $targetDirectory, $nucBlockSize, $dataType);
} elsif ($dataType eq "protein") {
    $size=&splitAndFormatFastaFile($sourceFastaPath, $targetDirectory, $proBlockSize, $dataType);
} else {
    die "Do not recognize data type " . $dataType . "\n";
}

print "Finished. Total size is $size\n";

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
    my $size=0;
    my $file=$_[0];
    my $targetDir=$_[1];
    my $blockSize=$_[2];
    my $dataType=$_[3];
    my $filePrefix=$targetDir . "/p";
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
            my $lineLength=(length $_)-1;
            $count += $lineLength;
            $size += $lineLength;
        }
        print OUTPUT $_;
    }
    close(OUTPUT);
    &doFormat("$filePrefix\_$index\.fasta", $dataType);
    close(INPUT);
    return $size;
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
        `cd $baseDir; $formatDbCmd -p F -i $filename`;
    } elsif ($dataType eq "protein") {
        `cd $baseDir; $formatDbCmd -p T -i $filename`;
    } else {
        die "Do not recognize data type " . $dataType . "\n";
    }
}

sub usage {
    die "Usage: -f <source fasta file> -d <target database directory> -t <type, N or P> \n";
}
