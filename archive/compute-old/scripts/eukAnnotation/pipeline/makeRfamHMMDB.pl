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
use Getopt::Std; 

use Cwd 'realpath';
use File::Basename;
my $program = realpath($0);
my $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::db;
our $errorMessage;

my $dirRfamHMMRepository = "/usr/local/db/RFAM/HMMs";

#################################################
# get command line options

my $usage = " 
	This script reads the egad..rfam table and constructs an HmmPfam
	database based on the specified flag column (prok, euk, or vir).
	
    USAGE: prokMakeRfamHMM.pl -R <Rfam database> -C <flag column> 

    OPTIONAL:
	-o overwrite existing database
	-h help (this message)
";

my %args;
&getopts('R:C:o',\%args);
if ($args{h}) {
	print $usage;
	exit(0);
}

my $rfamDB;
if ( !defined $args{R} ) {
	die "You must specify the path for the RFam database to be constructed. (-R)";
} else {
	$rfamDB = $args{R};
}

my $flagColumn;
if ( !defined $args{C} ) {
	die "You must specify the column to be used to flag inclusion (prok, euk, or vir). (-C)";
} else {
	$flagColumn = $args{C};
}

my $overwrite = 0;
if ( defined $args{o} ) { $overwrite = 1 }

#################################################
# does Rfam database already exist?
# can we overwrite it?
if ( !$overwrite && -e $rfamDB ) {
	die "RFam database \"" . $rfamDB . "\" already exists. Specify -o to overwrite it.";
} else {
	unlink $rfamDB;
}

#################################################
# read list of prok Rfams from sybase
print "Fetching list of RFams from SYBASE.\n";
my $dbh = &connectSybase("egad","access","access");
if ( !defined $dbh ) {
	die "Could not connect to SYBASE: " . $errorMessage;
}
my $rfams = &querySQL($dbh,"select accession from egad..rfam where $flagColumn=1 and iscurrent=1 order by 1");
if ( !defined $rfams ) {
	die "Could niot fetch list of rfams from SYBASE: " . $errorMessage;
}
$dbh->disconnect;

#################################################
# append selected rfams into a single HMM database
print "Building HMM database: $rfamDB\n";
if ( ! open(DB,">$rfamDB") ) {
	die "Could not open Rfam database \"" . $rfamDB . "\" for output.";
}
my $numrfam = 0;
my $numhmmcopied = 0;
my $numhmmskipped = 0;
foreach my $hmm ( @$rfams ) {
	foreach my $strand ( "fwd" , "rev" ) {
		my $hmmfile = $dirRfamHMMRepository . "/" . $$hmm[0] . "_" . $strand . ".HMM";
		if ( ! open(HMM,"<$hmmfile") ) {
			print "  WARNING: $hmmfile, open error: skipping HMM.\n";
			$numhmmskipped++;
		} else {
			print "  ADDING:  $hmmfile\n";
			while ( my $line = <HMM> ) {
				print DB $line; 
			}
			close(HMM);
			$numhmmcopied++;
		}
	}
} 
close(DB);
print "\nHMM copied:  $numhmmcopied\n";
print "HMM skipped: $numhmmskipped\n";
print "Database build completed.\n";
exit(0);
