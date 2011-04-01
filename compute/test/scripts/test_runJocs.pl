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

test_runJocs.pl - test the runJocs web-service call.

=head1 SYNOPSIS

    USAGE: test_runJocs.pl OPTION LIST HERE

=head1 OPTIONS

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -s   The server on which to connect [saffordt-ws1]
 --wsdl_name    The name of the wsdl [ComputeWS]
 --wsdl_path    Path on the server to the wsdl [compute-compute]

 --wsdl_url     Full url to the wsdl

=head2 VICS parameters

 --username                 User to submit as [Default is current]
 --project                  Grid submmission code [08020]
 --workSessionId            Work session id, if desired
 --jobName                  Name that refers to this job
 --fastaInputNodeId     VICS id for the input fasta
 
=head2 program parameters

Parameter options for CogBsmlLoader.pl -  
Preprocess data stored in BSML pairwise alignment documents into
BTAB structure for COG analysis using best_hits.pl.

--bsmlModelList  			List file of fasta files
--bsmlSearchList  			List file containing blast results
--bsmlJaccardList			List file containing clustalw results from Jaccard run
--pvalue 					P value cut off for run
--coverageCutoff

Parameter options for best_hit.pl -  
Generates # clusters of proteins that are connected by bidirectional
best hits from a single btab file.

-c 			e-value cutoff, best hits with an e-value above this cutoff will be ignored
-i 			input btab file
-j 			Jaccard coefficient cutoff

Parameter options for CogProteinFasta.pl

-cogFile  				Output from best_hit.pl, not passed into service
-bsmlModelList 			List file containing results from legacy2bsml
-maxCogSeqCount

=back

=head1  DESCRIPTION

This script tests the runJaccard web service.

=head1  INPUT


=head1  OUTPUT

When succesfully submitted, this web service returns the following:
Job Id: 1436500110895745195

=head1  CONTACT

    Erin Beck
    ebeck@jcvi.org

=cut

my $port      = 81;
my $server    = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url  = '';

my $username      = getlogin;
my $project       = '08100';
my $workSessionId = '';
my $jobName       = 'TestJocs';

my $bsml_search_list         = '';
my $bsml_model_list          = '';
my $bsml_jaccard_list        = '';
my $p_value                  = '';
my $coverage_cutoff          = '';
my $jaccard_coefficient      = '';
my $j_cutoff                 = '';
my $max_cog_seq_count        = '';

my $result = GetOptions(
	"port|p=i"    => \$port,
	"server|s=s"  => \$server,
	"wsdl_name=s" => \$wsdl_name,
	"wsdl_path=s" => \$wsdl_path,
	"wsdl_url=s"  => \$wsdl_url,

	"username=s"      => \$username,
	"project=s"       => \$project,
	"workSessionId=s" => \$workSessionId,
	"jobName=s"       => \$jobName,

	"bsml_search_list=s"         => \$bsml_search_list,
	"bsml_model_list=s"          => \$bsml_model_list,
	"bsml_jaccard_list=s"        => \$bsml_jaccard_list,
	"p_value=s"                  => \$p_value,
	"coverage_cutoff=s"          => \$coverage_cutoff,
	"jaccard_coefficient=s"      => \$jaccard_coefficient,
	"j_cutoff=s"                 => \$j_cutoff,
	"max_cog_seq_count=s"        => \$max_cog_seq_count
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runJocs';
my @options = (
	$username,         # username
	'',                # token
	$project,          # project
	$workSessionId,    # workSessionId
	$jobName,          # jobName

	$bsml_search_list,           #bsmlSearchList
	$bsml_model_list,            #bsmlModelList
	$bsml_jaccard_list,          #bsmlJaccardlLis
	$p_value,                    #pvalcut
	$coverage_cutoff,            #coverageCutoff
	$jaccard_coefficient,        #j
	$j_cutoff,                   #c
	$max_cog_seq_count          #maxCogSeqCount

);

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite->service($wsdl_url)->runJocs(@options);

