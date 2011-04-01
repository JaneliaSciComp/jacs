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

use warnings;
use strict;
our $errorMessage;

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
my $myLib = dirname($program);

# get command line options
my ( $dbfile ) = &initialize;

# initialize database
system("sqlite3 " . $dbfile . "<" . $myLib . "/ddls/dataset.ddl");
system("sqlite3 " . $dbfile . "<" . $myLib . "/ddls/sequence.ddl");
system("sqlite3 " . $dbfile . "<" . $myLib . "/ddls/compute.ddl");
system("sqlite3 " . $dbfile . "<" . $myLib . "/ddls/egad.ddl");
if ( ! -e $dbfile ) {
	die "\nCould not initialize sqlite db.\n"
	}
chmod(0777,$dbfile);
print $dbfile . " created.\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_h );
	&Getopts('D:h');

	if ( $opt_h ) {
		print
"
This script creates a sqlite database for use by compute.pl.

usage: ./createComputeDB.pl -D myComputes.db

-D <db file> path for sqlite db to be created
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the path for the db file (-D).\n";
	} elsif ( -e $opt_D ) {
		die "\nDatabase \"$opt_D\" already exists.\n";
	}
	
	return ( $opt_D );	
}
