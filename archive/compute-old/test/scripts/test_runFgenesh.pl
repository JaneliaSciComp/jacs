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

test_runFgenesh.pl - test the runFgenesh web-service call.

=head1 SYNOPSIS

    USAGE: test_runFgenesh.pl OPTION LIST HERE

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

 --par_file
 --GC_cutoff
 --position_1
 --position_2
 --condensed
 --exon_table
 --exon_bonus
 --pmrna
 --pexons
 --min_thr
 --scp_prom
 --scp_term
 --min_f_exon
 --min_i_exon
 --min_t_exon
 --min_s_exon
 --nvar
 --try_best_exons
 --try_best_sites
 --not_rem
 --vthr
 --use_table
 --show_table

=head1  DESCRIPTION

This script tests the runFgenesh web service.

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
my $jobName = 'TestFgenesh';
my $inputFastaNodeId = '1438285098964224171';
my $par_file    = '';
my $GC_cutoff   = '';
my $position_1  = '';
my $position_2  = '';
my $condensed   = '';
my $exon_table  = '';
my $exon_bonus  = '';
my $pmrna = '';
my $pexons = '';
my $min_thr = '';
my $scp_prom = '';
my $scp_term = '';
my $min_f_exon = '';
my $min_i_exon = '';
my $min_t_exon = '';
my $min_s_exon = '';
my $nvar = '';
my $try_best_exons = '';
my $try_best_sites = '';
my $not_rem = '';
my $vthr = '';
my $use_table = '';
my $show_table = '';


my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|s=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "inputFastaNodeId=s"        =>  \$inputFastaNodeId,

                        "par_file=s"                => \$par_file,
                        "GC_cutoff=s"               => \$GC_cutoff,
                        "position_1=s"              => \$position_1,
                        "position_2=s"              => \$position_2,
                        "condensed"                 => \$condensed,
                        "exon_table=s"              => \$exon_table,
                        "exon_bonus=s"              => \$exon_bonus,
                        "pmrna"                     => \$pmrna,
                        "pexons"                    => \$pexons,
                        "min_thr=s"                 => \$min_thr,
                        "scp_prom"                  => \$scp_prom,
                        "scp_term"                  => \$scp_term,
                        "min_f_exon=s"              => \$min_f_exon,
                        "min_i_exon=s"              => \$min_i_exon,
                        "min_t_exon=s"              => \$min_t_exon,
                        "min_s_exon=s"              => \$min_s_exon,
                        "nvar=s"                    => \$nvar,
                        "try_best_exons=s"          => \$try_best_exons,
                        "try_best_sites=s"          => \$try_best_sites,
                        "not_rem"                   => \$not_rem,
                        "vthr=s"                    => \$vthr,
                        "use_table=s"               => \$use_table,
                        "show_table=s"              => \$show_table,
                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runFgenesh';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $inputFastaNodeId,      # inputFastaNodeId
                $par_file,              # par_file
                $GC_cutoff,             # -GC:
                $position_1,            # -p1:
                $position_2,            # -p2:
                $condensed,             # -c
                $exon_table,            # exon_table
                $exon_bonus,            # exon_bonus
                $pmrna,                 # pmrna
                $pexons,                # pexons
                $min_thr,               # min_thr
                $scp_prom,              # scp_prom
                $scp_term,              # scp_term
                $min_f_exon,            # min_f_exon
                $min_i_exon,            # min_i_exon
                $min_t_exon,            # min_t_exon
                $min_s_exon,            # min_s_exon
                $nvar,                  # nvar
                $try_best_exons,        # try_best_exons
                $try_best_sites,        # try_best_sites
                $not_rem,               # not_rem
                $vthr,                  # vthr
                $use_table,             # use_table
                $show_table,            # show_table
                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runFgenesh(@options);

