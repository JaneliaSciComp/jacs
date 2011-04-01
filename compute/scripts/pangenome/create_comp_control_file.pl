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

use warnings;
use strict;
$|++;

#TODO
# 1.  add fancier (?) type determining (prok, ntprok, euk)
# 2.  pod.  especially stating the very specific use caseage.
use DBI;
use Getopt::Long qw(:config no_ignore_case pass_through);
use Pod::Usage;
use Data::Dumper;

my %opts = ();
my $server = 'SYBTIGR';
my %info = ();

GetOptions(\%opts,  'db_list|d=s',
                    'server|s=s',
                    'control_file|c=s',
                    'help|h',
                    'user|U=s',
                    'pass|P=s',
            );

if ($opts{'help'}) {
    print "$0\n".
        "\tdb_list, d:      list of dbs to work on.\n".
        "\tcontrol_file, c: name of the new control_file.\n".
        "\tserver, s:       SYBASE server hosting the dbs.\n".
        "\tuser, U:     username to login as\n",
        "\tpass, P:     password to login with\n";
    exit(0);
}
pod2usage( {-exitval => 0}) if (defined $opts{help});

# Some minor but necessary validations:
die "No db passed.  Use --db_list\n" unless (defined $opts{db_list});
die "No output file passed.  Use --control_file.\n" unless defined $opts{control_file};

# Set up the list of projects:
if ($opts{db_list}) {
    open (DBS,"< $opts{db_list}") || die "Can't open $opts{db_list}! $!\n";
    while (<DBS>) {
    	chomp;
        my ($db,$type) = split(/\t/,$_);
        $info{$db} = $type;
    }
    close DBS;
} else {
    die "Nuh uh, really?  How'd you get here?\n";
}
die "Empty input db list!\n" if (scalar (keys %info) == 0);

# Set up output file.  Use append mode if we're using a single db name.
open (CTL_FILE, "> $opts{control_file}") || die "Can't open $opts{control_file}! $!\n";

# Set up the connection to the server:
$server = $opts{server} if defined $opts{server};
my $dbh = DBI->connect("dbi:Sybase:$server", $opts{user} || "access", $opts{pass} || "access",
                       {RaiseError => 1, PrintError => 1}) ||
    die "Error connecting to server $server as access.\n";

# Create the control file now
foreach my $project (keys %info) {

    # 'use' the project
    $dbh->do("use $project") || die "Couldn't change context on $server to $project! $!\n";

    my $org_type = $info{$project};
    print CTL_FILE "database:$project organism_type:$org_type include_genefinders: exclude_genefinders:\n";

    # setup and run the asmbl_id query
    my $asmbl_query;
    if ($org_type eq 'euk') {
        $asmbl_query = $dbh->prepare("SELECT asmbl_id FROM clone_info WHERE is_public = 1");
    } else {
        $asmbl_query = $dbh->prepare("SELECT asmbl_id FROM stan WHERE iscurrent = 1");
    }

    my $asmbl_id;
    $asmbl_query->execute();
    $asmbl_query->bind_columns(\$asmbl_id);
    my $rows = 0;
    while ( $asmbl_query->fetch ) {
        print CTL_FILE "$asmbl_id\n";
        $rows++;
    }
    print "Found $rows asmbls for $project\n";

}

close CTL_FILE;
$dbh->disconnect();
exit(0);
