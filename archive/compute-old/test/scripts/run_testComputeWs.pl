#!/usr/local/perl

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

=head1 NAME

run_testComputeWs.pl - test a suite of web services known as ComputeWS for VICS.

=head1 SYNOPSIS

    USAGE: run_testComputeWs.pl

=head1 OPTIONS

=head1 DESCRIPTION

Sets up a new TestComputeWs object, then runs through a set of program executions
as directed by the parameters found in the passed in conf file.

NOTE THAT RIGHT NOW THIS SCRIPT IS HIGHLY EVOLVING.  There may, at various times,
be wierd phenomenon happening that wouldn't really belong in production code.

I<Caveat Emptor!>

=head1 INPUT

=head1 OUTPUT

=head1 CONTACT

 Jason Inman
 jinman@jcvi.org

=cut

use strict;
use warnings;
$|++;

use TestComputeWs;
use Getopt::Long;
use Pod::Usage;
use Cwd;

my $DEFAULT_CONF = abs_path('./testComputeWS.ini');
my $DEFAULT_NUC_BLASTDB = abs_path('./small_nuc_blastdb.fsa');
my $DEFAULT_PRO_BLASTDB = abs_path('./small_nuc_multi_prot.fsa');
my $DEFAULT_USER = getlogin;

my %opts;
my $username;
my $conf;

my $result = GetOptions(\%opts,
                        'conf_file=s',
                        'log_file=s',
                        'username|U=s',
                        'help|h',
                        ) || die "Error getting options!";
pod2usage( {-exitval => 0, -verbose => 2}) if ($opts{'help'});

&setup_options;

# set up a test object
my $tcw = new TestComputeWs;

# give it the config file
$tcw->config($conf);

# set up with a default username (for now)
my %params = ('username' => $username );
my ($output, $taskId); # these will be used/reused quite often.

# start running tests

### SETUP A WORK SESSION
$output = $tcw->submit('createWorkSession',\%params);
my $workSessionId = $1 if ($output =~ /Session Id: (\d+)/);
die "Can't get Session Id out of: '$output'\n" unless $workSessionId;
print "Work Session Id: $workSessionId\n";
$params{workSessionId} = $workSessionId;

#############               ###################
################    UPLOADS    ################
###################               #############

# upload some fastas, store ids in a tidy little hash
my %uploads;
print "\nRUNNING UPLOADANDFORMATBLASTDB (nuc blast db)\n";
$params{pathToFastaFile}    = $DEFAULT_NUC_BLASTDB;
$params{blastDBName}        = 'ws_test_db_nuc';
$params{blastDBdescription} = 'test db, small nucleotide';
$output = $tcw->submit('uploadAndFormatBlastDataset',\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /job (\d+)/);
$output = $tcw->get_job_status($taskId,'BlastDatabase');
print "$output\n" if $output;
$uploads{small_nuc_blastdb} = $1 if ($output =~ /\/(\d+)$/s);

print "\nRUNNING UPLOADFASTAFILETOSYSTEM (small nuc)\n";
$output = $tcw->submit('uploadFastaFileToSystem',\%params);
print "$output\n" if $output; 
$uploads{small_nuc_fasta} = $1 if ($output =~ /File id: (\d+)/);
print $uploads{small_nuc_fasta},"\n";


print "\nRUNNING UPLOADANDFORMATBLASTDB (prot blast db)\n";
$params{pathToFastaFile}    = $DEFAULT_PRO_BLASTDB;
$params{blastDBName}        = 'ws_test_db_prot';
$params{blastDBdescription} = 'test db, small protein';
$output = $tcw->submit('uploadAndFormatBlastDataset',\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /job (\d+)/);
$output = $tcw->get_job_status($taskId,'BlastDatabase');
print "$output\n" if $output;
$uploads{small_prot_blastdb} = $1 if ($output =~ /\/(\d+)$/s);

print "\nRUNNING UPLOADFASTAFILETOSYSTEM (small prot)\n";
$output = $tcw->submit('uploadFastaFileToSystem',\%params);
print "$output\n" if $output; 
$uploads{small_prot_fasta} = $1 if ($output =~ /File id: (\d+)/);
print $uploads{small_prot_fasta},"\n";

#############                           ###################
################   GETDATABASELOCATIONS    ################
###################                           #############
foreach my $db_type ('Blast','Hmmer','ReversePsiBlast') {
    print "\nRUNNING get$db_type"."DatabaseLocations\n";
    $output = $tcw->submit("get$db_type"."DatabaseLocations",\%params);
    print "$output\n" if $output;
}

print "\nRUNNING getSystemDatabaseIdByName\n";
$params{databaseName} = 'TINY';
$output = $tcw->submit('getSystemDatabaseIdByName',\%params);
print "$output\n" if $output;

#print map {"$_\t$uploads{$_}\n"} keys %uploads; exit;

=cut
#############              ###################
################    BLASTS    ################
###################              #############
# made a nice little loop here to do the blasts.
# made sure to accomodate various residue types per program.
foreach my $blast_type ('BlastN','MegaBlast','TBlastX','TBlastN','BlastP','BlastX') {
#my ($stuff, $order) = $tcw->get_config_options("run$blast_type");
#print map {"$_\t$stuff->{$_}\t$params{$_}\n"} keys %{$stuff}; exit;
    $params{subjectDBIdentifier} = ($blast_type =~ /BlastN|MegaBlast|TBlastX|TBlastN/) ?
                                    $uploads{small_nuc_blastdb} : $uploads{small_prot_blastdb};
    $params{queryFastaFileNodeId} = ($blast_type =~ /BlastN|MegaBlast|TBlastX|BlastX/) ?
                                    $uploads{small_nuc_fasta} : $uploads{small_prot_fasta};
    print "\nRUNNING ", uc($blast_type), "\n";
    $output = $tcw->submit("run$blast_type",\%params);
    print "$output\n" if $output;
    $taskId = $1 if ($output =~ /Job Id: (\d+)/);
    # get blast job status until it's either error or completed
    $result = $tcw->get_job_status($taskId,'Blast');
    print "$result\n" if $result;
}
=cut

#############                       ###################
################    REVERSEPSIBLAST    ################
###################                       #############
print "\nRUJNNING runReversePsiBlast\n";
$params{subjectDBIdentifier} = $uploads{small_prot_blastdb};
$params{queryFastaFileNodeId} = $uploads{small_prot_fasta};
#my ($stuff, $order) = $tcw->get_config_options("runReversePsiBlast");
#print map {"$params{$_}\n"} keys %{$stuff}; exit;
#print map {"$_\t$stuff->{$_}\t$params{$_}\n"} keys %{$stuff}; exit;
$output = $tcw->submit('runReversePsiBlast',\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /Job Id: (\d+)/);
$result = $tcw->get_job_status($taskId,'Blast');
print "$result\n" if $result;

#############             ###################
################    PRIAM    ################
###################             #############
print "\nRUNNING PRIAM\n";
$params{queryFastaFileNodeId} = $uploads{small_nuc_fasta};
$output = $tcw->submit("runPriam",\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /Job Id: (\d+)/);
$result = $tcw->get_job_status($taskId,'Priam');
print "$result\n" if $result;


#############               ###################
################    HMMPFAM    ################
###################               #############
print "\nRUNNING HMMPFAM\n";
$params{queryFastaFileNodeId} = $uploads{small_prot_fasta};
$output = $tcw->submit("runHmmpfam",\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /Job Id: (\d+)/);
$result = $tcw->get_job_status($taskId,'Hmmpfam');
print "$result\n" if $result;

#############                ###################
################    TRNASCAN    ################
###################                #############
# First, upload the test file:
print "\nRUNNING UPLOADFASTAFILETOSYSTEM\n";
$params{pathToFastaFile} = "/usr/local/devel/ANNOTATION/jinman/vics_ws_testing/small_Trna_test.fsa";
$output = $tcw->submit("uploadFastaFileToSystem",\%params);
print "$output\n" if $output;
$uploads{small_scan_nuc}= $1 if ($output =~ /File id: (\d+)/);
# Now submit the TrnaScan job:
print "\nRUNNING TRNASCAN\n";
$params{inputFastaFileNodeId} = $uploads{small_scan_nuc};
$output = $tcw->submit("runTrnaScan",\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /Job Id: (\d+)/);
$result = $tcw->get_job_status($taskId,'TrnaScan');
print "$result\n" if $result;

#############                ###################
################    RRNASCAN    ################
###################                #############
print "\nRUNNING runRrnaScan\n";
$params{inputFastaFileNodeId} = $uploads{small_nucc_fasta};
$output = $tcw->submit("runRrnaScan",\%params);
print "$output\n" if $output;
$taskId = $1 if ($output =~ /Job Id: (\d+)/);
$result = $tcw->get_job_status($taskId,'RrnaScan');
print "$result\n" if $result;

#############                     ###################
################   RUNORFCALLERS     ################
###################                     #############
foreach my $caller_type ('SimpleOrfCaller','MetaGenoOrfCaller','MetaGenoAnnotation',
                         'MetaGenoCombinedOrfAnno','Metagene') {
#my ($stuff, $order) = $tcw->get_config_options("run$blast_type");
#print map {"$_\t$stuff->{$_}\t$params{$_}\n"} keys %{$stuff}; exit;
    $params{inputFastaFileNodeId} = $uploads{small_nuc_fasta};
    print "\nRUNNING ", "run$caller_type", "\n";
    $output = $tcw->submit("run$caller_type",\%params);
    print "$output\n" if $output;
    $taskId = $1 if ($output =~ /Job Id: (\d+)/);
    # get blast job status until it's either error or completed
    $result = $tcw->get_job_status($taskId,$caller_type);
    print "$result\n" if $result;
}


#############              ###################
################    EXPORT    ################
###################              #############
print "\nRUNNING exportWorkFromSession\n";
$params{finalOutputDirectory} = "/usr/local/scratch/$params{username}/vics_output/";
$output = $tcw->submit("exportWorkFromSession",\%params);
print "$output\n" if $output;

#############              ###################
################    DELETE    ################
###################              #############
if ($opts{delete}) {
    print "\nRUNNING deleteTaskById\n";
    $output = $tcw->submit('deleteTaskById',\%params);
    print "$output\n" if $output;

    print "\nRUNNING deleteAllWorkForSession\n";
    $output = $tcw->submit("deleteAllWorkForSession",\%params);
    print "$output\n" if $output;
}

print "\nFINISHED RUNNING TESTS\n";

exit(0);

sub setup_options {

    $username = $opts{username}  || $DEFAULT_USER;
    $conf     = $opts{conf_file} || $DEFAULT_CONF;

    $opts{delete} = 1;

}
