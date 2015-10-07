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

test_runReversePsiBlast.pl - test the runReversePsiBlast web-service call.

=head1 SYNOPSIS

    USAGE: test_runReversePsiBlast.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid code for submission [08020]
 --workSessionId            Work session id, if desired
 --jobName                  Name to refer to this job
 --subjectDBIdentifier      VICS id for the database
 --queryFastaFileNodeId     VICS id for the input file

=head2 program parameters

 --eValueExponent
 --blastExtensionDropoffBits
 --believeDefline
 --showGIsInDeflines
 --lowercaseFiltering
 --forceLegacyBlastEngine
 --filterQueryWithSeg
 --gappedAlignmentDropoff
 --bitsToTriggerGapping
 --finalGappedAlignmentDropoff
 --databaseAlignmentsPerQuery

=head1  DESCRIPTION

This script tests the runReversePsiBlast web service.

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
my $jobName = 'TestReversePsiBlast';
my $subjectDBIdentifier = '1438256871256361131';
my $queryFastaFileNodeId = '1438285098964224171';
my $eValueExponent = -5;
my $blastExtensionDropoffBits = 0;
my $believeDefline = 0;
my $showGIsInDeflines = 0;
my $lowercaseFiltering = 0;
my $forceLegacyBlastEngine = 0;
my $filterQueryWithSeg = 0;
my $gappedAlignmentDropoff = 0;
my $bitsToTriggerGapping = 0;
my $finalGappedAlignmentDropoff = 0;
my $databaseAlignmentsPerQuery = 10;

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

                        "eValueExponent=i"              =>  \$eValueExponent,
                        "blastExtensionDropoffBits=i"   =>  \$blastExtensionDropoffBits,
                        "believeDefline=i"              =>  \$believeDefline,
                        "showGIsInDeflines=i"           =>  \$showGIsInDeflines,
                        "lowercaseFiltering=i"          =>  \$lowercaseFiltering,
                        "forceLegacyBlastEngine=i"      =>  \$forceLegacyBlastEngine,
                        "filterQueryWithSeg=i"          =>  \$filterQueryWithSeg,
                        "gappedAlignmentDropoff=i"      =>  \$gappedAlignmentDropoff,
                        "bitsToTriggerGapping=i"        =>  \$bitsToTriggerGapping,
                        "finalGappedAlignmentDropoff=i" =>  \$finalGappedAlignmentDropoff,
                        "databaseAlignmentsPerQuery=i"  =>  \$databaseAlignmentsPerQuery,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runReversePsiBlast';
my @options = ( $username,                      # username
                '',                             # token
                $project,                       # project
                $workSessionId,                 # workSessionId
                $jobName,                       # jobName
                $subjectDBIdentifier,           # subjectDBIdentifier
                $queryFastaFileNodeId,          # queryFastaFileNodeId
                $eValueExponent,                # eValueExponent
                $blastExtensionDropoffBits,     # blastExtensionDropoffBits
                $believeDefline,                # believeDefline
                $showGIsInDeflines,             # showGIsInDeflines
                $lowercaseFiltering,            # lowercaseFiltering
                $forceLegacyBlastEngine,        # forceLegacyBlastEngine
                $filterQueryWithSeg,            # filterQueryWithSeg
                $gappedAlignmentDropoff,        # gappedAlignmentDropoff
                $bitsToTriggerGapping,          # bitsToTriggerGapping
                $finalGappedAlignmentDropoff,   # finalGappedAlignmentDropoff
                $databaseAlignmentsPerQuery,    # databaseAlignmentsPerQuery
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runReversePsiBlast(@options);

