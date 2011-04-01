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
use DBI;
our $errorMessage;

require "getopts.pl";
use Cwd 'realpath';
use File::Basename;

my $program = realpath($0);
our $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::db;

my ( $db, $use_dev_pg, $grid_only, $includeObsolete ) = &initialize;
my $whereobs = "is_obsolete=0";
if ( $includeObsolete == 1 ) { $whereobs = "0=0" }

my $dbh = &connectSQLite( $db );
if ( ! defined $dbh ) { die $errorMessage . "\n" }

my $results = &querySQLArrayHash( $dbh,
	"select cast (job_id as text) as job_id from job where job_id>100000
	and $whereobs" );
if ( ! defined $results ) { die $errorMessage . "\n" }

my $tasklist = "";
foreach my $row ( @$results ) {
	if ( length($tasklist) > 0 ) { $tasklist .= "," }
	$tasklist .= $$row{job_id};
}

my $local;
my $overhd;
if ( ! $grid_only ) {
	$local = &querySQLArrayHash( $dbh,
		"select job_name, sum(0.75*(strftime('%s',date_completed)-strftime('%s',date_submitted))) as cpu_seconds, 1 as is_estimated
		from job where job_id<1000 and date_submitted<>'-' and date_completed is not null
		and $whereobs group by job_name" );
	if ( ! defined $local ) { die $errorMessage . "\n" }


# sort time ~75% CPU, insert time ~10% CPU
	if ( $includeObsolete ) {
		$overhd = &querySQLArrayHash( $dbh,
			"select 'Overhead' as job_name, round( ( 0.75*sum(num_results)/10785 + 0.10*sum(num_results)/463 ) / 900 + 0.5 ) * 900 as cpu_seconds, 1 as is_estimated from job where job_id>100000");
	} else {
		$overhd = &querySQLArrayHash( $dbh,
			"select 'Overhead' as job_name, round( ( 0.75*sum(num_results)/10785 + 0.10*sum(num_results)/463 ) / 900 + 0.5 ) * 900 as cpu_seconds, 1 as is_estimated from job where job_id>100000 and is_obsolete=0");
	}
	if ( ! defined $overhd ) { die $errorMessage . "\n" }
}

$dbh->disconnect;

if ( $use_dev_pg ) {
	$dbh = DBI->connect( "dbi:Pg:dbname=cameradb_dev;host=cadmium;port=5432",
		"camapp","camapp",{AutoCommit => 0, RaiseError => 1, PrintError => 0} );
} else {
	$dbh = DBI->connect( "dbi:Pg:dbname=compute_prod;host=thallium;port=5432",
		"csapp","csapp",{AutoCommit => 0, RaiseError => 1, PrintError => 0} );
}
if ( ! $dbh ) { die $DBI::errstr . "\n" }

$results = &querySQLArrayHash( $dbh,
	"select t.job_name, sum(a.cpu_time) as cpu_seconds
	from task t inner join accounting a on a.task_id=t.task_id
	where t.task_id in ($tasklist) group by t.job_name" );
if ( ! defined $results ) { die $errorMessage . "\n" }
$dbh->disconnect;

if ( defined $local ) { push ( @$results, @$local ) }
my @jobs = sort { $$a{job_name} cmp $$b{job_name} } @$results;
if ( defined $overhd ) { push @jobs, @$overhd }

my $totsec = 0;
my $totest = 0;
foreach my $row ( @jobs ) {
	$totsec += $$row{cpu_seconds};
	$$row{cpu_seconds} = int ( $$row{cpu_seconds} + 0.5 );
	my $hr = int( $$row{cpu_seconds} / 3600 );
	my $min = int( ( $$row{cpu_seconds} % 3600 ) / 60 );
	my $sec = $$row{cpu_seconds} % 60;
	if ( $$row{is_estimated} ) {
		$hr = "~" . $hr;
		$totest = 1;
	}	
	print &rpad($$row{job_name},20)
		. &lpad($hr,6) . ":" . &lpad($min,2,"0") . ":" . &lpad($sec,2,"0") . "\n";
}
$totsec = int ( $totsec + 0.5 );
my $hr = int( $totsec / 3600 );
if ( $totest ) { $hr = "~" . $hr }
my $min = int( ( $totsec % 3600 ) / 60 );
my $sec = $totsec % 60;


print &rpad("Total",20)
	. &lpad($hr,6) . ":" . &lpad($min,2,"0") . ":" . &lpad($sec,2,"0") . "\n";

exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_D $opt_h $opt_d $opt_g $opt_o );
	&Getopts('D:dgho');

	if ( $opt_h ) {
		print
"
This script reports CPU time used by computes.

usage: ./computeStats.pl -D ntsm07.db

-D <db file> path to compute db
-d use development version of VICS
-g grid jobs only
-o include stats for obsolete results
-h help (this message)
";
		exit(0);
	}

	if ( !$opt_D ) {
		die "\nYou must specify the annotation db file (-D)."
	} elsif ( ! -e $opt_D) {
		die "\nThe specified annotation db file does not exist: \"-D " . $opt_D . "\".";
	}

	my $incobs = 0;
	if ( $opt_o ) { $incobs = 1 }
	
	return ( $opt_D, $opt_d, $opt_g, $incobs );	
}
