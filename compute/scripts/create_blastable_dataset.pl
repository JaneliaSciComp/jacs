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

my $formatDbCmd="/usr/local/blast-2.2.15/bin/formatdb";
my $sourceTopDir="/project/camera/data/release-production/data";
my $altSourceTopDir="/scratch/cameraPrepareBlastWorkspace_20061011";
my $blastTopDir="/home/ccbuild/filestore/system";
my $nucBlockSize=100000000; # one-hundred million bp
my $proBlockSize=10000000;  # ten-million peptides

my $coreClusterSequencesProteinFasta         =$sourceTopDir . "/cluster/clusters/core/core_cluster_sequences.fasta";
my $finalClusterRepresentativeProteinFasta   =$sourceTopDir . "/cluster/clusters/final/cluster_representative_sequences.fasta";
my $gosClusterOrfsNucleotideFasta            =$sourceTopDir . "/cluster/orfs/gos.orf.dna.fasta";
my $move858ClusterOrfsNucleotideFasta        =$sourceTopDir . "/cluster/orfs/move858.orf.dna.fasta";
my $ncbiMgClusterOrfsNucleotideFasta         =$sourceTopDir . "/cluster/orfs/ncbi_mg.orf.dna.fasta";
my $tgiClusterOrfsNucleotideFasta            =$sourceTopDir . "/cluster/orfs/tgi.orf.dna.fasta";
my $tigrGeneIndicesNucleotideFasta           =$sourceTopDir . "/cluster/orfs/supporting_materials/tigr_gene_indices.fasta";
my $ensemblClusterPeptideFasta               =$sourceTopDir . "/cluster/peptides/sequences/ensembl.pep.fasta";
my $gosClusterOrfsPeptideFasta               =$sourceTopDir . "/cluster/peptides/sequences/gos.orf.pep.fasta";
my $move858ClusterOrfsPeptideFasta           =$sourceTopDir . "/cluster/peptides/sequences/move858.orf.pep.fasta";
my $ncbiMgClusterOrfsPeptideFasta            =$sourceTopDir . "/cluster/peptides/sequences/ncbi_mg.orf.pep.fasta";
my $nrPeptideFasta                           =$sourceTopDir . "/cluster/peptides/sequences/nr.pep.fasta";
my $tgiClusterOrfsPeptideFasta               =$sourceTopDir . "/cluster/peptides/sequences/tgi.orf.pep.fasta";
my $siteSpecific16sNucleotideFasta           =$sourceTopDir . "/gos/16s/site_specific_16s.fasta";
my $gosCombinedScaffoldsNucleotideFasta      =$sourceTopDir . "/gos/assemblies/combined/gos_combined_scaffolds.fasta";
my $gosAssembliesSiteSpecificNucleotideFasta =$sourceTopDir . "/gos/assemblies/site_specific/site_specific_scaffolds.fasta";
my $gosReadsNucleotideFasta                  =$altSourceTopDir . "/gos_reads/siteReadsNucleotide.fasta";
my $kinaseFamiliesProteinFasta               =$sourceTopDir . "/kinome/pkl_families.fasta";
my $microbialDraftGenomesNucleotideFasta     =$sourceTopDir . "/microbial_references/draft_genomes.fasta";
my $microbialFinishedGenomesNucleotideFasta  =$sourceTopDir . "/microbial_references/finished_genomes.fasta";
my $move858ScaffoldsNucleotideFasta          =$sourceTopDir . "/move858/assemblies/move858_scaffolds.fasta";
my $move858ReadsNucleotideFasta              =$sourceTopDir . "/move858/reads/JCVI_SMPL_1103283000058.fasta";
my $viralProteinFasta                        =$sourceTopDir . "/viral/family_peptides.fasta";
my $viralNucleotideFasta                     =$sourceTopDir . "/viral/family_scaffolds.fasta";
my $siphoviridaeScaffoldsNucleotideFasta     =$sourceTopDir . "/viral/sipho_e_asm/sipho_scaffold.fasta";

my %blastSourceDirMap = (
    "$coreClusterSequencesProteinFasta"         => "$blastTopDir/1015439106244608352:Core cluster protein sequences (P)",
    "$finalClusterRepresentativeProteinFasta"   => "$blastTopDir/1015439658454090080:Final representative cluster protein sequences (P)",
    "$gosClusterOrfsNucleotideFasta"            => "$blastTopDir/1015439539457491296:GOS ORF nucleotide sequences (N)",
    "$move858ClusterOrfsNucleotideFasta"        => "$blastTopDir/1015439209818751328:Chesapeake viral (MOVE858) ORF nucleotide sequences (N)",
    "$ncbiMgClusterOrfsNucleotideFasta"         => "$blastTopDir/1015438551023616352:NCBI microbial genome ORF nucleotide sequences (N)",
    "$tgiClusterOrfsNucleotideFasta"            => "$blastTopDir/1015439374868808032:TIGR gene index ORF nucleotide sequences (N)",
    "$tigrGeneIndicesNucleotideFasta"           => "$blastTopDir/1015439966567661920:TIGR gene index full-length nucleotide sequences (N)",
    "$ensemblClusterPeptideFasta"               => "$blastTopDir/1015439818651337056:Ensembl predicted peptide sequences (P)",
    "$gosClusterOrfsPeptideFasta"               => "$blastTopDir/1015438648499241312:GOS ORF peptide sequences (P)",
    "$move858ClusterOrfsPeptideFasta"           => "$blastTopDir/1015439023931392352:Chesapeake viral (MOVE858) ORF peptide sequences (P)",
    "$ncbiMgClusterOrfsPeptideFasta"            => "$blastTopDir/1015438843588903264:NCBI microbial genome ORF peptide sequences (P)",
    "$nrPeptideFasta"                           => "$blastTopDir/1015439256484577632:NCBI NR peptide sequences v20050210 (P)",
    "$tgiClusterOrfsPeptideFasta"               => "$blastTopDir/1015439305910255968:TIGR gene index ORF peptide sequences (P)",
    "$siteSpecific16sNucleotideFasta"           => "$blastTopDir/1015439766008627552:Site-specific 16s nucleotide sequences (N)",
    "$gosCombinedScaffoldsNucleotideFasta"      => "$blastTopDir/1015439915564925280:GOS combined assembly nucleotide sequences (N)",
    "$gosAssembliesSiteSpecificNucleotideFasta" => "$blastTopDir/1015439492242211168:GOS site-specific assembly nucleotide sequences (N)",
    "$gosReadsNucleotideFasta"                  => "$blastTopDir/1015439713080705376:GOS read nucleotide sequences (N)",
    "$kinaseFamiliesProteinFasta"               => "$blastTopDir/1015439154395218272:Kinase-like protein sequences (P)",
    "$microbialDraftGenomesNucleotideFasta"     => "$blastTopDir/1015438780334604640:Microbial draft genome nucleotide sequences (N)",
    "$microbialFinishedGenomesNucleotideFasta"  => "$blastTopDir/1015439607359078752:Microbial finished genome nucleotide sequences (N)",
    "$move858ScaffoldsNucleotideFasta"          => "$blastTopDir/1015439869498884448:Chesapeake viral (MOVE858) scaffold nucleotide sequences (N)",
    "$move858ReadsNucleotideFasta"              => "$blastTopDir/1015438972416950624:Chesapeake viral (MOVE858) read nucleotide sequences (N)",
    "$viralProteinFasta"                        => "$blastTopDir/1015438725603131744:GOS predicted viral protein sequences (P)",
    "$viralNucleotideFasta"                     => "$blastTopDir/1015439440035709280:GOS predicted viral nucleotide sequences (N)",
    "$siphoviridaeScaffoldsNucleotideFasta"     => "$blastTopDir/1015438924903874912:Siphoviridae assembly nucleotide sequences (N)" );

# We want to step through each member of the hash and do the formatdb

foreach my $key (keys %blastSourceDirMap) {
    print "Processing source directory $key...\n";
    my $value=$blastSourceDirMap{$key};
    my ( $targetDir, $description ) = split ":", $value;
    print "Using target dir $targetDir and description $description...\n";
    my $dataType;
    if ($value=~/\(N\)/) {
        $dataType="nucleotide";
    } elsif ($value=~/\(P\)/) {
        $dataType="protein";
    } else {
        die "Could not parse data type from key $value\n";
    }
    if (! -d $targetDir) {
        die "Could not find target directory $targetDir\n";
    }
    &doSystem("rm $targetDir\/*");
    my $sourceFasta=$key;
    if ($dataType eq "nucleotide") {
        $size=&splitAndFormatFastaFile($sourceFasta, $targetDir, $nucBlockSize, $dataType);
    } elsif ($dataType eq "protein") {
        $size=&splitAndFormatFastaFile($sourceFasta, $targetDir, $proBlockSize, $dataType);
    } else {
        die "Do not recognize data type " . $dataType . "\n";
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
