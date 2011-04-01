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

# THIS IS THE ORIGINAL SCRIPT FROM JEFF HOOVER

##!/usr/local/perl
#use strict;
#our $errorMessage;

#require "getopts.pl";
#use Cwd 'realpath';
#use File::Basename;

#my $program = realpath($0);
#my $myLib = dirname($program);
#push @INC, $myLib;
#require 'db.pm';
#require 'dataset.pm';

## get command line options
#my ( $eclist, $pnfile, $rulefile ) = &initialize;

## load ec definitions
#my %ec = {};
#open( EC, "<$eclist" );
#while ( my $line = <EC> ) {
#       $line =~ s/[\r\n]//g;
#       $line =~ s/\\/\\\\/g;
#       $line =~ s/\//\\\//g;
#       $line =~ s/"/\\"/g;
#       ( my $ecnum ) = split( /\s/, $line );
#       $ec{$ecnum} = $line;
##print "ecl: $line\n";
#}
#close( EC );

## process list of ASN files
#open( PRIAM, "<$pnfile" );
#while ( my $filename = <PRIAM> ) {
##print "fil: $filename\n";
#       $filename =~ s/[\r\n]//g;
##print "pnf: $filename\n";
#       my $profile = basename( $filename );
#       $profile =~ s/\.chk//;
##print "pro: $profile\n";
#       my $ecnum = $profile;
#       $ecnum =~ s/^\d*p//;
##print "ec#: $ecnum\n";
#       my $defline = $profile;
#       if ( exists $ec{$ecnum} ) {
#               $defline .= " | " . $ec{$ecnum};
#       } else {
#               $defline .= " | " . $ecnum;
#               print "profile $profile, unlisted ec#: $ecnum\n";
#       }
#
#       my @rules = split( /[\r\n]/, `grep "$profile" $rulefile` );
##print "gene rule=" . join( "\n", @rules ) . "\n";
#       my $gene_rule;
#       foreach my $rule ( @rules ) {
#               $rule =~ s/^/ /;
#               $rule =~ s/$/ /;
#               if ( index( $rule, " $profile " ) >= 0 ) {
#                       $gene_rule = $rule;
#                       $gene_rule =~ s/^\s+//;
#                       $gene_rule =~ s/\s+$//;
#                       last;
#               }
#       }
#       $defline .= " | $gene_rule";
#       if ( $gene_rule ne $profile ) {
#               print $defline . "\n";
#       }
#
##print "def: $defline\n";
## add defline to ASN file
#       my $cmd = "sed \"s/^\\(\\s*\\)\\(inst\\s*{\\)/\\1descr { title \\\"$defline\\\" } ,\\n\\1\\2/\" <$filename >$filename.upd";
##print "cmd: $cmd\n";
#       system($cmd);
#}
#system( "sed \"s/\\.chk/\\.chk\\.upd/\" <$pnfile >$pnfile.upd" );
#close( PRIAM );
#exit(0);
#
##############################################################
##
## read and validate commmand line parameters
#sub initialize {
#       use vars qw( $opt_E $opt_P $opt_R $opt_h );
#       &Getopts('E:P:R:h');
#
#       if ( $opt_h ) {
#               print
#"
#usage: ./annotatePriam.pl -E eclist -P pnfile -R rulesfile\"
#";
#               exit(0);
#       }
#
#       if ( !$opt_E ) {
#               die "\nYou must specify the file containing the ec list (-E).\n";
#       } elsif ( ! -e $opt_E ) {
#               die "\nSpecified file \"$opt_E\" does not exist (-E).\n";
#       }
#
#       if ( !$opt_P ) {
#               die "\nYou must specify the pn file pointing to the matrices (-P).\n";
#       } elsif ( ! -e $opt_P ) {
#               die "\nSpecified file \"$opt_P\" does not exist (-P).\n";
#       }
#
#       if ( !$opt_R ) {
#               die "\nYou must specify the rules file (-R).\n";
#       } elsif ( ! -e $opt_R ) {
#               die "\nSpecified rules file \"$opt_R\" does not exist (-R).\n";
#       }
#
#       return ( $opt_E, $opt_P, $opt_R );
#}

###################################################################################################
###################################################################################################
###################################################################################################

use strict;

my $ANNOTATION_DIR="../ANNOTATION";
my $CORR_FILE="$ANNOTATION_DIR\/profiles_corespondance_table.txt"; # incorrect spelling is norm for priam package
my $EC_FILE="$ANNOTATION_DIR\/..\/ec_list.txt";
my $DEFLINE_MAP_FILE="$ANNOTATION_DIR\/defline_map.txt";
my $ALIAS_FILE="$ANNOTATION_DIR\/profiles_alias.txt";
my %pri_to_corr_ec_hash;
my %ec_desc_hash;
my %defline_pri_hash;
my %alias_hash;

open(CORR, "<$CORR_FILE") || die "Could not open $CORR_FILE to read\n";
while(<CORR>) {
    my @arr=split /\s+/;
    if (! (defined $pri_to_corr_ec_hash{$arr[0]})) {
        my @carr=();
        push @carr, $arr[3];
        $pri_to_corr_ec_hash{$arr[0]}=\@carr;
    } else {
        my $carr_ref=$pri_to_corr_ec_hash{$arr[0]};
        push @$carr_ref, $arr[3];
        $pri_to_corr_ec_hash{$arr[0]}=$carr_ref;
    }
}
close(CORR);

open(ALIAS, "<$ALIAS_FILE") || die "Could not open file $ALIAS_FILE to read\n";
while(<ALIAS>) {
    my @arr=split /\s+/;
    my $primary=$arr[0];
    my $sec_string=$arr[1];
    my @secarr=split /\;/, $sec_string;
    foreach my $mem (@secarr) {
       $alias_hash{$mem}=$primary;
    }
}
close(ALIAS);

open(EC, "<$EC_FILE") || die "Could not open $EC_FILE to read\n";
while(<EC>) {
    /(\S+)\s+(.+)/;
    $ec_desc_hash{$1}=$2;
}
close(EC);

foreach my $key (keys %pri_to_corr_ec_hash) {
    my $carr_ref=$pri_to_corr_ec_hash{$key};
    my $defline;
    foreach my $ec_code (@$carr_ref) {
        my @code_arr=split 'p', $ec_code;
        my $desc=$ec_desc_hash{$code_arr[1]};
        $desc =~ s/\//\\\//g;
        if (defined $defline) {
            $defline.="\|$ec_code:$desc";
        } else {
            $defline="\\/ecinfo=$ec_code:$desc";
        }
    }
    $defline_pri_hash{$key}=$defline;
}

my $anno_dir_string=`ls $ANNOTATION_DIR`;
my @anno_files=split /\s+/, $anno_dir_string;
open(DEFLINE, ">$DEFLINE_MAP_FILE") || die "Could not open $DEFLINE_MAP_FILE to write\n";
foreach my $file (@anno_files) {
    if ($file=~/(.+)\.chk/) {
        my $pri_name=$1;
        my $full_prefix="$ANNOTATION_DIR\/$pri_name";
        my $full_path="$ANNOTATION_DIR\/$file";
        my $defline=$defline_pri_hash{$pri_name};
        if (! (defined $defline) ) {
            my $alias=$alias_hash{$pri_name};
            if (! (defined $alias)) {
                die "Could not find alias for primary name $pri_name\n";
            }
            $defline=$defline_pri_hash{$alias};
        }
        my $cmd = "sed \"s/^\\(\\s*\\)\\(inst\\s*{\\)/\\1descr { title \\\"$defline\\\" } ,\\n\\1\\2/\" < $full_path > $full_prefix.upd";
        print "cmd: $cmd\n";
        system($cmd);
        $defline =~ s/\\\//\//g;
        print DEFLINE "$pri_name\t$defline\n";
    }
}
close(DEFLINE);

