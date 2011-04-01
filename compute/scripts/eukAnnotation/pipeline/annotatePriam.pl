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
my ( $eclist, $pnfile, $rulefile ) = &initialize;

# load ec definitions
my %ec = {};
open( EC, "<$eclist" );
while ( my $line = <EC> ) {
	$line =~ s/[\r\n]//g;
	$line =~ s/\\/\\\\/g;
	$line =~ s/\//\\\//g;
	$line =~ s/"/\\"/g;
	( my $ecnum ) = split( /\s/, $line );
	$ec{$ecnum} = $line;	
#print "ecl: $line\n";
}
close( EC );

# process list of ASN files
open( PRIAM, "<$pnfile" );
while ( my $filename = <PRIAM> ) {
#print "fil: $filename\n";
	$filename =~ s/[\r\n]//g;
#print "pnf: $filename\n";
	my $profile = basename( $filename );
	$profile =~ s/\.chk//;
#print "pro: $profile\n";
	my $ecnum = $profile;
	$ecnum =~ s/^\d*p//;
#print "ec#: $ecnum\n";
	my $defline = $profile;
	if ( exists $ec{$ecnum} ) {
		$defline .= " | " . $ec{$ecnum};
	} else {
		$defline .= " | " . $ecnum;
		print "profile $profile, unlisted ec#: $ecnum\n";
	}
	
	my @rules = split( /[\r\n]/, `grep "$profile" $rulefile` );
#print "gene rule=" . join( "\n", @rules ) . "\n";
	my $gene_rule;
	foreach my $rule ( @rules ) {
		$rule =~ s/^/ /;
		$rule =~ s/$/ /;
		if ( index( $rule, " $profile " ) >= 0 ) {
			$gene_rule = $rule;
			$gene_rule =~ s/^\s+//;
			$gene_rule =~ s/\s+$//;
			last;
		}
	}
	$defline .= " | $gene_rule";
	if ( $gene_rule ne $profile ) {
		print $defline . "\n";
	}
		
#print "def: $defline\n";
# add defline to ASN file
	my $cmd = "sed \"s/^\\(\\s*\\)\\(inst\\s*{\\)/\\1descr { title \\\"$defline\\\" } ,\\n\\1\\2/\" <$filename >$filename.upd";
#print "cmd: $cmd\n";
	system($cmd);
}
system( "sed \"s/\\.chk/\\.chk\\.upd/\" <$pnfile >$pnfile.upd" );
close( PRIAM );
exit(0);

#############################################################
#
# read and validate commmand line parameters
sub initialize {
	use vars qw( $opt_E $opt_P $opt_R $opt_h );
	&Getopts('E:P:R:h');

	if ( $opt_h ) {
		print
"
usage: ./annotatePriam.pl -E eclist -P pnfile -R rulesfile\"
";
		exit(0);
	}

	if ( !$opt_E ) {
		die "\nYou must specify the file containing the ec list (-E).\n";
	} elsif ( ! -e $opt_E ) {
		die "\nSpecified file \"$opt_E\" does not exist (-E).\n";
	}

	if ( !$opt_P ) {
		die "\nYou must specify the pn file pointing to the matrices (-P).\n";
	} elsif ( ! -e $opt_P ) {
		die "\nSpecified file \"$opt_P\" does not exist (-P).\n";
	}

	if ( !$opt_R ) {
		die "\nYou must specify the rules file (-R).\n";
	} elsif ( ! -e $opt_R ) {
		die "\nSpecified rules file \"$opt_R\" does not exist (-R).\n";
	}
	
	return ( $opt_E, $opt_P, $opt_R );	
}

