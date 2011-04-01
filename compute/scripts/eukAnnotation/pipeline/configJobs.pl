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

use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::dataset;
use EAP::compute;

#-----------------------------------------------------------------------
# get options from command line
my ( $configdb, $jobfile ) = &initialize;

my $dbh = &connectSQLite( $configdb, my $autocommit = 1 );
if ( !defined $dbh ) { die "\nCould not open config db \"" . $configdb . "\": " . $errorMessage . "\n" }

$dbh->begin_work;

if ( !defined &executeSQL( $dbh, "delete from job_config" ) ) { die "\nCould not clear old jobs: " . $errorMessage . "\n" }

if ( ! open( JOBS, "<$jobfile" ) ) {die "\nCould not open job file \"" . $jobfile . "\".\n" } 
my @jobs;
while ( my $job = <JOBS> ) {
	$job =~ s/[\r\n]//g;
	if ( $job =~ /^\#/ || $job=~ /^\s*$/ ) {
	} else {
		my @row = split( /\t/, $job );

		my @options = split( /\~\~/, $row[2] );
		@options = sort @options;
		$row[2] = join( "~~", @options );
		
		while ( scalar @row < 5 ) {	push( @row, undef ) }; 
		my $addjob = &executeSQL( $dbh,
			"insert into job_config(job_name,program_name,program_options,subject_db_name,query_db_name) values(?,?,?,?,?)",
			@row);
		if ( !defined $addjob ) { die "\nCould not add job \"" . $job . "\": " . $errorMessage . "\n" }
		push( @jobs, \@row );
	}
}
close(JOBS);

$dbh->commit;

print "\n" . &rpad( "Job Name", 20) . "  " . &rpad( "Program Name", 8) . "  " . &rpad( "Program Options", 25 ) . "  " . &rpad( "Subject DB", 25 ) . "  Query DB\n";
foreach my $job ( @jobs ) {
	print &rpad( $$job[0], 20) . "  " . &rpad( $$job[1], 8) . "  " . &rpad( $$job[2], 25 ) . "  " . &rpad( $$job[3], 25 ) . "  " . $$job[4] . "\n";
}

print "\nJobs configured.\n";
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_C $opt_J $opt_h );
	&Getopts('C:J:h');

	if ( $opt_h ) {
		print
"
Loads the contents of a tab-delimited job specification file into a configuration db.
Previously existing job specifications are deleted before the file is loaded.

usage: ./configJobs.pl -C viralConfig.db -J viral_precomputes.cfg

-C <config db file> path to sqlite db to load hjob configuartions
-J <job specification file> path to tab-delimited file defining jobs
";
		exit(0);
	}
	
	if ( !$opt_J ) {
		die "\nYou must specify the job specification file (-J)."
	} elsif ( ! -e $opt_J ) {
		die "\nThe indicated job specification file does not exist: \"-J " . $opt_J ."\".";
	}

	if ( !$opt_C ) {
		die "\nYou must specify the configuration db file (-C)."
	} elsif ( ! -e $opt_C ) {
		die "\nThe specified configuration db file does not exist: \"-C " . $opt_C . "\".";
	}
	
	return ( $opt_C, $opt_J );	
}
