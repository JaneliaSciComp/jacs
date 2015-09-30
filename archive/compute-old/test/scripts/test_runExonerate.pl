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

test_runExonerate.pl - test the runExonerate web-service call.

=head1 SYNOPSIS

    USAGE: test_runExonerate.pl OPTION LIST HERE

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

 --query_fasta_node_id      VICS node id for the query fasta
 --target_fasta_node_id     VICS node id for the target fasta

=head2 program parameters

 --querytype
 --targettype
 --querychunkidtype
 --targetchunkidtype
 --querychunktotaltype
 --targetchunktotaltype
 --verbose
 --exhaustive
 --bigseq
 --forcescan
 --saturatethreshold
 --customserver
 --fastasuffix
 --model
 --score
 --percent
 --showalignment
 --showsugar
 --showcigar
 --showvulgar
 --showquerygff
 --showtargetgff
 --ryo
 --bestn
 --subopt
 --gappedextension
 --refine
 --refineboundary
 --dpmemory
 --compiled
 --terminalrangeint
 --terminalrangeext
 --joinrangeint
 --joinrangeext
 --spanrangeint
 --spanrangeext
 --extensionthreshold
 --singlepass
 --joinfilter
 --annotation
 --softmaskquery
 --softmasktarget
 --dnasubmat
 --proteinsubmat
 --fsmmemory
 --forcefsm
 --wordjump
 --gapopen
 --gapextend
 --codongapopen
 --codongapextend
 --minner
 --maxner
 --neropen
 --minintron
 --maxintron
 --intronpenalty
 --frameshift
 --useaatla
 --geneticcode
 --hspfilter
 --useworddropoff
 --seedrepeat
 --dnawordlen
 --proteinwordlen
 --codonwordlen
 --dnahspdropoff
 --proteinhspdropoff
 --codonhspdropoff
 --dnahspthreshold
 --proteinhspthreshold
 --codonhspthreshold
 --dnawordlimit
 --proteinwordlimit
 --codonwordlimit
 --geneseed
 --geneseedrepeat
 --alignmentwidth
 --forwardcoordinates
 --quality
 --splice3
 --splice5

=head1  DESCRIPTION

This script tests the runExonerate web service.

=head1  INPUT

There is no required input for this script.  However, it is assumed that an
appropriate fasta has been uploaded (provided to this script via the
--query_fasta_node_id parameter), and that a target fasta has also been uploaded.

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
my $jobName = 'TestExonerate';
my $query_fasta_node_id = '';
my $target_fasta_node_id = '';

my $querytype = '';
my $targettype = '';
my $querychunkidtype = '';
my $targetchunkidtype = '';
my $querychunktotaltype = '';
my $targetchunktotaltype = '';
my $verbose = '';
my $exhaustive = '';
my $bigseq = '';
my $forcescan = '';
my $saturatethreshold = '';
my $customserver = '';
my $fastasuffix = '';
my $model = '';
my $score = '';
my $percent = '';
my $showalignment = '';
my $showsugar = '';
my $showcigar = '';
my $showvulgar = '';
my $showquerygff = '';
my $showtargetgff = '';
my $ryo = '';
my $bestn = '';
my $subopt = '';
my $gappedextension = '';
my $refine = '';
my $refineboundary = '';
my $dpmemory = '';
my $compiled = '';
my $terminalrangeint = '';
my $terminalrangeext = '';
my $joinrangeint = '';
my $joinrangeext = '';
my $spanrangeint = '';
my $spanrangeext = '';
my $extensionthreshold = '';
my $singlepass = '';
my $joinfilter = '';
my $annotation = '';
my $softmaskquery = '';
my $softmasktarget = '';
my $dnasubmat = '';
my $proteinsubmat = '';
my $fsmmemory = '';
my $forcefsm = '';
my $wordjump = '';
my $gapopen = '';
my $gapextend = '';
my $codongapopen = '';
my $codongapextend = '';
my $minner = '';
my $maxner = '';
my $neropen = '';
my $minintron = '';
my $maxintron = '';
my $intronpenalty = '';
my $frameshift = '';
my $useaatla = '';
my $geneticcode = '';
my $hspfilter = '';
my $useworddropoff = '';
my $seedrepeat = '';
my $dnawordlen = '';
my $proteinwordlen = '';
my $codonwordlen = '';
my $dnahspdropoff = '';
my $proteinhspdropoff = '';
my $codonhspdropoff = '';
my $dnahspthreshold = '';
my $proteinhspthreshold = '';
my $codonhspthreshold = '';
my $dnawordlimit = '';
my $proteinwordlimit = '';
my $codonwordlimit = '';
my $geneseed = '';
my $geneseedrepeat = '';
my $alignmentwidth = '';
my $forwardcoordinates = '';
my $quality = '';
my $splice3 = '';
my $splice5 = '';
my $forcegtag = '';


my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "query_fasta_node_id=s"     =>  \$query_fasta_node_id,
                        "target_fasta_node_id=s"    =>  \$target_fasta_node_id,

                        "querytype=s" => \$querytype,
                        "targettype=s" => \$targettype,
                        "querychunkidtype=s" => \$querychunkidtype,
                        "targetchunkidtype=s" => \$targetchunkidtype,
                        "querychunktotaltype=s" => \$querychunktotaltype,
                        "targetchunktotaltype=s" => \$targetchunktotaltype,
                        "verbose=s" => \$verbose,
                        "exhaustive=s" => \$exhaustive,
                        "bigseq=s" => \$bigseq,
                        "forcescan=s" => \$forcescan,
                        "saturatethreshold=s" => \$saturatethreshold,
                        "customserver=s" => \$customserver,
                        "fastasuffix=s" => \$fastasuffix,
                        "model=s" => \$model,
                        "score=s" => \$score,
                        "percent=s" => \$percent,
                        "showalignment=s" => \$showalignment,
                        "showsugar=s" => \$showsugar,
                        "showcigar=s" => \$showcigar,
                        "showvulgar=s" => \$showvulgar,
                        "showquerygff=s" => \$showquerygff,
                        "showtargetgff=s" => \$showtargetgff,
                        "ryo=s" => \$ryo,
                        "bestn=s" => \$bestn,
                        "subopt=s" => \$subopt,
                        "gappedextension=s" => \$gappedextension,
                        "refine=s" => \$refine,
                        "refineboundary=s" => \$refineboundary,
                        "dpmemory=s" => \$dpmemory,
                        "compiled=s" => \$compiled,
                        "terminalrangeint=s" => \$terminalrangeint,
                        "terminalrangeext=s" => \$terminalrangeext,
                        "joinrangeint=s" => \$joinrangeint,
                        "joinrangeext=s" => \$joinrangeext,
                        "spanrangeint=s" => \$spanrangeint,
                        "spanrangeext=s" => \$spanrangeext,
                        "extensionthreshold=s" => \$extensionthreshold,
                        "singlepass=s" => \$singlepass,
                        "joinfilter=s" => \$joinfilter,
                        "annotation=s" => \$annotation,
                        "softmaskquery=s" => \$softmaskquery,
                        "softmasktarget=s" => \$softmasktarget,
                        "dnasubmat=s" => \$dnasubmat,
                        "proteinsubmat=s" => \$proteinsubmat,
                        "fsmmemory=s" => \$fsmmemory,
                        "forcefsm=s" => \$forcefsm,
                        "wordjump=s" => \$wordjump,
                        "gapopen=s" => \$gapopen,
                        "gapextend=s" => \$gapextend,
                        "codongapopen=s" => \$codongapopen,
                        "codongapextend=s" => \$codongapextend,
                        "minner=s" => \$minner,
                        "maxner=s" => \$maxner,
                        "neropen=s" => \$neropen,
                        "minintron=s" => \$minintron,
                        "maxintron=s" => \$maxintron,
                        "intronpenalty=s" => \$intronpenalty,
                        "frameshift=s" => \$frameshift,
                        "useaatla=s" => \$useaatla,
                        "geneticcode=s" => \$geneticcode,
                        "hspfilter=s" => \$hspfilter,
                        "useworddropoff=s" => \$useworddropoff,
                        "seedrepeat=s" => \$seedrepeat,
                        "dnawordlen=s" => \$dnawordlen,
                        "proteinwordlen=s" => \$proteinwordlen,
                        "codonwordlen=s" => \$codonwordlen,
                        "dnahspdropoff=s" => \$dnahspdropoff,
                        "proteinhspdropoff=s" => \$proteinhspdropoff,
                        "codonhspdropoff=s" => \$codonhspdropoff,
                        "dnahspthreshold=s" => \$dnahspthreshold,
                        "proteinhspthreshold=s" => \$proteinhspthreshold,
                        "codonhspthreshold=s" => \$codonhspthreshold,
                        "dnawordlimit=s" => \$dnawordlimit,
                        "proteinwordlimit=s" => \$proteinwordlimit,
                        "codonwordlimit=s" => \$codonwordlimit,
                        "geneseed=s" => \$geneseed,
                        "geneseedrepeat=s" => \$geneseedrepeat,
                        "alignmentwidth=s" => \$alignmentwidth,
                        "forwardcoordinates=s" => \$forwardcoordinates,
                        "quality=s" => \$quality,
                        "splice3=s" => \$splice3,
                        "splice5=s" => \$splice5,
                        "forcegtag=s" => \$forcegtag,

                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runExonerate';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $query_fasta_node_id, # query_fasta_node_id
                $target_fasta_node_id, # target_fasta_node_id
                $querytype, # querytype
                $targettype, # targettype
                $querychunkidtype, # querychunkidtype
                $targetchunkidtype, # targetchunkidtype
                $querychunktotaltype, # querychunktotaltype
                $targetchunktotaltype, # targetchunktotaltype
                $verbose, # verbose
                $exhaustive, # exhaustive
                $bigseq, # bigseq
                $forcescan, # forcescan
                $saturatethreshold, # saturatethreshold
                $customserver, # customserver
                $fastasuffix, # fastasuffix
                $model, # model
                $score, # score
                $percent, # percent
                $showalignment, # showalignment
                $showsugar, # showsugar
                $showcigar, # showcigar
                $showvulgar, # showvulgar
                $showquerygff, # showquerygff
                $showtargetgff, # showtargetgff
                $ryo, # ryo
                $bestn, # bestn
                $subopt, # subopt
                $gappedextension, # gappedextension
                $refine, # refine
                $refineboundary, # refineboundary
                $dpmemory, # dpmemory
                $compiled, # compiled
                $terminalrangeint, # terminalrangeint
                $terminalrangeext, # terminalrangeext
                $joinrangeint, # joinrangeint
                $joinrangeext, # joinrangeext
                $spanrangeint, # spanrangeint
                $spanrangeext, # spanrangeext
                $extensionthreshold, # extensionthreshold
                $singlepass, # singlepass
                $joinfilter, # joinfilter
                $annotation, # annotation
                $softmaskquery, # softmaskquery
                $softmasktarget, # softmasktarget
                $dnasubmat, # dnasubmat
                $proteinsubmat, # proteinsubmat
                $fsmmemory, # fsmmemory
                $forcefsm, # forcefsm
                $wordjump, # wordjump
                $gapopen, # gapopen
                $gapextend, # gapextend
                $codongapopen, # codongapopen
                $codongapextend, # codongapextend
                $minner, # minner
                $maxner, # maxner
                $neropen, # neropen
                $minintron, # minintron
                $maxintron, # maxintron
                $intronpenalty, # intronpenalty
                $frameshift, # frameshift
                $useaatla, # useaatla
                $geneticcode, # geneticcode
                $hspfilter, # hspfilter
                $useworddropoff, # useworddropoff
                $seedrepeat, # seedrepeat
                $dnawordlen, # dnawordlen
                $proteinwordlen, # proteinwordlen
                $codonwordlen, # codonwordlen
                $dnahspdropoff, # dnahspdropoff
                $proteinhspdropoff, # proteinhspdropoff
                $codonhspdropoff, # codonhspdropoff
                $dnahspthreshold, # dnahspthreshold
                $proteinhspthreshold, # proteinhspthreshold
                $codonhspthreshold, # codonhspthreshold
                $dnawordlimit, # dnawordlimit
                $proteinwordlimit, # proteinwordlimit
                $codonwordlimit, # codonwordlimit
                $geneseed, # geneseed
                $geneseedrepeat, # geneseedrepeat
                $alignmentwidth, # alignmentwidth
                $forwardcoordinates, # forwardcoordinates
                $quality, # quality
                $splice3, # splice3
                $splice5, # splice5
                $forcegtag, # forcegtag
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runExonerate(@options);

