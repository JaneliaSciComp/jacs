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

test_runEvidenceModeler.pl - test the runEvidenceModeler web-service call.

=head1 SYNOPSIS

    USAGE: test_runEvidenceModeler.pl OPTION LIST HERE

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

 --fastaInputNodeId         VICS node id for genome input

=head2 program parameters

 --weights                        
 --gene_predictions               
 --protein_alignments             
 --transcript_alignments          
 --repeats                        
 --terminalExons                  
 --stitch_ends                    
 --extend_to_terminal             
 --stop_codons                    
 --min_intron_length              
 --INTERGENIC_SCORE_ADJUST_FACTOR 
 --exec_dir                       
 --forwardStrandOnly              
 --reverseStrandOnly              
 --verbose                        
 --debug                          
 --report_ELM                     
 --RECURSE                     
 --limit_range_lend               
 --limit_range_rend               
 --segmentSize
 --overlapSize

=head1  DESCRIPTION

This script tests the runEvidenceModeler web service.

=head1  INPUT

There is no required input for this script.  However, it is assumed that an
appropriate fasta has been uploaded (provided to this script via the
--fastaInputNodeId parameter).

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
my $jobName = 'TestEvidenceModeler';
my $fastaInputNodeId                = '';

my $weights                         = '';
my $gene_predictions                = '';
my $protein_alignments              = '';
my $transcript_alignments           = '';
my $repeats                         = '';
my $terminalExons                   = '';
my $stitch_ends                     = '';
my $extend_to_terminal              = '';
my $stop_codons                     = '';
my $min_intron_length               = '';
my $INTERGENIC_SCORE_ADJUST_FACTOR  = '';
my $exec_dir                        = '';
my $forwardStrandOnly               = '';
my $reverseStrandOnly               = '';
my $verbose                         = '';
my $debug                           = '';
my $report_ELM                      = '';
my $RECURSE                      = '';
my $limit_range_lend                = '';
my $limit_range_rend                = '';
my $segmentSize                     = '';
my $overlapSize                     = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "fastaInputNodeId=s"	=>	\$fastaInputNodeId,

                        "weights=s"	=>	\$weights,
                        "gene_predictions=s"	=>	\$gene_predictions,
                        "protein_alignments=s"	=>	\$protein_alignments,
                        "transcript_alignments=s"	=>	\$transcript_alignments,
                        "repeats=s"	=>	\$repeats,
                        "terminalExons=s"	=>	\$terminalExons,
                        "stitch_ends=s"	=>	\$stitch_ends,
                        "extend_to_terminal=s"	=>	\$extend_to_terminal,
                        "stop_codons=s"	=>	\$stop_codons,
                        "min_intron_length=s"	=>	\$min_intron_length,
                        "INTERGENIC_SCORE_ADJUST_FACTOR=s"	=>	\$INTERGENIC_SCORE_ADJUST_FACTOR,
                        "exec_dir=s"	=>	\$exec_dir,
                        "forwardStrandOnly=s"	=>	\$forwardStrandOnly,
                        "reverseStrandOnly=s"	=>	\$reverseStrandOnly,
                        "verbose=s"	=>	\$verbose,
                        "debug=s"	=>	\$debug,
                        "report_ELM=s"	=>	\$report_ELM,
                        "RECURSE=s"	=>	\$RECURSE,
                        "limit_range_lend=s"	=>	\$limit_range_lend,
                        "limit_range_rend=s"	=>	\$limit_range_rend,
                        "segmentSize=s"         =>  \$segmentSize,
                        "overlapSize=s"         =>  \$overlapSize,

                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runEvidenceModeler';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
				$fastaInputNodeId,		#fastaInputNodeId

				$weights,		#weights
				$gene_predictions,		#gene_predictions
				$protein_alignments,		#protein_alignments
				$transcript_alignments,		#transcript_alignments
				$repeats,		#repeats
				$terminalExons,		#terminalExons
				$stitch_ends,		#stitch_ends
				$extend_to_terminal,		#extend_to_terminal
				$stop_codons,		#stop_codons
				$min_intron_length,		#min_intron_length
				$INTERGENIC_SCORE_ADJUST_FACTOR,		#INTERGENIC_SCORE_ADJUST_FACTOR
				$exec_dir,		#exec_dir
				$forwardStrandOnly,		#forwardStrandOnly
				$reverseStrandOnly,		#reverseStrandOnly
				$verbose,		#verbose
				$debug,		#debug
				$report_ELM,		#report_ELM
				$RECURSE,		#RECURSE
				$limit_range_lend,		#limit_range_lend
				$limit_range_rend,		#limit_range_rend
                $segmentSize,           #segmentSize
                $overlapSize,           #overlapSize

                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runEvidenceModeler(@options);

