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
my ( $db, $extendedresults ) = &initialize;

# open db
my $dbh = &connectSQLite( $db, my $autocommit = 1);
if ( !defined $dbh ) {
	die "\nCould not open db: " . $errorMessage . "\n";	
}

my $datasets;
if ( $extendedresults ) {
	$datasets = &querySQLArrayHash( $dbh,
		"select dataset_name as \"Name\", dataset_version as \"V\", description as \"Description\","
			. "case when subject_node>0 then 'S' else 'Q' end as \"S/Q\","
			. "content_type as \"Type\", content_count as \"#Items\", content_length as \"Length\","
			. "source_type as \"Src\", case when is_obsolete=0 then 'N' else 'Y' end as \"Obs?\","
			. "date_released as \"Released\", date_retired as \"Retired\" "
			. "from dataset_history where version_id>0 order by dataset_name asc, dataset_version desc");
} else {
	$datasets = &querySQLArrayHash( $dbh,
		"select dataset_name as \"Name\", dataset_version as \"V\", description as \"Description\","
			. "case when subject_node>0 then 'S' else 'Q' end as \"S/Q\","
			. "content_type as \"Type\", content_count as \"#Items\", content_length as \"Length\","
			. "source_type as \"Src\",case when is_obsolete=0 then 'N' else 'Y' end as \"Obs?\","
			. "date_released as \"Released\", date_retired as \"Retired\" "
			. "from dataset_detail where version_id>0 and is_obsolete=0 order by dataset_name asc");
}
$dbh->disconnect;
if ( !defined $datasets ) {
	die "\nCould not query db: " . $errorMessage . "\n";
} elsif ( scalar @$datasets == 0 ) {
	print "\nNo datasets found.\n";
}

print "\n"
	. &rpad("Name",20) . "  "
	. &lpad("V",2) . "  "
	. &rpad("Description",40) . " "
	. &cpad("S/Q",3) . "  "
	. &rpad("Type",4) . "  "
	. &lpad("#Items",9) . "  "
	. &lpad("Length",12) . "  "
	. &rpad("Src",4) . "  "
	. &cpad("Obs?",4) . "  "
	. &cpad("Released",19) . "  "
	. &cpad("Retired",19) . "\n";

foreach my $row ( @$datasets ) {
print &rpad($$row{"Name"},20) . "  "
	. &lpad($$row{"V"},2) . "  "
	. &rpad($$row{"Description"},40) . " "
	. &cpad($$row{"S/Q"},3) . "  "
	. &rpad($$row{"Type"},4) . "  "
	. &lpad($$row{"#Items"},9) . "  "
	. &lpad($$row{"Length"},12) . "  "
	. &rpad($$row{"Src"},4) . "  "
	. &cpad($$row{"Obs?"},4) . "  "
	. &cpad($$row{"Released"},19) . "  "
	. &cpad($$row{"Retired"},19) . "\n";
}	
print "\n" . scalar @$datasets . " datasets.\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_h $opt_x );
	&Getopts('D:hx');

	if ( $opt_h ) {
		print
"
This script lists the contents of a compute module db. 

usage: ./listDatasets.pl -D /usr/local/projects/jhoover/myLibrary.db

-D <db file> path to db
-x <extended output> optional, include old versions and obsolete datasets
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the db (-D).\n";
	} elsif ( ! -e $opt_D ) {
		die "\nDatabase \"$opt_D\" does not exist (-D).\n";
	}

	if ( $opt_x ) {
		$opt_x = 1;
	} else {
		$opt_x = 0;
	}
	
	return ( $opt_D, $opt_x );	
}

