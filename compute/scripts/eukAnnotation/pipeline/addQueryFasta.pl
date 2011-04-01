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
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline/');
use EAP::db;
use EAP::dataset;

# get command line options
my ( $db, $fasta, $name, $description, $attrlist ) = &initialize;
my @attrs = split( /,/, $attrlist );
  
# open db
my $dbh = &connectSQLite( $db, my $autocommit = 1);
if ( !defined $dbh ) {
	die "\nCould not open db: " . $errorMessage . "\n";	
}

# add fasta
my $dataset = &addFile( $dbh, $name, $description, $fasta, 1, 0, "seq", $attrlist );
$dbh->disconnect;
if ( !defined $dataset ) {
	die "\nCould not add query dataset: " . $errorMessage . "\n";	
}
	
print "\nDataset Added:\n"
	. "Name: " . $$dataset{dataset_name} . "\n"
	. "Version: $$dataset{dataset_version}\n"
	. "Description: " . $description . "\n"
	. "Content Source: " . $$dataset{content_path} . "\n"
	. "VICS Node: $$dataset{query_node}\n"
	. "Sequence Attributes: $$dataset{seq_attr_list}\n"
	. "Number of Sequences: $$dataset{content_count}\n"
	. "Sequence Length: $$dataset{content_length}\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_F $opt_D $opt_N $opt_d $opt_h $opt_a );
	&Getopts('D:F:N:d:ha:');

	if ( $opt_h ) {
		print
"
This script adds a fasta file to a compute db for use as a QUERY dataset. 

usage: ./addQueryFasta.pl -D /usr/local/projects/jhoover/myResults.db -F /usr/local/projects/jhoover/mySeqs.fasta -N \"Dataset Name\" -d \"Dataset description.\" -a \"READ_ID,ORF_ID,SAMPLE_NAME\"

-D <compute db file> path to compute db
-F <fasta file> path to fasta file
-N <dataset name> short, unique name for dataset
-d <description> optional, description of dataset
-a <attribute list> comma separated list of defline tags to be parsed as sequence attributes
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the compute db (-D).\n";
	} elsif ( ! -e $opt_D ) {
		die "\nCompue db \"$opt_D\" does not exist (-D).\n";
	}

	if ( !$opt_F ) {
		die "\nYou must specify the fasta file (-F).\n";
	} elsif ( ! -e $opt_F ) {
		die "\nFasta file \"$opt_F\" does not exist (-F).\n";
	}
	
	if ( !$opt_N ) {
		die "\nYou must specify a short, unique dataset name (-N).\n";
	}
	
	return ( $opt_D, $opt_F, $opt_N, $opt_d, $opt_a );	
}

