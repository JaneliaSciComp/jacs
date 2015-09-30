#!/usr/local/perl -w

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

test_runGenezilla.pl - test the runGenezilla web-service call.

=head1 SYNOPSIS

    USAGE: test_runGenezilla.pl OPTION LIST HERE

=head1 OPTIONS

NOTE:  This script deviates slightly from the typical connection parameter options.
Genezilla has an -s option already, so the alias for --server is -S as opposed to
the commonly seen -s in this test suite.

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -S   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid code needed to submit
 --workSessionId            Work session ID, if desired
 --jobName                  Name to refer to the job as
 --fastaInputNodeId         VICS id for the input fasta

=head2 program parameters

 --isoInputNodeId
 --cpgIslandPredictionFile
 --isochorePredictionFile
 --ignoreShortFasta
 
=head1  DESCRIPTION

 Invokes Genezilla via VICS.

=head1  INPUT

There is no required input for this script.  However, it is assumed that an
appropriate nucleotide fasta and iso file have been uploaded (provided to this script via the
--fastaInputNodeId and --isoInputNodeId parameter)

=head1  OUTPUT

When succesfully submitted, this web service returns the following:
Job Id: 1436500110895745195

=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut

my $port = 81;
my $server = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url = '';

my $username = getlogin;
my $project = '08020';
my $workSessionId = '';
my $jobName = 'TestGenezilla';
my $inputFastaNodeId = '1438285098964224171';

my $isoInputNodeId          = '';
my $cpgIslandPredictionFile       = '';
my $isochorePredictionFile       = '';
my $ignoreShortFasta       = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|S=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "inputFastaNodeId=s"        =>  \$inputFastaNodeId,

                        "isoInputNodeId=s"		    => \$isoInputNodeId,
                        "cpgIslandPredictionFile=s"		=> \$cpgIslandPredictionFile,
                        "isochorePredictionFile=s"		=> \$isochorePredictionFile,
                        "ignoreShortFasta=s"		=> \$ignoreShortFasta,

                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runGenezilla';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $inputFastaNodeId,      # inputFastaNodeId

				$isoInputNodeId,		    # isoInputNodeId
				$cpgIslandPredictionFile,   # cpgIslandPredictionFile
				$isochorePredictionFile,	# isochorePredictionFile
				$ignoreShortFasta,		    # ignoreShortFasta

                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runGenezilla(@options);

