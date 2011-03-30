#!/usr/local/bin/perl -w

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
$|++;

use SOAP::Lite;
use Getopt::Long;

=head1 NAME

test_runFasta2Bsml.pl - test the runFasta2Bsml web-service call.

=head1 SYNOPSIS

    USAGE: test_runFasta2Bsml.pl

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid submmission code [08020]
 --workSessionId            Work session id, if desired
 --jobName                  Name that refers to this job
 --fastaInputNodeId         VICS id for the input fasta
 
=head2 program parameters
 
=back

=head1  DESCRIPTION

This script tests the runFasta2Bsml web service. This script is used to convert fasta to BSML.  

=head1  INPUT

	B<--fasta_input> Input files or folders.  Can be a comma-separated list of mixed input types.

    B<--fasta_list>Text file that is a list of input files and/or folders.

    B<--format> Format.  'multi' (default) writes all sequences to a multi-entry bsml file, and 'single' writes each sequence in a separate file named like $id.bsml

	B<--class,-c> Sets the class attribute of each Sequence element created.  Default = assembly.

=head1  OUTPUT

When succesfully submitted, this web service returns the following:
Job Id: 1436500110895745195

=head1  CONTACT

    Erin Beck
    ebeck@jcvi.org

=cut

my $port      = 81;
my $server    = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url  = '';

my $username      = getlogin;
my $project       = '08100';
my $workSessionId = '';
my $jobName       = 'TestFasta2Bsml';

my $fasta_input = '';
my $fasta_list  = '';
my $format      = '';
my $class       = '';
my $organism;
my $genus   = '';
my $species = '';

my $result = GetOptions(
	"port|p=i"    => \$port,
	"server|s=s"  => \$server,
	"wsdl_name=s" => \$wsdl_name,
	"wsdl_path=s" => \$wsdl_path,
	"wsdl_url=s"  => \$wsdl_url,

	"username=s"      => \$username,
	"project=s"       => \$project,
	"workSessionId=s" => \$workSessionId,
	"jobName=s"       => \$jobName,

	"fasta_input=s" => \$fasta_input,
	"fasta_list=s"  => \$fasta_list,
	"format=s"      => \$format,
	"class=s"       => \$class,
	"organism"      => \$organism,
	"genus=s"       => \$genus,
	"species=s"     => \$species
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runFasta2Bsml';
my @options = (
	$username,         # username
	'',                # token
	$project,          # project
	$workSessionId,    # workSessionId
	$jobName,          # jobName

	$fasta_input,      #fasta_input
	$fasta_list,       #fasta_list
	$format,           #format
	$class,            #class
	$organism,         #flag for parsing organism name from file name
	$genus,            #genus
	$species           #species
);

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite->service($wsdl_url)->runFasta2Bsml(@options);

