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
use DBI;

my $program = realpath($0);
our $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::dataset;
use EAP::compute;

#-----------------------------------------------------------------------
# get options from command line
my ( $annotationDB ) = &initialize;

my $annodbh = &connectSQLite( $annotationDB, my $autocommit = 1 );
if ( !defined $annodbh ) {
	die "\nCould not open annotation db: " . $errorMessage . "\n";
}

$annodbh->func( "eval_to_pval", 1, sub { my $eval = shift; return 1. - 2.718281828**(-$eval) }, "create_function" );

if ( !defined &executeSQL( $annodbh, "update btab set pvalue = eval_to_pval(evalue)" ) ) {
	die "\nUpdate failed: " . $errorMessage . "\n";
}
$annodbh->disconnect;;
print "Pvalue updated.\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_C $opt_P $opt_a $opt_h $opt_r );
	&Getopts('D:C:P:r:a:h');

	if ( $opt_h ) {
		print
"
This script calculates Pvalue from Evalue as 1 - e^(-Evalue) and update the BTAB table.

usage: ./fixPvalue.pl -D ntsm07.db

-D <annotation db file> path to sqlite db contains query sets and compute results
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the annotation db file (-D)."
	} elsif ( ! -e $opt_D) {
		die "\nThe specified annotation db file does not exist: \"-D " . $opt_D . "\".";
	}
	
	return ( $opt_D );	
}


