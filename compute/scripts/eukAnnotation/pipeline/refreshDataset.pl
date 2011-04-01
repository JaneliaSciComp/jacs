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
my ( $db, $name, $description, $file, $node_id, $node_owner ) = &initialize;

# open db
my $dbh = &connectSQLite( $db, my $autocommit = 1);
if ( !defined $dbh ) {
	die "\nCould not open db: " . $errorMessage . "\n";	
}

# fetch dataset
my $dataset = &getDatasetByName( $dbh, $name );
if ( !defined $dataset ) {
	die "\nCould not retrieve dataset: " . $errorMessage . "\n";	
}
my $version = $$dataset{dataset_version};

if ( !defined $description ) { $description = $$dataset{description} }
my $content_type = $$dataset{content_type};
	
my ( $usage, $source, $vicsnode );
if ( $$dataset{content_source} eq "node" ) {
	$source = "imported from VICS." ;
} else {
	$source = $$dataset{content_path};
}
if ( $$dataset{subject_node} > 0 ) {
	$usage = "Subject";
	$vicsnode = "$$dataset{subject_node} ($$dataset{formatted_by}/$$dataset{date_formatted})";
} else {
	$usage = "Query";
	$vicsnode = "$$dataset{query_node} ($$dataset{uploaded_by}/$$dataset{date_uploaded})";
}

# refresh fasta based dataset
if ( $$dataset{source_type} eq "file" ) {
	if ( defined $node_id ) {
		die "\nCannot refresh file-based dataset from VICS node. Use delete and add.\n"
	}
	if ( !defined $file ) { $file = $$dataset{content_path} }

	$dataset = &updateFile( $dbh, $name, $description, $file, $content_type );
	$dbh->disconnect;
	if ( !defined $dataset ) {
		die "\nCould not refresh dataset: " . $errorMessage . "\n";
	}
	
	my $content_change = "No";
	if ( $version != $$dataset{dataset_version} ) { $content_change = "Yes"}
	
	print "\nDataset Refreshed:\n"
	. "Name: " . $$dataset{dataset_name} . "\n"
	. "Version: $$dataset{dataset_version}\n"
	. "Description: " . $$dataset{description} . "\n"
	. "Usage: $usage\n"
	. "VICS node: " . $vicsnode . "\n"
	. "Content Type: $$dataset{content_type}\n"
	. "Content Source: $$dataset{content_path}\n"
	. "Content changed? $content_change\n" 
	. "Content Count: $$dataset{content_count}\n"
	. "Content Length: $$dataset{content_length}\n";

# refresh imported dataset
} else {
	if ( defined $file ) {
		die "\nCannot refresh imported VICS node from file. Use delete and add.\n"
	}
	
	if ( !defined $node_id ) { $node_id = $$dataset{subject_node} }
	if ( !defined $node_owner ) { $node_owner = $$dataset{formatted_by}	}
	
	$dataset = &updateNode( $dbh, $name, $description, $node_id, $node_owner, $content_type );
	$dbh->disconnect;
	if ( !defined $dataset ) {
		die "\nCould not refresh dataset: " . $errorMessage . "\n";
	}
	
	my $content_change = "No";
	if ( $version != $$dataset{dataset_version} ) { $content_change = "Yes" }

	print "\nDataset Refreshed:\n"
	. "Name: " . $$dataset{dataset_name} . "\n"
	. "Version: $$dataset{dataset_version}\n"
	. "Description: " . $$dataset{description} . "\n"
	. "Usage: $usage\n"
	. "VICS node: " . $vicsnode . "\n"
	. "Content Type: $$dataset{content_type}\n"
	. "Content changed? $content_change\n" 
	. "Content Count: $$dataset{content_count}\n"
	. "Content Length: $$dataset{content_length}\n";
}

exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_N $opt_d $opt_f $opt_h $opt_i $opt_o );
	&Getopts('D:N:d:f:hi:o:');

	if ( $opt_h ) {
		print
"
This script refreshes a dataset.  The new contents are compared to the current contents and if they are
different a new version of the dataset is created.

usage: ./../refreshDataset.pl -D myConfig.db -N \"NCBI NINT\"

-D <config db file> path to config DB
-N <dataset name> short, unique name for dataset
-d <description> optional, reset description of dataset
   (default is current description) 
-f <from file> optional, reset path to content file (file based datasets only),
   (default is current file)
-i <from VICs node id> optional, reset VICS node to be used for refresh (node based datasets only),
   (default is current node)
-o <node owner> optional, reset owner of VICS node (node based datasets only)
   (default is current owner)
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the config db (-D).\n";
	} elsif ( ! -e $opt_D ) {
		die "\nConfig db \"$opt_D\" does not exist (-D).\n";
	}
	
	if ( !$opt_N ) {
		die "\nYou must specify a short, unique dataset name (-N).\n";
	}

	if ( $opt_f && ! -e $opt_f ) {
		die "\nFile \"$opt_f\" does not exist (-f).\n";
	}

	return ( $opt_D, $opt_N, $opt_d, $opt_f, $opt_i, $opt_o );	
}

