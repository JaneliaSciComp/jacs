#!/usr/bin/perl

use strict;

my $archiveScreenDir = "/archive/flylight_archive/screen/SecData/registrations";
my $stagingDir = "/nrs/jacs/jacsData/filestore/system/ScreenStaging";
my $v3d = "/home/murphys/vaa3d/v3d/vaa3d";

my @dateBasedDirArr;

my @archiveContents = split /\s+/, `ls $archiveScreenDir`;

foreach my $dirName (@archiveContents) {
    if (-d "$archiveScreenDir\/$dirName") {
	push @dateBasedDirArr, "$archiveScreenDir\/$dirName";
    }
}

foreach my $archiveDirPath (@dateBasedDirArr) {
    print "Processing $archiveDirPath\n";
    $archiveDirPath=~/(.+)\/(\S+)$/;
    my $dateDir=$2;
    my $stagingDirPath="$stagingDir\/$dateDir";
    if (! -d $stagingDirPath) {
	my $createStagingDirCmd="mkdir $stagingDirPath";
	print "$createStagingDirCmd\n";
	system($createStagingDirCmd);
    }
    my @archiveFiles = split /\s+/, `ls $archiveDirPath`;
    foreach my $archiveFile (@archiveFiles) {
	print "Processing archive file $archiveFile\n";
	my $archivePath="$archiveDirPath\/$archiveFile";
	if ($archivePath=~/(.+\S)\/(\S.+)(\.reg\.local\.raw)$/) {
	    print "Found raw file $archivePath\n";
	    my $archivePathPrefix=$1;
	    my $filePrefix=$2;
	    my $qualityPath="$archivePathPrefix\/$filePrefix\.loop2\.raw\_matching\_quality\.csv";
	    if (-e $qualityPath) {
		print "Also found quality file=$qualityPath\n";
		my $stagingPath="$stagingDirPath\/$filePrefix\.reg\.local\.v3dpbd";
		my $stagingQualityPath="$stagingDirPath\/$filePrefix\.quality\.csv";
		print "Using stagingPath=$stagingPath   qualityPath=$stagingQualityPath\n";
		if (! -e $stagingPath) {
		    my $v3dCmd="$v3d -cmd image-loader -convert8 $archivePath $stagingPath";
		    print "$v3dCmd\n";
		    system($v3dCmd);
		} else {
		    print "Already found $stagingPath\n";
		}
		if (! -e $stagingQualityPath) {
		    my $qualityCpCmd="cp $qualityPath $stagingQualityPath";
		    print "$qualityCpCmd\n";
		    system($qualityCpCmd);
		} else {
		    print "Already found $stagingQualityPath\n";
		}
	    } else {
		print "Could not find quality file $qualityPath - skipping this entry\n";
	    }
	} else {
	    print "File $archivePath is not considered a raw file of interest\n";
	}
    }
}
