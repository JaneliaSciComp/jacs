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
my $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::db;
use EAP::dataset;

# get command line options
my ( $db, $dataset_name ) = &initialize;

# open library
my $dbh = &connectSQLite( $db, my $autocommit = 1);
if ( !defined $dbh ) {
	die "\nCould not open db: " . $errorMessage . "\n";	
}

# add fasta
my $dataset = &getDatasetByName( $dbh, $dataset_name );
if ( !defined $dataset ) {
	die "\nCould not retrieve dataset \"" . $dataset_name . "\": " . $errorMessage . "\n";
}

if ( $$dataset{is_obsolete} ) {
	$dbh->disconnect;
	print "\nDataset is obsolete:\n";
} else {
	$dataset = &deleteDataset( $dbh, $dataset );
	$dbh->disconnect;
	if ( !defined $dataset ) {
		die "\nCould not delete dataset \"" . $dataset_name . "\": " . $errorMessage . "\n";	
	}
	print "\nDataset deleted:\n"
}

my ( $source, $usage, $vicsnode );
if ( $$dataset{content_source} eq "node" ) {
	$source = "imported from VICS." ;
} else {
	$source = $$dataset{content_path};
}
if ( $$dataset{subject_node} > 0 ) {
	$usage = "Subject";
	$vicsnode = "$$dataset{subject_node} ($$dataset{formatted_by}/$$dataset{date_formatted})." ;
} else {
	$usage = "Query";
	$vicsnode = "$$dataset{query_node} ($$dataset{uploaded_by}/$$dataset{date_uploaded})";
}

print "Name: " . $$dataset{dataset_name} . "\n"
	. "Version: $$dataset{dataset_version}\n"
	. "Description: " . $$dataset{description} . "\n"
	. "Sequence Type: $$dataset{seq_type}\n" 
	. "Usage: $usage\n"
	. "VICS Node: $vicsnode\n"
	. "Content Type: $$dataset{content_type}\n"
	. "Content Source: $source\n"
	. "Content Count: $$dataset{content_count}\n"
	. "Content Length: $$dataset{content_length}\n";

exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_N $opt_h );
	&Getopts('D:N:h');

	if ( $opt_h ) {
		print
"
This script deletes a dataset (marks it obsolete). 

usage: ./deleteDataset.pl -D myConfig.db -N CMR_ORFS

-D <db file> path to sqlite db
-N <dataset name> short, unique name for dataset
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the db (-D).\n";
	} elsif ( ! -e $opt_D ) {
		die "\nLibrary \"$opt_D\" does not exist (-D).\n";
	}

	if ( !$opt_N ) {
		die "\nYou must specify a short, unique dataset name (-N).\n";
	}
	
	return ( $opt_D, $opt_N );	
}

