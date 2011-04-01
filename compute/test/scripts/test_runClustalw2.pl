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

test_runTmhmm.pl - test the runTmhmm web-service call.

=head1 SYNOPSIS

    USAGE: test_runTmhmm.pl OPTION LIST HERE

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
 --fastaInputNodeId         VICS id for the input fasta
 
=head2 program parameters

	-quiet 
	-align 
	-newtree 
	-usetree 
	-newtree1 
	-usetree1 
	-newtree2 
	-usetree2 
	-bootstrap 
	-tree 
	-quicktree 
	-convert 
	-batch 
	-iteration
	-type
	-profile 
	-sequences 
	-matrix 
	-dnamatrix 
	-negative 
	-noweights 
	-gapopen 
	-gapext 
	-endgaps 
	-nopgap 
	-nohgap 
	-novgap 
	-hgapresidues
	-maxdiv 
	-gapdist 
	-pwmatrix 
	-pwdnamatrix 
	-pwgapopen 
	-pwgapext 
	-ktuple 
	-window 
	-pairgap 
	-topdiags 
	-score
	-transweight
	-seed 
	-kimura 
	-tossgaps 
	-bootlabelsode
	-output
	-outputtree
	-outorder
	-cluster_case
	-seqnos
	-seqno_range
	-range
	-nosecstr1 
	-nosecstr2 
	-secstrout
	-helixgap 
	-strandgap 
	-loopgap 
	-terminalgap 
	-helixendin 
	-helixendout 
	-strandendin 
	-strandendout 
	-numiter 
	-clustering
	-maxseqlen 
	-stats 

=head1  DESCRIPTION

This script tests the runTmhmm web service, invoking with the username
argument set to the current user and the token argument set to ''.

=head1  INPUT

There is no required input for this script.

=head1  OUTPUT

When succesfully submitted, this web service returns the following:
Job Id: 1436500110895745195

=head1  CONTACT

    Jason Inman
    jinman@jcvi.org

=cut

my $port      = 81;
my $server    = 'saffordt-ws1';
my $wsdl_name = 'ComputeWS';
my $wsdl_path = 'compute-compute';
my $wsdl_url  = '';

my $username         = getlogin;
my $project          = '08100';
my $workSessionId    = '';
my $jobName          = 'TestClustalw2';
my $fastaInputNodeId = ''; #'1504256384294715768';
my $fastaInputFileList = '';

my $quiet         = '';
my $align         = '';
my $newtree       = '';
my $usetree       = '';
my $newtree1      = '';
my $usetree1      = '';
my $newtree2      = '';
my $usetree2      = '';
my $bootstrap     = '';
my $tree          = '';
my $quicktree     = '';
my $convert       = '';
my $batch         = '';
my $iteration     = '';
my $type          = '';
my $profile       = '';
my $sequences     = '';
my $matrix        = '';
my $dnamatrix     = '';
my $negative      = '';
my $noweights     = '';
my $gapopen       = '';
my $gapext        = '';
my $endgaps       = '';
my $nopgap        = '';
my $nohgap        = '';
my $novgap        = '';
my $hgapresidues  = '';
my $maxdiv        = '';
my $gapdist       = '';
my $pwmatrix      = '';
my $pwdnamatrix   = '';
my $pwgapopen     = '';
my $pwgapext      = '';
my $ktuple        = '';
my $window        = '';
my $pairgap       = '';
my $topdiags      = '';
my $score         = '';
my $transweight   = '';
my $seed          = '';
my $kimura        = '';
my $tossgaps      = '';
my $bootlabelsode = '';
my $output        = '';
my $outputtree    = '';
my $outorder      = '';
my $cluster_case  = '';
my $seqnos        = '';
my $seqno_range   = '';
my $range         = '';
my $nosecstr1     = '';
my $nosecstr2     = '';
my $secstrout     = '';
my $helixgap      = '';
my $strandgap     = '';
my $loopgap       = '';
my $terminalgap   = '';
my $helixendin    = '';
my $helixendout   = '';
my $strandendin   = '';
my $strandendout  = '';
my $numiter       = '';
my $clustering    = '';
my $maxseqlen     = '';
my $stats         = '';

my $result = GetOptions(
	"port|p=i"    => \$port,
	"server|s=s"  => \$server,
	"wsdl_name=s" => \$wsdl_name,
	"wsdl_path=s" => \$wsdl_path,
	"wsdl_url=s"  => \$wsdl_url,

	"username=s"         => \$username,
	"project=s"          => \$project,
	"workSessionId=s"    => \$workSessionId,
	"jobName=s"          => \$jobName,
	"fastaInputNodeId=s" => \$fastaInputNodeId,
	"fastaInputFileList=s" => \$fastaInputFileList,

	"quiet=s"         => \$quiet,
	"align=s"         => \$align,
	"newtree=s"       => \$newtree,
	"usetree=s"       => \$usetree,
	"newtree1=s"      => \$newtree1,
	"usetree1=s"      => \$usetree1,
	"newtree2=s"      => \$newtree2,
	"usetree2=s"      => \$usetree2,
	"bootstrap=s"     => \$bootstrap,
	"tree=s"          => \$tree,
	"quicktree=s"     => \$quicktree,
	"convert=s"       => \$convert,
	"batch=s"         => \$batch,
	"iteration=s"     => \$iteration,
	"type=s"          => \$type,
	"profile=s"       => \$profile,
	"sequences=s"     => \$sequences,
	"matrix=s"        => \$matrix,
	"dnamatrix=s"     => \$dnamatrix,
	"negative=s"      => \$negative,
	"noweights=s"     => \$noweights,
	"gapopen=s"       => \$gapopen,
	"gapext=s"        => \$gapext,
	"endgaps=s"       => \$endgaps,
	"nopgap=s"        => \$nopgap,
	"nohgap=s"        => \$nohgap,
	"novgap=s"        => \$novgap,
	"hgapresidues=s"  => \$hgapresidues,
	"maxdiv=s"        => \$maxdiv,
	"gapdist=s"       => \$gapdist,
	"pwmatrix=s"      => \$pwmatrix,
	"pwdnamatrix=s"   => \$pwdnamatrix,
	"pwgapopen=s"     => \$pwgapopen,
	"pwgapext=s"      => \$pwgapext,
	"ktuple=s"        => \$ktuple,
	"window=s"        => \$window,
	"pairgap=s"       => \$pairgap,
	"topdiags=s"      => \$topdiags,
	"score=s"         => \$score,
	"transweight=s"   => \$transweight,
	"seed=s"          => \$seed,
	"kimura=s"        => \$kimura,
	"tossgaps=s"      => \$tossgaps,
	"bootlabelsode=s" => \$bootlabelsode,
	"output=s"        => \$output,
	"outputtree=s"    => \$outputtree,
	"outorder=s"      => \$outorder,
	"cluster_case=s"  => \$cluster_case,
	"seqnos=s"        => \$seqnos,
	"seqno_range=s"   => \$seqno_range,
	"range=s"         => \$range,
	"nosecstr1=s"     => \$nosecstr1,
	"nosecstr2=s"     => \$nosecstr2,
	"secstrout=s"     => \$secstrout,
	"helixgap=s"      => \$helixgap,
	"strandgap=s"     => \$strandgap,
	"loopgap=s"       => \$loopgap,
	"terminalgap=s"   => \$terminalgap,
	"helixendin=s"    => \$helixendin,
	"helixendout=s"   => \$helixendout,
	"strandendin=s"   => \$strandendin,
	"strandendout=s"  => \$strandendout,
	"numiter=s"       => \$numiter,
	"clustering=s"    => \$clustering,
	"maxseqlen=s"     => \$maxseqlen,
	"stats=s"         => \$stats
);

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runClustalw2';
my @options = (
	$username,                              # username
	'',                                     # token
	$project,                               # project
	$workSessionId,                         # workSessionId
	$jobName,                               # jobName
	$fastaInputNodeId,                      # fastaInputNodeId
	$fastaInputFileList,                    # fastaInputFileList

	$align,                                 # align
	$tree,                                  # tree
	$bootstrap,                             # bootstrap
	$convert,                               # convert
	$quicktree,                             # quicktree
	$type,                                  # type
	$negative,                              # negative
	$output,                                # output
	$outorder,                              # outorder
	$cluster_case,                          # case
	$seqnos,                                 # seqnos
	$seqno_range,                           # seqno_range
	$range,                                 # range
	$maxseqlen,                             # maxseqlen
	$quiet,                                 # quiet
	$stats,                                  # stats
	$ktuple,                                # ktuple
	$topdiags,                              # topdiags
	$window,                                # window
	$pairgap,                               # pairgap
	$score,                                 # score
	$pwmatrix,                              # pwmatrix
	$pwdnamatrix,                           # pwdnamatrix
	$pwgapopen,                             # pwgapopen
	$pwgapext,                              # pwgapext
	$newtree,                               # newtree
	$usetree,                               # usetree
	$matrix,                                # matrix
	$dnamatrix,                             # dnamatrix
	$gapopen,                               # gapopen
	$gapext,                                # gapext
	$endgaps,                               # endgaps
	$gapdist,                               # gapdist
	$nopgap,                                # nopgap
	$nohgap,                                # nohgap
	$hgapresidues,                          # hgapresidues
	$maxdiv,                                # maxdiv
	$transweight,                           # transweight
	$iteration,                             # iteration
	$numiter,                               # numiter
	$noweights,                             # noweights
	$profile,                               # profile
	$newtree1,                              # newtree1
	$usetree1,                              # usetree1
	$newtree2,                              # newtree2
	$usetree2,                              # usetree2
	$sequences,                             # sequences
	$nosecstr1,                             # nosecstr1
	$nosecstr2,                             # nosecstr2
	$secstrout,                             # secstrout
	$helixgap,                              # helixgap
	$strandgap,                             # strandgap
	$loopgap,                               # loopgap
	$terminalgap,                           # terminalgap
	$helixendin,                            # helixendin
	$helixendout,                           # helixendout
	$strandendin,                           # strandendin
	$strandendout,                          # strandendout
	$outputtree,                            # outputtree
	$seed,                                  # seed
	$kimura,                                # kimura
	$tossgaps,                              # tossgaps
	$bootlabelsode,                         # bootlabelsode
	$clustering,                            # clustering
	$batch                                  # batch
);

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite->service($wsdl_url)->runClustalw2(@options);

