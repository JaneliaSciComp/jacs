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

test_runHmmpfam.pl - test the runHmmpfam web-service call.

=head1 SYNOPSIS

    USAGE: test_runHmmpfam.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid code needed to submit
 --workSessionId            Work session ID, if desired
 --jobName                  Name to refer to the job as
 --subjectDBIdentifier      VICS id for the subject database
 --queryFastaFileNodeId     VICS id for the input fasta

=head2 program parameters

 --maxBestDomainAligns
 --evalueCutoff
 --tbitThreshold
 --zModelNumber
 --useHmmAccessions
 --cutGa
 --cutNc
 --cutTc
 --domE
 --domT
 --null2

=head1  DESCRIPTION

This script tests the runHmmpfam web service.

=head1  INPUT

There is no required input for this script.

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
my $jobName = 'TestHmmpfam';
my $subjectDBIdentifier = '1365496776676606238';
my $queryFastaFileNodeId = '1438285098964224171';
my $maxBestDomainAligns = 0;
my $evalueCutoff = 10.0;
my $tbitThreshold = -1000000.0;
my $zModelNumber = 59021;
my $useHmmAccessions = 0;
my $cutGa = 0;
my $cutNc = 0;
my $cutTc = 0;
my $domE  = "1.0E9";
my $domT  = -1000000.0;
my $null2 = 0;

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "subjectDBIdentifier=s"     =>  \$subjectDBIdentifier,
                        "queryFastaFileNodeId=s"    =>  \$queryFastaFileNodeId,
                        "maxBestDomainAligns=i"     =>  \$maxBestDomainAligns,
                        "evalueCutoff=i"            =>  \$evalueCutoff,
                        "tbitThreshold=i"           =>  \$tbitThreshold,
                        "zModelNumber=i"            =>  \$zModelNumber,
                        "useHmmAccessions=i"        =>  \$useHmmAccessions,
                        "cutGa=i"                   =>  \$cutGa,
                        "cutNc=i"                   =>  \$cutNc,
                        "cutTc=i"                   =>  \$cutTc,
                        "domE=s"                    =>  \$domE,
                        "domT=i"                    =>  \$domT,
                        "null2=i"                   =>  \$null2,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runHmmpfam';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $subjectDBIdentifier,   # subjectDBIdentifier
                $queryFastaFileNodeId,  # queryFastaFileNodeId
                $maxBestDomainAligns,   # maxBestDomainAligns
                $evalueCutoff,          # evalueCutoff
                $tbitThreshold,         # tbitThreshold
                $zModelNumber,          # zModelNumber
                $useHmmAccessions,      # useHmmAccessions
                $cutGa,                 # cutGa
                $cutNc,                 # cutNc
                $cutTc,                 # cutTc
                $domE,                  # domE
                $domT,                  # domT
                $null2,                 # null2
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runHmmpfam(@options);

