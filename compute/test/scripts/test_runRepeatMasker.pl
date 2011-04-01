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

test_runRepeatMasker.pl - test the runRepeatMasker web-service call.

=head1 SYNOPSIS

    USAGE: test_runRepeatMasker.pl OPTION LIST HERE

=head1 OPTIONS

NOTE:  This script deviates slightly from the typical connection parameter options.
RepeatMasker has an -s option already, so the alias for --server is -S as opposed to
the commonly seen -s in this test suite.

=head2 connection parameters

 --port, -p     The port on the server to connect to [81]
 --server, -S   The server on which to connect [saffordt-ws1]
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

 --nolow
 --noint
 --norna
 --alu
 --div
 --lib
 --cutoff
 --species
 --is_only
 --is_clip
 --no_is
 --rodspec
 --primspec
 --wublast
 --s
 --q
 --qq
 --gc
 --gccalc
 --frag
 --maxsize
 --nocut
 --noisy
 --ali
 --inv
 --cut
 --small
 --xsmall
 --x
 --poly
 --ace
 --gff
 --u
 --xm
 --fixed
 --no_id
 --excln
 
=head1  DESCRIPTION

This script tests the runRepeatMasker web service.  For the most part, the
options reflect those in the RepeatMasker executable with a few exceptions:

1) No -parallel option is implemented.  We don't want this nice program to run
wild on the grid, now, do we?

2) The menagerie of species flags has been replaced by a species option that
takes any of those flags as it's value.  Default is no flag, which specifies
primates.

3) Some options have been ignored since they are merely synonyms for other options.

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
my $jobName = 'TestRepeatMasker';
my $inputFastaNodeId = '1438285098964224171';

my $nolow       = '';
my $noint       = '';
my $norna       = '';
my $alu         = '';
my $div         = '';
my $lib         = '';
my $cutoff      = '';
my $species     = '';
my $is_only     = '';
my $is_clip     = '';
my $no_is       = '';
my $rodspec     = '';
my $primspec    = '';
my $wublast     = '';
my $s           = '';
my $q           = '';
my $qq          = '';
my $gc          = '';
my $gccalc      = '';
my $frag        = '';
my $maxsize     = '';
my $nocut       = '';
my $noisy       = '';
my $ali         = '';
my $inv         = '';
my $cut         = '';
my $small       = '';
my $xsmall      = '';
my $x           = '';
my $poly        = '';
my $ace         = '';
my $gff         = '';
my $u           = '';
my $xm          = '';
my $fixed       = '';
my $no_id       = '';
my $excln       = '';

my $result = GetOptions("port|p=i"      =>  \$port,
                        "server|S=s"    =>  \$server,
                        "wsdl_name=s"   =>  \$wsdl_name,
                        "wsdl_path=s"   =>  \$wsdl_path,
                        "wsdl_url=s"    =>  \$wsdl_url,

                        "username=s"                =>  \$username,
                        "project=s"                 =>  \$project,
                        "workSessionId=s"           =>  \$workSessionId,
                        "jobName=s"                 =>  \$jobName,
                        "inputFastaNodeId=s"        =>  \$inputFastaNodeId,

                        "nolow=s"		=> \$nolow,
                        "noint=s"		=> \$noint,
                        "norna=s"		=> \$norna,
                        "alu=s" 		=> \$alu,
                        "div=s"	    	=> \$div,
                        "lib=s"		    => \$lib,
                        "cutoff=s"		=> \$cutoff,
                        "species=s"		=> \$species,
                        "is_only=s"		=> \$is_only,
                        "is_clip=s"		=> \$is_clip,
                        "no_is=s"		=> \$no_is,
                        "rodspec=s"		=> \$rodspec,
                        "primspec=s"	=> \$primspec,
                        "wublast=s"		=> \$wublast,
                        "s=s"   		=> \$s,
                        "q=s"	    	=> \$q,
                        "qq=s"		    => \$qq,
                        "gc=s"		    => \$gc,
                        "gccalc=s"		=> \$gccalc,
                        "frag=s"		=> \$frag,
                        "maxsize=s"		=> \$maxsize,
                        "nocut=s"		=> \$nocut,
                        "noisy=s"		=> \$noisy,
                        "ali=s" 		=> \$ali,
                        "inv=s"	    	=> \$inv,
                        "cut=s"		    => \$cut,
                        "small=s"		=> \$small,
                        "xsmall=s"		=> \$xsmall,
                        "x=s"		    => \$x,
                        "poly=s"		=> \$poly,
                        "ace=s" 		=> \$ace,
                        "gff=s"	    	=> \$gff,
                        "u=s"		    => \$u,
                        "xm=s"		    => \$xm,
                        "fixed=s"		=> \$fixed,
                        "no_id=s"		=> \$no_id,
                        "excln=s"		=> \$excln,

                        );

$wsdl_url = "http://$server:$port/$wsdl_path/$wsdl_name?wsdl" unless $wsdl_url;

my $program = 'runRepeatMasker';
my @options = ( $username,              # username
                '',                     # token
                $project,               # project
                $workSessionId,         # workSessionId
                $jobName,               # jobName
                $inputFastaNodeId,      # inputFastaNodeId

				$nolow, 				# nolow
				$noint,	    			# noint
				$norna,		    		# norna
				$alu,			    	# alu
				$div,				    # div
				$lib,   				# lib
				$cutoff,                # cutoff
				$species,				# species
				$is_only,				# is_only
				$is_clip,				# is_clip
				$no_is, 				# no_is
				$rodspec,				# rodspec
				$primspec,				# primspec
				$wublast,				# wublast
				$s, 	    			# s
				$q,	    	    		# q
				$qq,			    	# qq
				$gc,				    # gc
				$gccalc,				# gccalc
				$frag,  				# frag
				$maxsize,				# maxsize
				$nocut,	    			# nocut
				$noisy,		    		# noisy
				$ali,			    	# ali
				$inv,				    # inv
				$cut,   				# cut
				$small,	    			# small
				$xsmall,				# xsmall
				$x,			        	# x
				$poly,				    # poly
				$ace,   				# ace
				$gff,	    			# gff
				$u,			        	# u
				$xm,				    # xm
				$fixed, 				# fixed
				$no_id,	    			# no_id
				$excln,		    		# excln

                );

print "$wsdl_url\n$program\n@options\n";
print SOAP::Lite
    -> service($wsdl_url)
    -> runRepeatMasker(@options);

