#! /usr/bin/perl -w

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

use strict;
use SOAP::Lite;

my $VICS_URL = "http://centosgridtest-prod:8180/compute-compute/ComputeWS?wsdl";

# Put this in a file, and then refer to the file path
my $test_peptide_fasta = "/usr/local/projects/SIFT/zguan/testFasta/brca1ProteinTest.fasta";

if (! -e "$test_peptide_fasta") {
    die "Could not locate input peptide fasta file " . $test_peptide_fasta . "\n";
}

my $FASTA_NODE_ID=0;

my $uploadFastaOutput = SOAP::Lite
    -> service($VICS_URL)
    -> uploadFastaFileToSystem('zguan', 'token', '', $test_peptide_fasta);

if ($uploadFastaOutput=~/File id: (\S+)/) {
    $FASTA_NODE_ID=$1;
} else {
    die "Could not parse fasta upload output=" . $uploadFastaOutput . "\n";
}

my $SIFTPROTEINSUBSTITUTION_TASK_ID=0;
my $siftProteinSubstitutionOutput = SOAP::Lite
   -> service($VICS_URL)
   -> runSiftProteinSubstitution
                            ('zguan',                             # username
                             'token',                             # token
                             '0116',                              # project
                             '',                                  # sessionId
                             'siftProteinSubstitution test',      # job name
                             $FASTA_NODE_ID,                      # fasta input node id
                             'S4A');                               # substitution string

if ($siftProteinSubstitutionOutput=~/Job Id: (\S+)/) {
    $SIFTPROTEINSUBSTITUTION_TASK_ID=$1;
} else {
    die "Could not parse task id for siftProteinSubstitution from=" . $siftProteinSubstitutionOutput . "\n";
}

my $location=&waitForTask($SIFTPROTEINSUBSTITUTION_TASK_ID);

print "SiftProteinSubstitutionOutput result is in this directory=" . $location . "\n";

sub waitForTask {
    my $taskId=$_[0];
    my $status=0;
    my $location="";
    while ($status==0) {
       my $statusOutput = SOAP::Lite
             ->service($VICS_URL)
             ->getTaskStatus("zguan", "token", $taskId);
       if ($statusOutput=~/problem/) {
           die "There was a problem waiting for taskId=".$taskId." : ".$statusOutput . "\n";
       } elsif ($statusOutput=~/location: (\S+)/) {
           $location=$1;
           $status=1;
       } else {
           print "Status for taskId=".$taskId." : ".$statusOutput."\n";
           sleep(60);
       }
    }
    return $location;
}
