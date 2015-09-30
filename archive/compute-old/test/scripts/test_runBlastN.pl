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

test_runBlastN.pl - test the runBlastN web-service call.

=head1 SYNOPSIS

    USAGE: test_runBlastN.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters:

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2  VICS parameters:

 --username         User to run this as.  [current user]
 --project          Project code for the grid [08020]
 --workSessionId    Work session in which to store results [null]
 --jobName          Name to identify the job [TestBlastN]
 --subjectDBIdentifier  VICS db identifier for the subject db [1436484266337765547]
 --queryFastaFileNodeId VICS identifier for the query fasta [1436478961931520171]

=head2 Program paramaters and default values:

For more information regarding these options read http://www.ncbi.nlm.nih.gov/staff/tao/URLAPI/blastall.html#3.2

 --databaseAlignmentsPerQuery = 10
 --filter = 'T'
 --eValueExponent = -5
 --lowercaseFiltering = 0
 --believeDefline = 0
 --databaseSize = 0
 --gapExtendCost = -1
 --gappedAlignment = 1
 --hitExtensionThreshold = 1
 --matrix = 'BLOSUM62'
 --multiHitWindowSize = 0
 --searchStrand = 'both'
 --ungappedExtensionDropoff = 0
 --bestHitsToKeep = 0
 --finalGappedDropoff = 0
 --gapOpenCost = -1
 --gappedAlignmentDropoff = 0
 --matchReward = 0
 --mismatchPenalty = 0
 --searchSize = 0
 --showGIs = 0
 --wordsize = 0
 --formatTypesCsv = ''

=head1  DESCRIPTION

This script tests the runBlastN web service, invoking with the supplied arguments.

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
my $jobName = 'TestBlastN';
my $subjectDBIdentifier = '1436484266337765547';
my $queryFastaFileNodeId = '1436478961931520171';
my $databaseAlignmentsPerQuery = 10;
my $filter = 'T';
my $eValueExponent = -5;
my $lowercaseFiltering = 0;
my $believeDefline = 0;
my $databaseSize = 0;
my $gapExtendCost = -1;
my $gappedAlignment = 1;
my $hitExtensionThreshold = 1;
my $matrix = 'BLOSUM62';
my $multiHitWindowSize = 0;
my $searchStrand = 'both';
my $ungappedExtensionDropoff = 0;
my $bestHitsToKeep = 0;
my $finalGappedDropoff = 0;
my $gapOpenCost = -1;
my $gappedAlignmentDropoff = 0;
my $matchReward = 0;
my $mismatchPenalty = 0;
my $searchSize = 0;
my $showGIs = 0;
my $wordsize = 0;
my $formatTypesCsv = '';


my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"        =>  \$username,
                        "project=s"         =>  \$project,
                        "workSessionId=s"   =>  \$workSessionId,
                        "jobName=s"         =>  \$jobName,
                        "subjectDBIdentifier=s"         =>  \$subjectDBIdentifier,
                        "queryFastaFileNodeId=s"        =>  \$queryFastaFileNodeId,
                        "databaseAlignmentsPerQuery=i"  =>  \$databaseAlignmentsPerQuery,
                        "filter=s"                      =>  \$filter,
                        "eValueExponent=i"              =>  \$eValueExponent,
                        "lowercaseFiltering=i"          =>  \$lowercaseFiltering,
                        "believeDefline=i"              =>  \$believeDefline,
                        "databaseSize=i"                =>  \$databaseSize,
                        "gapExtendCost=i"               =>  \$gapExtendCost,
                        "gappedAlignment=i"             =>  \$gappedAlignment,
                        "hitExtensionThreshold=i"       =>  \$hitExtensionThreshold,
                        "matrix=s"                      =>  \$matrix,    
                        "multiHitWindowSize=i"          =>  \$multiHitWindowSize,
                        "searchStrand=s"                =>  \$searchStrand,
                        "ungappedExtensionDropoff=i"    =>  \$ungappedExtensionDropoff,
                        "bestHitsToKeep=i"              =>  \$bestHitsToKeep,
                        "finalGappedDropoff=i"          =>  \$finalGappedDropoff,
                        "gapOpenCost=i"                 =>  \$gapOpenCost,
                        "gappedAlignmentDropoff=i"      =>  \$gappedAlignmentDropoff,
                        "matchReward=i"                 =>  \$matchReward,
                        "mismatchPenalty=i"             =>  \$mismatchPenalty,
                        "searchSize=i"                  =>  \$searchSize,
                        "showGIs=i"                     =>  \$showGIs,
                        "wordsize=i"                    =>  \$wordsize,
                        "formatTypesCsv=s"              =>  \$formatTypesCsv,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runBlastN';
my @options = ( $username,  # username
                '',         # token
                $project,   # project
                $workSessionId,                 # workSessionId
                $jobName,                       # jobName
                $subjectDBIdentifier,           # subjectDBIdentifier
                $queryFastaFileNodeId,          # queryFastaFileNodeId
                $databaseAlignmentsPerQuery,    # databaseAlignmentsPerQuery
                $filter,                        # filter
                $eValueExponent,                # eValueExponent
                $lowercaseFiltering,            # lowercaseFiltering
                $believeDefline,                # believeDefline
                $databaseSize,                  # databaseSize
                $gapExtendCost,                 # gapExtendCost
                $gappedAlignment,               # gappedAlignment
                $hitExtensionThreshold,         # hitExtensionThreshold
                $matrix,                        # matrix
                $multiHitWindowSize,            # multiHitWindowSize
                $searchStrand,                  # searchStrand
                $ungappedExtensionDropoff,      # ungappedExtensionDropoff
                $bestHitsToKeep,                # bestHitsToKeep
                $finalGappedDropoff,            # finalGappedDropoff
                $gapOpenCost,                   # gapOpenCost
                $gappedAlignmentDropoff,        # gappedAlignmentDropoff
                $matchReward,                   # matchReward
                $mismatchPenalty,               # mismatchPenalty
                $searchSize,                    # searchSize
                $showGIs,                       # showGIs
                $wordsize,                      # wordsize
                $formatTypesCsv,                # formatTypesCsv

                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runBlastN(@options);

