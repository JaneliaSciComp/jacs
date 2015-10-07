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
my $myLib = dirname($program).'/ddls';
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::vics;
use EAP::dataset;

# get command line options
my ( $library ) = &initialize;

# initialize database
system("sqlite3 " . $library . "<" . $myLib . "/dataset.ddl");
system("sqlite3 " . $library . "<" . $myLib . "/sequence.ddl");
system("sqlite3 " . $library . "<" . $myLib . "/job_config.ddl");
if ( ! -e $library ) {
	die "\nCould not initialize sqlite db for library.\n"
}
chmod(0777,$library);
print $library . " created\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_C $opt_h );
	&Getopts('C:h');

	if ( $opt_h ) {
		print
"
This script creates a sqlite database to serve as a dataset library.
 
usage: ./createConfigDB.pl -C /usr/local/projects/jhoover/myConfig.db

-C <db file> path for sqlite db to be created to hold configuration
";
		exit(0);
	}

	if ( !$opt_C ) {
		die "\nYou must specify the path for the db file (-C).\n";
	} elsif ( -e $opt_C ) {
		die "\nDatabase " . $opt_C . " already exists.\n";
	}
	

	return ( $opt_C );	
}

