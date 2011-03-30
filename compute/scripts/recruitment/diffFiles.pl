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

my $file1 = shift;
my $file2 = shift;

### get the lines from file2 that are not in file1
### assumes that order of lines in file1 and file2
### are the same

my $fh1 = FileUtil::openFileHandle($file1);
my $fh2 = FileUtil::openFileHandle($file2);
my $line1 = <$fh1>;
my $line2 = <$fh2>;
while (defined($line2)) {
  if ($line1 eq $line2) {
    $line1 = <$fh1>;
    $line2 = <$fh2>;
  } else {
    print $line2;
    $line2 = <$fh2>;
  }
}
$fh1->close();
$fh2->close();
