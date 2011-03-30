#!/bin/csh

blastall -p blastn -d B.dna.fasta -i A.dna.fasta > blastn.out
blastall -p blastp -d B.pep.fasta -i A.pep.fasta > blastp.out
blastall -p blastx -d B.pep.fasta -i A.dna.fasta > blastx.out
blastall -p tblastn -d B.dna.fasta -i A.pep.fasta > tblastn.out
blastall -p tblastx -d B.dna.fasta -i A.dna.fasta > tblastx.out
megablast -d B.dna.fasta -i A.dna.fasta -D 2 -W 11> megablast.out
