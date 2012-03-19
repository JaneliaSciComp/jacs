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

test_getBlastDatabaseStatus.pl - test the getBlastDatabaseStatus web-service call.

=head1 SYNOPSIS

    USAGE: test_getBlastDatabaseStatus.pl OPTION LIST HERE

=head1 OPTIONS

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

 --username     User to submit as [Default is current]
 --taskId       VICS job id to check on

=head1  DESCRIPTION

This script tests the getBlastDatabaseStatus web service, invoking with the username
argument set to the current user and the token argument set to ''.

=head1  INPUT

There is no required input for this script.

=head1  OUTPUT

This web service presents the following output upon successful formating of the file:

Status Type: completed
Status Description: Process 1436484265041725611 completed successfully
Blast Database Name: ji_ws_test_dbBlast Database Id: 1436484266337765547Blast Database Location: Xruntime-shared/filestore/jinman/BlastDatabases/1436484266337765547

Some other similar message should appear if the db is still being formatted.

=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut


my $port = 81;
my $server = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';

my $username = getlogin;
my $taskId   = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    => \$wsdl_url,

                        "username=s"    =>  \$username,
                        "taskId=s"      =>  \$taskId,
                        );
                        
$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'getBlastDatabaseStatus';
my @options = ( $username,  # username
                '',         # token
                $taskId,    # taskId (job id as told by uploadAndFormatBlastDataset)
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> getBlastDatabaseStatus(@options);

