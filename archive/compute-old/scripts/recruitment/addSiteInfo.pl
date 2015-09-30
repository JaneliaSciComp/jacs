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

my $src = shift;
my $sfh = FileUtil::openFileHandle($src);
my $data = shift;
my $dfh = FileUtil::openFileHandle($data);
while (<$sfh>) {
  if (/\S+/) {
    chomp;
    my @d = split /\s+/;
    if ($d[0]) {
      $map{$d[0]} = $d[2];
      if ($d[1]) {
        $mate{$d[0]} = $d[1];
        $mate{$d[1]} = $d[0];
      } else {
        $mate{$d[0]} = "-";
      }
    }
    if ($d[1]) {
      $map{$d[1]} = $d[2];
      if ($d[0]) {
        $mate{$d[0]} = $d[1];
        $mate{$d[1]} = $d[0];
      } else {
        $mate{$d[1]} = "-";
      }
    }
  }
}
$sfh->close();

while (<$dfh>) {
  chomp;
  my @d = split /\t/;
  next unless exists($map{$d[5]});
  $d[22] = $map{$d[5]};
  $d[23] = $mate{$d[5]};
  my $l = join "\t",@d;
  print $l,"\n";
}
$dfh->close();

