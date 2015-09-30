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

mate_check.pl - assign mate pair status to individual fragments from a layout of a set of interest given 1) a 4-column layout (UID begin end isRev) OR a btab file, and 2) a list of mate pairs (UID1 UID2)

=head1 VERSION

This document refers to version 1.00 of mate_check.pl, released 09.28.2004.

=head1 SYNOPSIS

mate_check.pl [-mingood <int>] [-maxgood <int>] -layout <filename> -mates <filename>

Flags:
    
    -layout <filename>    File containing fragment layout.
    -site  <filename>     File containing mates.
    -btab                 Layout file is btab format
    -help|h               Print help message

=head1 DESCRIPTION

=head2 Overview

Assigns mate pair status (one of 16 values) to individual fragments from a layout of a set of interest given a 4-column layout (UID begin end isRev) and a list of mate pairs (UID1 UID2)

Status values are as follows:

                   frgOrient
                  fwd   rev
has no mate        5     6
too close          3     4       (with --->  <--- orientation)
good               1     2       (with --->  <--- orientation)
too far, right ori 7     8       (with --->  <--- orientation)
missing mate       15    16

                   order of frg
                   first second
anti oriented      9     10        (i.e. --->   --->)
normal orient      11    12        (i.e. --->   --->)
outie oriented     13    14        (i.e. <---   --->)


 Proposed 8-color coloration of these codes:

  {1,2} = green
  {3,4} = yellow
  {5,6} = black (or white)
  {7,8} = red
  15    = dark blue
  16    = cyan
  {9,10,13} = orange
  {11,12,14} = purple

=head2 Credit

Hacked by Aaron Halpern. Modified by Doug Rusch 5/18/2006.

=cut

use Pod::Usage;
#use FileUtil;
use Getopt::Long qw(:config no_ignore_case no_auto_abbrev);

my %options = ();
my $prog = $0;
$prog =~ s/.*\///;
my ($help,$man,$mingood,$maxgood,$layoutfile,$matefile);
my %siteMin;
my %siteMax;
my $btabinput;

BEGIN: {
  GetOptions(
    "help|h"      => \$help,
    "layout|l=s"  => \$layoutfile,
    "btab"        => \$btabinput,
    "site=s"        => \$siteInfo) || pod2usage(2);
  pod2usage(1) if defined($help) || !defined($layoutfile) || !defined($siteInfo);
}


my $frgs=0;

$btabinput = 0 if !defined($btabinput);

#my $sfh = FileUtil::openFileHandle($siteInfo);
#while (<$sfh>) {
open (INFILE1, $siteInfo)|| die "could not  $siteInfo open for reading\n";
while(<INFILE1>){
  chomp;
  my @d = split /\t/;
  $siteMin{$d[3]} = $d[6];
  $siteMax{$d[3]} = $d[7];
}
#$sfh->close();
close($siteFile);
#my $lfh = FileUtil::openFileHandle($layoutfile);
#while(<$lfh>) {
open (INFILE2, $layoutfile)|| die "could not open for reading\n";
while(<INFILE2>){
  my @d = split /\t/;
  $id = $d[5];
  $beg{$id} = $d[2];
  $end{$id} = $d[3];
  if ($d[8] ==1){$d[8]=0;}
  
  $rev{$id} = $d[8];
  
  if ($rev{$id}){
      $status{$id}=6; # this can be overwritten below; this means has no mate
  } else {
      $status{$id}=5;
  }

  $id[$frgs]=$id;
  $count{$id}++;
  $frgs++;
}
#$lfh->close();
close($layoutfile);
open (INFILE3, $layoutfile)|| die "could not open for reading\n";
while(<INFILE3>) {
#$lfh = FileUtil::openFileHandle($layoutfile);
#while(<$lfh>) {
  chomp;
  my @w = split /\t/;
  my $m1 = $w[5];
  my $m2 = $w[23];
  next if $m2 eq "-";
  my $site = $w[22];
  if (defined($end{$m1})) {
  	$a = 1;
  } else {
  	$a = 0;
  }
  if (defined($end{$m2})) {
	  $b = 1;
  }else{
	  $b = 0;
  }
  
#  print "$m1 $m2 $site $a $b\n";
  if ($a==1 && $b==1) {
  	if ($beg{$m1} <= $beg{$m2}) {
	    $x = $m1;
	    $y = $m2;
  	} else {
	    $x = $m2;
	    $y = $m1;
	  }
#    print "yyy $m1 $m2 $rev{$x} $rev{$y}\n";
	  if ($rev{$x} == 0) {
	    if ($rev{$y} == 0) {
		    $ori = "AA";
		    $xc = 11;
		    $yc = 12;
	    } else {
		    $ori = "AB";
        my @pos;
        push @pos,$beg{$x},$end{$x},$beg{$y},$end{$y};
		    $sep= getMax(\@pos) - getMin(\@pos);
#        print "$m1 $m2 $sep $beg{$x} $end{$x} $beg{$y} $end{$y}\n";
    		if ($ori == "AB") {
  		    if ($sep < $siteMin{$site}) {
      			$diststat = "too close";
			      $xc = 3;
      			$yc = 4;
		      } elsif ($sep <= $siteMax{$site}) {
		      	$diststat = "good separation";
			      $xc = 1;
      			$yc = 2;
		      } else {
			      $diststat = "too far";
			      $xc = 7;
			      $yc = 8;
		      }
		    }
	    } 
	  } else {
	    if ($rev{$y} == 0) {
    		$ori = "BA";
		    $xc = 13;
		    $yc = 14;
	    } else {
		    $ori = "BB";
		    $xc = 9;
		    $yc = 10;
	    } 
	  }
	
	  if ($count{$x} > 1 || $count{$y} > 1) {
  	  $xc+=16;
	    $yc+=16;
	  }

  } else {
	  $x=$w[5];
	  $y=$w[24];
	  if ($a == 1) {
	    # 2nd of pair is missing from set of interest, but exists
	    if($rev{$x}){
    		$xc = 16;
	    } else {
		    $xc = 15;
	    }
	    $yc=-1;
#		   print "Missed mate for $x\n";
	  } 
	  if ($b == 1) {
	    # first of pair is missing from set of interest, but exists
	    if ($rev{$y}) {
    		$yc = 16;
	    } else {
    		$yc = 15;
	    }
	    $xc=-1;
#		   print "Missed mate for $y\n";
	  }
  }
  $status{$x}=$xc;
  $status{$y}=$yc;
#	print "setting status{$x} = $status{$x}\tstatus{$y} = $status{$y}\n";
}
#$lfh->close();
close($layoutfile);
open (INFILE4, $layoutfile)|| die "could not open for reading\n";
while(<INFILE4>) {
#$lfh = FileUtil::openFileHandle($layoutfile);
#while(<$lfh>) {
  chomp;
  my @d = split /\t/;
  $d[24] = $status{$d[5]};
  my $l = join "\t",@d;
  print $l,"\n";
}
#$lfh->close();
close($layoutfile);
sub getMin {
  my $l = shift;
  
  my $min;
  foreach my $i (@{$l}) {
    $min = $i if $i < $min || !defined($min);
  }
  
  return $min;
}

sub getMax {
  my $l = shift;
  
  my $max;
  foreach my $i (@{$l}) {
    $max = $i if $i > $max || !defined($max);
  }
  
  return $max;
}

exit(0);
