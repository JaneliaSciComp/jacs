#!/usr/bin/env perl

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

use FindBin qw($Bin);
use lib ("$Bin/lib");

use strict;
use Fasta::SimpleIndexer;
use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);
use Data::Dumper;
use File::Basename;

my %options = ();
my $results = GetOptions( \%options, 'cogFile|c=s', 'fastaInputFile|m=s', 'outputDir|o=s', 'maxCogSeqCount|s=s', 'extension|e=s', 'log|l=s', 'debug=s');

my $cogFile = $options{'cogFile'};
my $fastaInputFile = $options{'fastaInputFile'};
my $outDir = $options{'outputDir'};
my $maxCogSeqCount = $options{'maxCogSeqCount'};
if($options{'extension'} eq ''){
    $options{'extension'} = 'fsa';
}

# mapping from Seq.id and/or Seq-data-import.identifier to polypeptide sequence
my $Prot = {};
# mapping from polypeptide Feature.id to Seq.id
my $Feat = {};

#Get rid of trailing slashes in directory names
$fastaInputFile =~ s/\/+$//;
$outDir =~ s/\/+$//; 

if( !$fastaInputFile )
{
    die "no Fasta File Directory specified";
}
else
{
    if( ! -e $fastaInputFile )
    {
        die "could not open list file: $fastaInputFile";
    }
}

if( !$cogFile )
{
    die "cog file not specified";
}

if( !$outDir )
{
    die "output directory not specified";
}
else
{
    if( ! -d $outDir )
    {
        mkdir( $outDir );
    }
}

my $fasta = new Fasta::SimpleIndexer($fastaInputFile);

my ($filename, $directory, $suffix) = &fileparse($fastaInputFile);
my $lookup_file = $directory . "/" . $filename . "_id_lookup.txt";
my $id_lookup_hsh = {};

#make hash of original loci
if(-s $lookup_file){
	$id_lookup_hsh = &parse_fasta_lookup($lookup_file);
}

open( INPUTCOGS, "<$cogFile" ) or die "could not open $cogFile.";

my $cog = undef;
my $list = [];

while( my $line = <INPUTCOGS> )
{
    if( $line =~ /^\t([\S]*)/ )
    {
        # A new sequence has been found and added to the current cog.
        push( @{$list}, $1 );
    }

    if( $line =~ /^COG\s+=\s+([^,\s]+)/ )
    {
        # A new cog has been encountered, flush the previous to disk if present
        my $newcog = $1;
        &outputCog($cog, $list) if (defined($cog));
        $cog = $newcog;
        $list = [];
    }
}

outputCog($cog,$list) if (defined($cog));
exit(0);

## subroutines
sub parse_fasta_lookup{
	my $file = shift;
	$id_lookup_hsh = {};
	
	open(IN, $file);
	my @loci = <IN>;
	close IN;
	
	foreach my $locus (@loci){
		my($original, $new) = split(/\t/,$locus);		
		$new =~ s/\s+$//;
		$id_lookup_hsh->{$new} = $original;
	}
	
	return $id_lookup_hsh;
}

sub outputCog {
    my($cog, $list) = @_;
	
    if(scalar(@{$list})>1){
    	
		if(@{$list} <= $maxCogSeqCount){
		    open( OUTFILE, ">$outDir/$cog.$$.$options{'extension'}" ) or die "could not open $cog.$options{'extension'}";

		    foreach my $seq ( @{$list} )
		    {   	
                my $residues = undef;
                
                #If lookup file exists grab original loci found in fasta
                #Loci in cog output do not have the | as it causes clustal to fail
				if(exists $id_lookup_hsh->{$seq}){
                	$seq = $id_lookup_hsh->{$seq};
                }
                
                $residues = $fasta->get_record($seq);
                die "no sequence data found for seq=$seq" if (!defined($residues));
                print OUTFILE $residues;
		    }
		    close( OUTFILE );
		}
		else{
		    open( OUTFILE, ">$outDir/$cog.$$.$options{'extension'}" ) or die "could not open $cog.$options{'extension'}";
		    foreach my $seq ( @{$list} )
		    {
                print OUTFILE ">$seq\n";
                print OUTFILE "X\n";
		    }
		    close( OUTFILE );
		}
    }
}
