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

test_runMetaGenoCombinedOrfAnno.pl - test the runMetaGenoCombinedOrfAnno web-service call.

=head1 SYNOPSIS

    USAGE: test_runMetaGenoCombinedOrfAnno.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username     User to submit as [Default is current]
 --project      Grid code for submission [08020]
 --workSessionId            Work session id, if desired
 --jobName                  Name to refer to this job
 --inputFastaFileNodeId     VICS id for the input fasta

=head2 program parameters

 --useClearRange    

=head1  DESCRIPTION

This script tests the runMetaGenoCombinedOrfAnno web service.

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
my $workSessionId= '';
my $jobName = 'TestMetaGenoCombinedOrfAnno';
my $inputFastaFileNodeId = '1436478961931520171';
my $useClearRange = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "inputFastaFileNodeId=s"    =>  \$inputFastaFileNodeId,
                        "useClearRange=s"           =>  \$useClearRange
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runMetaGenoCombinedOrfAnno';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $inputFastaFileNodeId,  # inputFastaFileNodeId
                $useClearRange,         # useClearRange
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runMetaGenoCombinedOrfAnno(@options);

