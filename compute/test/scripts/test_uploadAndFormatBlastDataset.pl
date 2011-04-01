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

test_uploadAndFormatBlastDataset.pl - test the uploadAndFormatBlastDataset web-service call.

=head1 SYNOPSIS

    USAGE: test_uploadAndFormatBlastDataset.pl OPTION LIST HERE

=head1 OPTIONS

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

 --username             Username to submit as [Default is current]
 --workSessionId        Work session ID, if needed.
 --blastDBName          What to name the formatted db. [testDB]
 --blastDBDescription   Longer description than the name. ["This is a test database"]
 --pathToFastaFile      Location of the fasta file to upload.

=head1  DESCRIPTION

This script tests the uploadAndFormatBlastDataset web service

=head1  INPUT

There is no required input for this script.

=head1  OUTPUT

This web service presents the follwoing output upon successful upload of the file:
Blast Database Name: ji_ws_test_db
Check status of job 1436484265041725611 with service getBlastDatabaseStatus() to know when your database creation is complete.

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
my $workSessionId = '';
my $blastDBName = 'testDB';
my $blastDBDescription = 'small prot test DB';
my $pathToFastaFile = '/usr/local/devel/ANNOTATION/jinman/vics_ws_testing/DS234993_multi_prot.fsa';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"            =>  \$username,
                        "workSessionId=s"       =>  \$workSessionId,
                        "blastDBName=s"         =>  \$blastDBName,
                        "blastDBDescription=s"  =>  \$blastDBDescription,
                        "pathToFastaFile=s"     =>  \$pathToFastaFile,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'uploadAndFormatBlastDataset';
my @options = ( $username,  # username
                '',         # token
                $workSessionId,         # workSessionId
                $blastDBName,           # blastDBName
                $blastDBDescription,    # blastDBDescription
                $pathToFastaFile,       # pathToFastaFile
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> uploadAndFormatBlastDataset(@options);

