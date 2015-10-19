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
use Bio::SeqIO::genbank;
use Getopt::Std;

my %options=();
getopts("i:n:p:",\%options);

if (! (defined $options{i} && defined $options{n} && defined $options{p})) {
    die "usage: -i <input gbff> -n <output nucleotide fasta> -p <output protein fasta>\n";
}

my $inputFile=$options{i};
my $nucleotideFile=$options{n};
my $proteinFile=$options{p};

my %accToGiHash;
my %defByGiHash;

&doFirstPass();

my $input=Bio::SeqIO->new(-file=>$inputFile, -format=>'GenBank');
open(NUC,">$nucleotideFile") || die "Could not open nucleotide file $nucleotideFile to write\n";
open(PRO,">$proteinFile") || die "Could not open protein file $proteinFile to write\n";

while(my $seq=$input->next_seq()) {
    my $sequenceDisplay=$seq->display_id();
    my $sequenceSpecies=$seq->species->species;
    my $sequenceVersion=$seq->seq_version();
    my $fullAccession=$sequenceDisplay . "\." . $sequenceVersion;
    my $giNumber=$accToGiHash{$fullAccession};
    if (! defined $giNumber || $giNumber eq "") {
        die "Could not find GI entry for $giNumber\n";
    }
    my $definition=$defByGiHash{$giNumber};
    my @secondaryAccessions=$seq->get_secondary_accessions();
    my $annoCollection=$seq->annotation;

# DEBUGGING AID FOR GETTING NUCLEOTIDE FILE INFORMATION
#    print "Annotations:\n";
#    foreach my $key ($annoCollection->get_all_annotation_keys) {
#       print "KEY: $key\n";
#       my @annotations=$annoCollection->get_Annotations($key);
#       for my $value (@annotations) {
#           print $value->tagname . " " . $value->as_text . "\n";
#       }
#    }
#    print "Version:\n";
#    my @verAnno=$annoCollection->get_Annotations('VERSION');
#    foreach my $v (@verAnno) {
#       print "$v\n";
#    }
#    print "Secondary Accessions:\n";
#    foreach my $acc (@secondaryAccessions) {
#       print "$acc\n";
#    }

    my $sequence=$seq->seq();
    my $sequenceLength=length $sequence;
    print NUC ">NCBI_NT_$giNumber \/accession=\"$fullAccession\" \/organism=\"$sequenceSpecies\" \/length=\"$sequenceLength\" \/definition=\"$definition\"\n";
    my $fastaSequence=&fastaFormat($sequence);
    print NUC $fastaSequence;

# DEBUGGING SECTION FOR PROTEIN FILES
    for my $feat_object ($seq->get_SeqFeatures) {
        if ($feat_object->primary_tag eq 'CDS') {
            my %featHash;
            my $cdsGiNum;
            my $translation;
            for my $tag ($feat_object->get_all_tags) {
                my $value;
                for my $v ($feat_object->get_tag_values($tag)) {
                    $value .= "$v ";
                }
                $value=~/\s*(\S.+\S)\s*/;
                $value=$1;
                if ($tag eq 'db_xref' && $value=~/GI:(\S+)/) {
                    $cdsGiNum=$1;
                } elsif ($tag eq 'translation') {
                    $translation=$value;
                } else {
                    $featHash{$tag}=$value;
                }
            }
            if (defined $cdsGiNum && defined $translation) {
                my $defline=">NCBI_PEP_$cdsGiNum \/organism=\"$sequenceSpecies\" ";
                foreach my $tag (keys %featHash) {
                    my $value=$featHash{$tag};
                    if (defined $value && (length($value)>0)) {
                        $defline .= "\/$tag" . "=\"" . $value . "\" ";
                    }
                }
                $defline=~/^\s*(\S.+\S)\s*$/;
                $defline=$1;
                print PRO "$defline\n";
                my $fasta=&fastaFormat($translation);
                print PRO $fasta;
            } else {
                die "Could not find cds GI number or translation for sequence $giNumber\n";
            }
        }
    }

}
close(NUC);
close(PRO);


sub fastaFormat() {
    my $result="";
    my $width=60;
    my $sequence=$_[0];
    my $length=length($sequence);
    my $position=0;
    while ($position < $length) {
        my $end=$position+$width;
        if ($end>$length) {
            $end=$length;
        }
        my $len=$end-$position;
        $result .= substr($sequence,$position,$len) . "\n";
        $position+=$len;
    }
    return $result;
}

# This subroutine gets information that could not be obtained
# in an obvious way with the BioPerl objects.
sub doFirstPass() {
    my $prevDefinition="";
    my $defState=0;
    open(INPUT,"<$inputFile") || die "Could not open $inputFile to read\n";
    while(<INPUT>) {
        if (/^DEFINITION\s+(\S.+\S)\s*$/) {
            $prevDefinition=$1;
            $defState=1;
        } elsif ($defState==1) {
            if (/^(\S+)/) {
                $defState=0;
            } elsif (/\s+(\S.+\S)\s*$/) {
                $prevDefinition .= $1;
            }
        }
        if (/^VERSION\s+(\S+)\s+(\S+)/) {
            my $acc=$1;
            my $giEntry=$2;
            if ($giEntry=~/GI:(\S+)/) {
                my $giNumber=$1;
                $accToGiHash{$acc}=$giNumber;
                $defByGiHash{$giNumber}=$prevDefinition;
            } else {
                die "Could not parse giEntry $giEntry\n";
            }
        }
    }
    close(INPUT);
}

