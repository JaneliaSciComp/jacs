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
our $myLib = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::generic;
use EAP::dataset;

print "You are logged in as " . getlogin . "\n";
if ( getlogin ne "ccbuild" ) { die "\nYou must be logged in as ccbuild to use this program.\n" }

#-----------------------------------------------------------------------
# get options from command line
my ( $annotationDB ) = &initialize;
$annotationDB = realpath($annotationDB);

#-----------------------------------------------------------------------
# find VICS tasks and nodes to be deleted
my $annodbh = &connectSQLite( $annotationDB, my $autocommit = 1 );
if ( !defined $annodbh ) {
	die "\nCould not open annotation db: " . $errorMessage . "\n";
}
my $vicsdbh = DBI->connect( "dbi:Pg:dbname=compute_prod;host=thallium;port=5432",
	"csapp","csapp",{AutoCommit => 1, RaiseError => 1, PrintError => 0} );
$vicsdbh->begin_work;

#-----------------------------------------------------------------------
# find tasks
my $tasklist;
{
	my $tasks = &querySQLArrayHash( $annodbh,
		"select job_id from compute where job_id>1000000" );
	if ( !defined $tasks ) {
		die "\nCould not find tasks: " . $errorMessage . "\n";
	}

	foreach my $task ( @$tasks ) {
		if ( !defined $tasklist ) { $tasklist = $$task{job_id} }
		else { $tasklist .= "," . $$task{job_id} }
	}
}

# find nodes
my $qrylist;
{
	my $querynodes = &querySQLArrayHash( $annodbh,
		"select query_node, dataset_name from dataset_version where query_node>0" );
	if ( !defined $querynodes ) {
		die "\nCould not find query nodes: " . $errorMessage . "\n";
	}
	foreach my $querynode ( @$querynodes ) {
		if ( !defined $qrylist ) { $qrylist = $$querynode{query_node} }
		else { $qrylist .= "," . $$querynode{query_node} }
		
		my $queryset = &getDatasetByName( $annodbh, $$querynode{dataset_name} );
		if ( $$queryset{is_obsolete} == 0 ) { &deleteDataset( $annodbh, $queryset ) }
	}
}

my $sql;
if ( defined $tasklist ) {
	$sql = "select node_id, node_owner, subclass from node where task_id in (select task_id from get_task_tree('$tasklist'))";
}
if ( defined $qrylist ) {
	if ( ! defined $sql ) { $sql = "select node_id, node_owner, subclass from node where node_id in ($qrylist)" }
	else { $sql .= " union select node_id, node_owner, subclass from node where node_id in ($qrylist)" }
}
my $nodes;
if ( defined $sql ) {
	$nodes = querySQLArrayHash( $vicsdbh, $sql );
	if ( !defined $nodes ) {
		die "\nCould not find nodes: " . $errorMessage . "\n";
	}
}

my $nodelist;
foreach my $node ( @$nodes ) {
	if ( !defined $nodelist ) { $nodelist = $$node{node_id} }
	else { $nodelist .= "," . $$node{node_id} }
}

#-----------------------------------------------------------------------
# remove tasks and nodes from VICS database
if ( defined $nodelist ) {
	print "delete from node where node_id in ($nodelist)\n";
	$vicsdbh->do( "delete from node where node_id in ($nodelist)" );
} else {
	print "No nodes found\n";
}
if ( defined $tasklist ) {
	print "delete from task_event where task_id in (select task_id from get_task_tree('$tasklist') )\n";
	$vicsdbh->do( "delete from task_event where task_id in (select task_id from get_task_tree('$tasklist') )" );
	print "delete from task_message where task_id in (select task_id from get_task_tree('$tasklist') )\n";
	$vicsdbh->do( "delete from task_message where task_id in (select task_id from get_task_tree('$tasklist') )" );
	print "delete from task_parameter where task_id in (select task_id from get_task_tree('$tasklist') )\n";
	$vicsdbh->do( "delete from task_parameter where task_id in (select task_id from get_task_tree('$tasklist') )" );
	print "delete from task_input_node where task_id in (select task_id from get_task_tree('$tasklist') )\n";
	$vicsdbh->do( "delete from task_input_node where task_id in (select task_id from get_task_tree('$tasklist') )" );
	print "delete from task where task_id in (select task_id from get_task_tree('$tasklist') )\n";
	$vicsdbh->do( "delete from task where task_id in (select task_id from get_task_tree('$tasklist') )" );
#	$vicsdbh->do( "delete from task where task_id in
#					(select task_id from get_task_tree('$tasklist') where task_id>0
#						except select parent_id from get_task_tree('$tasklist')) where parent_id>0" );
} else {
	print "No tasks found.\n";
}
$vicsdbh->commit;

#-----------------------------------------------------------------------
# remove node directories from disk
foreach my $node ( @$nodes ) {
	my $dir = "Xruntime-shared/filestore/$$node{node_owner}/$$node{node_id}";
	if ( -e $dir ) {
		print "rm -r -f $dir\n";
		system("rm -r -f $dir")
	} else {
		$dir = "Xruntime-shared/filestore/$$node{node_owner}/*/$$node{node_id}";
		print "rm -r -f $dir\n";
		system("rm -r -f $dir")
	}
}

#-----------------------------------------------------------------------
# clean-up and exit
$annodbh->disconnect;
$vicsdbh->disconnect;
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
This script displays the computations in a compute db.

usage: ./deleteVICSResults.pl -D ntsm07.db

-D <annotation db file> delete VICS raw results from disk
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

