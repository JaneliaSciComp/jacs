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
our $errorMessage;

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::vics;


my $sqlitedb = $ARGV[0];
$sqlitedb = realpath( $sqlitedb );
if ( substr( $sqlitedb, length( $sqlitedb ) - 3, 3 ) eq ".gz" ) {
	system "gunzip $sqlitedb";
	$sqlitedb = substr( $sqlitedb, 0, length( $sqlitedb ) - 3 );
}
if ( ! -e $sqlitedb ) { die "Could not find sqlite db $sqlitedb" }
my $annodir = dirname( $sqlitedb );

system "mkdir $annodir/old_annotation";
system "mv $sqlitedb.* $annodir/old_annotation";
system "mv $annodir/com2GO* $annodir/old_annotation";
system "mv $annodir/*.summary* $annodir/old_annotation";
print "Old results backed up\n";

system "echo \"update htab set total_score=(select sum(domain_score) from htab h where h.query_seq_id=htab.query_seq_id and h.subject_hmm_id=htab.subject_hmm_id and h.job_id=htab.job_id);\" | sqlite3 $sqlitedb";
print "HTAB corrected\n";

if ( defined $ARGV[1] ) {
	my $fasta = $ARGV[1];
	if ( substr( $fasta, length( $fasta ) - 3, 3 ) eq ".gz" ) {
		system "gunzip $fasta";
		$fasta = substr( $fasta, 0, length( $fasta ) - 3 );
	}
	$fasta = realpath( $fasta );
	if ( ! -e $fasta ) { die "Could not find fasta file $fasta" }
	system "/local/devel/ANNOTATION/ViralMGPipeline/branches/0.33/addQueryFasta.pl -D $sqlitedb -N proteinstmp -F $fasta";
	system "/local/devel/ANNOTATION/ViralMGPipeline/branches/0.33/compute.pl -P 0116 -D $sqlitedb -C /local/devel/ANNOTATION/ViralMGPipeline/branches/0.33/viralPrecomputesCfg.db -c PRIAM -a \"proteinstmp as proteins\"";
	print "PRIAM updated\n";
}

our $vicsWSDL = "http://camapp:8180/compute-compute/ComputeWS?wsdl";	#development
my $job = vicsGetVmapAnnotation( getlogin || getpwuid($>) || 'ccbuild', $sqlitedb, $annodir, "0116" );
if ( !defined $job ) { die $errorMessage }
print "Re-running annotation module, VICS job id $$job{id}, please do not interrupt...\n";
$job = vicsWaitForJob( $$job{id} );
if ( !defined $job ) { die $errorMessage }
if ( $$job{status} ne "completed" ) {
	die "getVmapAnnotation failed: $$job{result_message}"		
}
print "annotation completed\n";

my $annofile = realpath( "$sqlitedb.annotation" );
$job = vicsCom2GO( getlogin || getpwuid($>) || 'ccbuild', $annofile, 1, 3, $annodir, "0116" );
if ( !defined $job ) { die $errorMessage }
print "Rr-running com2GO module, VICS job id $$job{id}, please do not interrupt...\n";
$job = vicsWaitForJob( $$job{id} );
if ( !defined $job ) { die $errorMessage }
if ( $$job{status} ne "completed" ) {
	die "com2GO failed: $$job{result_message}"		
}
print "com2GO completed\n";

exit(0);
