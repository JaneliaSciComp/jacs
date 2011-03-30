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

use FileUtil;

my $goodFile = shift;
my $mateFile = shift;
my $missedFile = shift;

my $afh = FileUtil::openFileHandle($mateFile);
while (<$afh>) {
  chomp;
  my @d = split /\s+/;
  if ($d[1] && $d[0]) {
    $map{$d[0]} = $d[1];
    $map{$d[1]} = $d[0];
  }
}
$afh->close();
print STDERR "Done with mates\n";

my $gfh = FileUtil::openFileHandle($goodFile);
while (<$gfh>) {
  chomp;
  my @d = split /\s+/;
  $good{$d[5]}{$d[0]} = 1;
}
$gfh->close();
print STDERR "Done with good hits\n";

my $sfh = FileUtil::openFileHandle($missedFile);
while (<$sfh>) {
  my $l = $_;
  chomp;
  my @d = split /\s+/;
  if (!exists($good{$d[5]}) && exists($good{$map{$d[5]}})) {
    if (exists($good{$map{$d[5]}}{$d[0]})) {  ### if the mate exists on the same seq
      my $delta = $d[6] + ($d[16] - $d[7]);
      print $l if $delta / $d[16] < 0.2;
      $good{$d[5]} = 1;
    }
  }
}
$sfh->close();
