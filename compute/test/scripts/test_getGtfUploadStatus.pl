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

test_getGtfUploadStatus.pl - test the getGtfUploadStatus web-service call.

=head1 SYNOPSIS

    USAGE: test_getGtfUploadStatus.pl OPTION LIST HERE

=head1 OPTIONS

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

 --username     User to submit as [Default is current]
 --taskId       VICS job id to check on

=head1  DESCRIPTION

This script tests the getGtfUploadStatus web service.

=head1  INPUT

There is no required input for this script.

=head1  OUTPUT

When succesfully submitted, this web service returns the following if the task has already completed:

Status Type: completed
Status Description: Process 1437964257315522731 completed successfully

Result(s) location: Xruntime-shared/filestore/jinman/WorkSession/1437964257542015147

If the task isn't completed, some other status is returned.  For example:

Status Type: merging
Status Description: Starting sequence Merge Blast Results

or if there's an error:

Status Type: error
Status Description: Action PROCESS BlastWithGridMerge encountered unexpected exception: 


=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut

my $server = 'saffordt-ws1';
my $port = 81;
my $wsdl_path = 'compute-compute';
my $wsdl_name = 'ComputeWS';
my $wsdl_url = '';

my $username = getlogin;

my $taskId = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"    =>  \$username,
                        "taskId=s"      =>  \$taskId,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'getGtfUploadStatus';
my @options = ( $username,  # username
                '',         # token
                $taskId,    # taskId
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> getGtfUploadStatus(@options);

