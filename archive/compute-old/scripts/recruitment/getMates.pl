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

my $list = shift;
my $lfh = FileUtil::openFileHandle($list);
while (<$lfh>) {
  chomp;
  my @d = split /\t/;
  $map{$d[5]} = 0;
}
$lfh->close();

my $file = shift;
my $fh = FileUtil::openFileHandle($file);
while (<$fh>) {
  if (/^>(\S+)/ && exists($map{$1})) {
    my $id = $1;
    my $src = "-";
    if (/\/src=(\S+)/) {
      $src = $1;
    }
    if (/mate=(\S+)/) {
      $map{$id} = "$1\t$src";
    } else {
      $map{$id} = "0\t$src";
    }
  }
}
$fh->close();

foreach my $id (keys %map) {
  next if $seen{$id};
  my $v = $map{$id};
  my @d = split /\t/,$v;
  $d[1] = "-" unless defined($d[1]);
  print "$id\t$d[0]\t$d[1]\n";
  print "$d[0]\t$id\t$d[1]\n";
  $seen{$id} = 1;
  $seen{$map{$id}} = 1;
}
