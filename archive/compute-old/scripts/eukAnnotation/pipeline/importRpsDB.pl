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

my $program = realpath($0);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::db;
use EAP::dataset;

# get command line options
my ( $db, $name, $description, $node_id, $node_owner ) = &initialize;

# open db
my $dbh = &connectSQLite( $db, my $autocommit = 1);
if ( !defined $dbh ) {
	die "\nCould not open db: " . $errorMessage . "\n";	
}

# add fasta
my $dataset = &addNode( $dbh, $name, $description, $node_id, $node_owner, "rps" );
$dbh->disconnect;
if ( !defined $dataset ) {
	die "\nCould not add import VICS rps db: " . $errorMessage . "\n";	
}
print "\nDataset imported:\n"
	. "Name: " . $$dataset{dataset_name} . "\n"
	. "Version: $$dataset{dataset_version}\n"
	. "Description: " . $description . "\n"
	. "VICS node: $$dataset{subject_node} ($$dataset{formatted_by}/$$dataset{date_formatted})\n"
	. "Number of Sequences: $$dataset{content_count}\n"
	. "Sequence Length: $$dataset{content_length}\n";

exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_N $opt_I $opt_d $opt_h $opt_o );
	&Getopts('D:N:I:d:ho:s:');

	if ( $opt_h ) {
		print
"
This script imports a Reverse Psi Blast DB from VICS to a config db for use as a SUBJECT dataset.

usage: ./importRpsDB.pl -D myConfig.db -I 1349486742876457131 -o jhoover -N CDS_RPS -d \"rpsformatted CDD database from NCBI, version 2.12\"

-D <config db file> path to config db
-I <VICS node ID> VICS \"node\" id for blast db
-N <dataset name> short, unique name for dataset
-d <description> optional, description of dataset
-o <node owner> optional, owner of node (default is system)
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the cofig db (-D).\n";
	} elsif ( ! -e $opt_D ) {
		die "\nSpecified config db \"$opt_D\" does not exist (-D).\n";
	}
	
	if ( !$opt_N ) {
		die "\nYou must specify a short, unique dataset name (-N).\n";
	}

	if ( !$opt_I ) {
		die "\nYou must specify the VICS rps db node id (-I).\n";
	}

	if ( !$opt_o ) {
		$opt_o = "system";
	}	
	
	return ( $opt_D, $opt_N, $opt_d, $opt_I, $opt_o );	
}

