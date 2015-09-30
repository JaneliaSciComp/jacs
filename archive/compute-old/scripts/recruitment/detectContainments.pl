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

my $file = shift;
my $flap = shift;
my $overlap = shift;
my $pid = shift;
my $fh = FileUtil::openFileHandle($file);
while (<$fh>) {
  my $l = $_;
  chomp;
  my @d = split /\t/;
  next if $d[0] eq $d[5];
  next if ($d[12] - $d[13]) < $overlap;
  next if ($d[10] + $d[17]) / $d[12] < $pid;
  my $qcontained = $d[2] < $flap && ($d[15] - $d[3]) < $flap;
  my $scontained = $d[6] < $flap && ($d[16] - $d[7]) < $flap;
  next unless $qcontained || $scontained;
  print $l;
}
$fh->close();
