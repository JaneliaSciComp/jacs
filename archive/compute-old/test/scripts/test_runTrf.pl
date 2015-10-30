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

test_runTrf.pl - test the runTrf web-service call.

=head1 SYNOPSIS

    USAGE: test_runTrf.pl OPTION LIST HERE

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
 --fastaInputNodeId         VICS id for the input fasta

=head2 program parameters

 --matchingWeight
 --mismatching_penalty
 --indel_penalty
 --match_probability
 --indel_probability
 --minscore
 --maxperiod
 --masked_sequence_file
 --flanking_sequence
 --data_file
 --suppress_html_input

=head1  DESCRIPTION

This script tests the runTrf web service.

=head1  INPUT

There is no required input for this script.  However, it is assumed that an
appropriate nucleotide fasta has been uploaded (provided to this script via the
--fastaInputNodeId parameter)

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
my $jobName = 'TestTrf';
my $fastaInputNodeId = '1438285098964224171';

my $matchingWeight      = '';
my $mismatching_penalty = '';
my $indel_penalty       = '';
my $match_probability   = '';
my $indel_probability   = '';
my $minscore            = '';
my $maxperiod           = '';
my $masked_sequence_file= '';
my $flanking_sequence   = '';
my $data_file           = '';
my $suppress_html_input = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "fastaInputNodeId=s"        =>  \$fastaInputNodeId,

                        "matchingWeight=s"			=>	\$matchingWeight,
                        "mismatching_penalty=s"		=>	\$mismatching_penalty,
                        "indel_penalty=s"			=>	\$indel_penalty,
                        "match_probability=s"		=>	\$match_probability,
                        "indel_probability=s"		=>	\$indel_probability,
                        "minscore=s"			    =>	\$minscore,
                        "maxperiod=s"			    =>	\$maxperiod,
                        "masked_sequence_file=s"	=>	\$masked_sequence_file,
                        "flanking_sequence=s"		=>	\$flanking_sequence,
                        "data_file=s"			    =>	\$data_file,
                        "suppress_html_input=s"		=>	\$suppress_html_input,
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runTrf';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $fastaInputNodeId,      # fastaInputNodeId

                $matchingWeight,		# matchingWeight
                $mismatching_penalty,	# mismatching_penalty
                $indel_penalty,		    # indel_penalty
                $match_probability,		# match_probability
                $indel_probability,		# indel_probability
                $minscore,		        # minscore
                $maxperiod,		        # maxperiod
                $masked_sequence_file,	# masked_sequence_file (-m)
                $flanking_sequence,		# flanking_sequence (-f)
                $data_file,		        # data_file (-d)
                $suppress_html_input,	# suppress_html_input (-h)
);


print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runTrf(@options);

