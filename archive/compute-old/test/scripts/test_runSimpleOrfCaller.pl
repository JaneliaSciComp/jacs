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

test_runSimpleOrfCaller.pl - test the runSimpleOrfCaller web-service call.

=head1 SYNOPSIS

    USAGE: test_runSimpleOrfCaller.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid code to use for submission [08020]
 --workSessionId            Work session, if desired
 --jobName                  Name by which to refer to this job
 --inputFastaFileNodeId     VICS id for the input file

=head2 program parameters

 --translation_table
 --beggining_as_start
 --end_as_stop
 --assume_stops
 --full_orfs
 --min_orf_size
 --max_orf_size
 --min_unmasked_size
 --frames
 --force_methionine
 --header_additions

=head1  DESCRIPTION

This script tests the runSimpleOrfCaller web service.

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
my $jobName = 'TestSimpleOrfCaller';
my $inputFastaFileNodeId = '1436478961931520171';
my $translation_table = '';
my $beginning_as_start = '';
my $end_as_stop = '';
my $assume_stops = '';
my $full_orfs = '';
my $min_orf_size = '';
my $max_orf_size = '';
my $min_unmasked_size = '';
my $frames = '';
my $force_methionine = '';
my $header_additions = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"    =>  \$username,
                        "project=s"     =>  \$project,
                        "workSessionId=s"   =>  \$workSessionId,
                        "jobName=s"         =>  \$jobName,
                        "inputFastaFileNodeId=s"    =>  \$inputFastaFileNodeId,
                        "translation_table=s"       =>  \$translation_table,
                        "beginning_as_start=s"      =>  \$beginning_as_start,
                        "end_as_stop=s"             =>  \$end_as_stop,
                        "assume_stops=s"            =>  \$assume_stops,
                        "full_orfs=s"               =>  \$full_orfs,
                        "min_orf_size=s"            =>  \$min_orf_size,
                        "max_orf_size=s"            =>  \$max_orf_size,
                        "min_unmasked_size=s"       =>  \$min_unmasked_size,
                        "frames=s"                  =>  \$frames,
                        "force_methionine=s",       =>  \$force_methionine,
                        "header_additions=s",       =>  \$header_additions,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runSimpleOrfCaller';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $inputFastaFileNodeId,  # inputFastaFileNodeId
                $translation_table,     # translation_table
                $beginning_as_start,    # beginning_as_start
                $end_as_stop,           # end_as_stop
                $assume_stops,          # assume_stops
                $full_orfs,             # full_orfs
                $min_orf_size,          # min_orf_size
                $max_orf_size,          # max_orf_size
                $min_unmasked_size,     # min_unmasked_size
                $frames,                # frames
                $force_methionine,      # force_methionine
                $header_additions,      # header_additions
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runSimpleOrfCaller(@options);

