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

# This script does not rebuild the p_#.fasta files from the source files, but rather
# removes the existing formatdb indices from the BlastDatabaseFileNode directories,
# and then re-executes formatdb on them.

my $formatDbCmd="/usr/local/blast-2.2.15/bin/formatdb";
my $blastTopDir="/home/ccbuild/filestore/system";
my $nucBlockSize=100000000; # one-hundred million bp
my $proBlockSize=10000000;  # ten-million peptides

my @directoryList = (
                     "$blastTopDir/1020141375733104992:nucleotide",
                     "$blastTopDir/1020141386583769440:nucleotide",
                     "$blastTopDir/1020141388651561312:nucleotide",
                     "$blastTopDir/1020141391050703200:nucleotide",
                     "$blastTopDir/1020141393332404576:nucleotide",
                     "$blastTopDir/1020141395563774304:nucleotide",
                     "$blastTopDir/1020141398021636448:nucleotide",
                     "$blastTopDir/1020141400353669472:nucleotide",
                     "$blastTopDir/1020141449737404768:nucleotide",
                     "$blastTopDir/1020132834288861536:nucleotide",
                     "$blastTopDir/1020133561732170080:nucleotide",
                     "$blastTopDir/1020133911306436960:nucleotide",
                     "$blastTopDir/1020134323933675872:nucleotide",
                     "$blastTopDir/1020134688301252960:nucleotide",
                     "$blastTopDir/1020134852743135584:nucleotide",
                     "$blastTopDir/1020134912537133408:nucleotide",
                     "$blastTopDir/1020134972809281888:nucleotide",
                     "$blastTopDir/1020135115105239392:nucleotide",
                     "$blastTopDir/1020135157648064864:nucleotide",
                     "$blastTopDir/1020135195950448992:nucleotide",
                     "$blastTopDir/1020135239759954272:nucleotide",
                     "$blastTopDir/1020135276648857952:nucleotide",
                     "$blastTopDir/1020135432110735712:nucleotide",
                     "$blastTopDir/1020135486858985824:nucleotide",
                     "$blastTopDir/1020135548892741984:nucleotide",
                     "$blastTopDir/1020135698860081504:nucleotide",
                     "$blastTopDir/1020135850328981856:nucleotide",
                     "$blastTopDir/1020136016385671520:nucleotide",
                     "$blastTopDir/1020136172187287904:nucleotide",
                     "$blastTopDir/1020136327301038432:nucleotide",
                     "$blastTopDir/1020136483698245984:nucleotide",
                     "$blastTopDir/1020136806835814752:nucleotide",
                     "$blastTopDir/1020136979582419296:nucleotide",
                     "$blastTopDir/1020137141700657504:nucleotide",
                     "$blastTopDir/1020137492893925728:nucleotide",
                     "$blastTopDir/1020137654487875936:nucleotide",
                     "$blastTopDir/1020137801930244448:nucleotide",
                     "$blastTopDir/1020137963251564896:nucleotide",
                     "$blastTopDir/1020138109418864992:nucleotide",
                     "$blastTopDir/1020138235533197664:nucleotide",
                     "$blastTopDir/1020138505185001824:nucleotide",
                     "$blastTopDir/1020138731866161504:nucleotide",
                     "$blastTopDir/1020138893598523744:nucleotide",
                     "$blastTopDir/1020139334822527328:nucleotide",
                     "$blastTopDir/1020139853519520096:nucleotide",
                     "$blastTopDir/1020140031345426784:nucleotide",
                     "$blastTopDir/1020140913755685216:nucleotide",
                     "$blastTopDir/1020141085734732128:nucleotide",
                     "$blastTopDir/1020141262163935584:nucleotide",
                     "$blastTopDir/1020141319529431392:nucleotide",
                     "$blastTopDir/1020141370049823072:nucleotide",
                     "$blastTopDir/1020141372411216224:nucleotide",
                     "$blastTopDir/1020141452006523232:nucleotide",
                     "$blastTopDir/1020141454388887904:nucleotide",
                     "$blastTopDir/1020141456678977888:nucleotide",
                     "$blastTopDir/1020135239160168800:nucleotide" );

# We will do two passes - the first to clean and the second to formatdb
foreach my $item (@directoryList) {
    my ($dir,$type)=split /:/, $item;
    print "Processing directory $dir...\n";
    my @fileList = split /\s+/,&doSystem("ls $dir");
    foreach my $file (@fileList) {
        my $fullpath="$dir\/$file";
        if (! -e $fullpath) {
            die "Could not find file $fullpath\n";
        }
        if ($fullpath=~/\.fasta$/) {
            # do nothing on this pass
            #print "$dir\/$file : $type\n";
            #&doFormat($fullpath, $type);
        } else {
            &doSystem("rm $fullpath");
        }
    }
}

foreach my $item (@directoryList) {
    my ($dir,$type)=split /:/, $item;
    print "Processing directory $dir...\n";
    my @fileList = split /\s+/,&doSystem("ls $dir");
    foreach my $file (@fileList) {
        my $fullpath="$dir\/$file";
        if (! -e $fullpath) {
            die "Could not find file $fullpath\n";
        }
        if ($fullpath=~/\.fasta$/) {
            print "$dir\/$file : $type\n";
            &doFormat($fullpath, $type);
        } else {
            # &doSystem("rm $fullpath");
        }
    }
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
