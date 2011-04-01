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

use DBI;
use strict;
use Getopt::Std; 
use File::Copy;
use Fcntl ':flock'; # import LOCK_* constants
 
use Cwd 'realpath';
use File::Basename;
my $program = realpath($0);
my $egadDir = dirname($program);
use lib ('/usr/local/devel/ANNOTATION/EAP/pipeline');
use EAP::db;

# get arguments
my $usage = " 
	USAGE: refreshEgad.pl -D myComputes.db
	-D <sqlite db> refresh the specified db file

	OPTIONAL:
	-h help (this message)
	-w <working directory> default is current directory
";

my %args;
&getopts('w:D:h',\%args);
if ( $args{h}) {
	print "\n" . $usage . "\n";
	exit(0);
}

my $workingdir = ".";
if ( $args{w}) {
	$workingdir = $args{w};
}
if ( substr( $workingdir, length($workingdir)-1, 1 ) eq "/" ) {
	$workingdir = substr( $workingdir, 0, length($workingdir)-1 );
}
if ( ! -e $workingdir ) {
   	die "\nDesignated working directory \"" . $workingdir . "\" does not exists.\n";
}

my $egadDB;
if ( $args{D} ) {
	$egadDB = $args{D};
} else {
	die "\nNo sqlite db specified.\n"; 
}

# connect to sqlite db
my $sqlh = connectSQLite( $egadDB, my $autocommit = 1 );
if ( !defined $sqlh ) { die "\nsqlite connect1: " . $DBI::errstr . "\n" };

# clear old data from sqlite db
my $delete = &executeSQL( $sqlh, "delete from hmm2" );
$delete = &executeSQL( $sqlh, "delete from rfam" );

# connect to sybase
print "Connecting to Sybase.\n";
my $sybh = connectSybase( "egad", "access", "access" );
if ( !defined $sybh ) { die "\nsybase connect: " . $DBI::errstr . "\n" };

# copy rfam data
print "Copying rfam data from sybase to sqlite.\n";
my $data =
	$sybh->selectall_arrayref(
		"select id,accession,feat_type,feat_class,com_name,gene_sym,window_size,noise_cutoff,gathering_thresh,trusted_cutoff,euk,prok,vir,iscurrent "
			. "from rfam where iscurrent=1");
if ( !defined $data ) { die "\nselect rfam: " . $DBI::errstr . "\n" }

foreach my $row ( @$data ) {
	my $insert =
		$sqlh->do(
			"insert into rfam(id,accession,feat_type,feat_class,com_name,gene_sym,window_size,noise_cutoff,gathering_thresh,trusted_cutoff,euk,prok,vir,iscurrent)"
				."values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
			undef,@$row);
	if ( !defined $insert ) { die "\ninsert rfam: " . $DBI::errstr . "\n" }
}

# copy rfam data
print "Copying hmm2 data from sybase to sqlite.\n";
$data =
	$sybh->selectall_arrayref(
		"select id,hmm_acc,hmm_len,iso_type,hmm_com_name,gene_sym,ec_num,noise_cutoff,gathering_cutoff,trusted_cutoff,trusted_cutoff2,is_current "
			. "from hmm2 where is_current=1");
if ( !defined $data ) { die "\nselect rfam: " . $DBI::errstr . "\n" }

$sqlh->begin_work;
foreach my $row ( @$data ) {
	my $insert =
		$sqlh->do(
			"insert into hmm2(id,hmm_acc,hmm_len,iso_type,hmm_com_name,gene_sym,ec_num,noise_cutoff,gathering_cutoff,trusted_cutoff,trusted_cutoff2,is_current)"
				."values(?,?,?,?,?,?,?,?,?,?,?,?)",
			undef,@$row);
	if ( !defined $insert ) { die "\ninsert hmm2: " . $DBI::errstr . "\n" }
}

# commit and disconnect
$sqlh->commit;
$sqlh->disconnect;
$sybh->disconnect;
print "Data commited.\n";

# done
print "Download Completed.\n";
exit(0);
