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

# assumes that file is sorted with read id in column 6 and genome begin/end in columns 3 and 4 respectively.

my $file = shift;
my $fh = FileUtil::openFileHandle($file);
while (<$fh>) {
  my $d = $_;
  chomp;
  my $k;
  my @d = split /\t/;
  if (@l && $d[5] eq $l[5]) {
    if ($d[2] == $l[2]) {
      if ($d[3] < $l[3]) {
        $k = 1;
      } else {
        $k = 0;
      }
    } elsif ($d[3] == $l[3]) {
      if ($d[2] > $l[2]) {
        $k = 1;
      } else {
        $k = 0;
      }
    } else {
      $k = -1;
    }
    if ($k) {
      print $l;
      if ($k == 1) {
        @d = @l;
        $d = $l;
      }
    }
  } else {
    print $l;
  }
  $l = $d;
  @l = @d;
}
$fh->close();

