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
use LogUtil;

### Input parameters
my $workDir = shift;
my $sourcePath = shift;
my $localDir = shift;

### Global constants
my $QUOTE = chr(39);

my @removeList;
my $cmd;
$cmd = "mkdir -p $localDir";
runSystem($cmd);

### sort and remove duplicate/contained hits
$cmd = "find $sourcePath/blastData -name $QUOTE*.bz2$QUOTE | xargs -i bunzip2 -c {} | sort -u -T $localDir/ | sort -k 6,6 -k 3,3n -T $localDir/ | /usr/local/perl $workDir/removeOverlappingDups.pl - | sort -u -T $localDir | sort -k 6,6 -k 3,3n -T $localDir | bzip2 -c > $sourcePath/nr.blast.result.bz2";
push @removeList,"$sourcePath/nr.blast.result.bz2";
runSystem($cmd);

$cmd = "/usr/local/perl $workDir/detectContainments.pl $sourcePath/nr.blast.result.bz2 25 300 0.3 | bzip2 -c > $sourcePath/easyContained.hits.bz2";
push @removeList,"$sourcePath/easyContained.hits.bz2";
runSystem($cmd);

$cmd = "/usr/local/perl diffFiles.pl $sourcePath/easyContained.hits.bz2 $sourcePath/nr.blast.result.bz2 | bzip2 -c > $sourcePath/initiallyBad.result.bz2";
push @removeList,"$sourcePath/initiallyBad.result.bz2";
runSystem($cmd);

$cmd = "/usr/local/perl $workDir/detectContainments.pl $sourcePath/initiallyBad.result.bz2 20 100 0.3 | bzip2 -c > $sourcePath/smallContained.hits.bz2";
push @removeList,"$sourcePath/smallContained.hits.bz2";
runSystem($cmd);

$cmd = "bunzip2 -c $sourcePath/easyContained.hits.bz2 $sourcePath/smallContained.hits.bz2 | /usr/local/perl $workDir/getMates.pl - $workDir/all.headers.gz | bzip2 -c > $sourcePath/mates.list.bz2";
runSystem($cmd);

$cmd = "bunzip2 -c $sourcePath/easyContained.hits.bz2 $sourcePath/smallContained.hits.bz2 | /usr/local/perl $workDir/findOkMissedMates.pl - $sourcePath/mates.list.bz2 $sourcePath/initiallyBad.result.bz2 | bzip2 -c > $sourcePath/rescuedMate.hits.bz2";
push @removeList,"$sourcePath/rescuedMate.hits.bz2";
runSystem($cmd);

$cmd = "bunzip2 -c $sourcePath/easyContained.hits.bz2 $sourcePath/smallContained.hits.bz2 $sourcePath/rescuedMate.hits.bz2 | sort -u | bzip2 -c > $sourcePath/combined.hits.bz2";
push @removeList,"$sourcePath/combined.hits.bz2";
runSystem($cmd);

$cmd = "/usr/local/perl $workDir/addSiteInfo.pl $sourcePath/mates.list.bz2 $sourcePath/combined.hits.bz2 | bzip2 -c > $sourcePath/combinedPlusSite.hits.bz2";
push @removeList,"$sourcePath/combinedPlusSite.hits.bz2";
runSystem($cmd);

$cmd = "/usr/local/perl $workDir/mate_check.pl -site $workDir/allPlusPhase2.info -layout $sourcePath/combinedPlusSite.hits.bz2 -btab | sort -T $localDir -k 3,3n | bzip2 -c > $sourcePath/combinedPlusSitePlusMate.hits.bz2";
runSystem($cmd);
push @removeList,"$sourcePath/newData.flag";

foreach my $f (@removeList) {
  if (-e $f) {
    $cmd = "rm $f";
    runSystem($cmd);
  }
}

sub runSystem {
  my ($cmd,$message) = @_;

  my $error;
  print STDERR $cmd,"\n";
  if ($error = system($cmd)) {
    exit 1;
  }
}
