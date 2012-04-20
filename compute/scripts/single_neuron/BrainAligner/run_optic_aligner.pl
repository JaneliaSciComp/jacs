#!/usr/local/bin/perl

use Getopt::Std;
use strict;

our ($opt_v, $opt_b, $opt_l, $opt_t, $opt_w, $opt_i);

getopts("v:b:l:t:w:i:") || &usage("");

my $v3d         = $opt_v;
my $ba          = $opt_b;
my $templateDir = $opt_t;
my $workingDir  = $opt_w;
my $inputStack  = $opt_i;

if (! -e $v3d) {
    &usage("Could not locate v3d progam at location $v3d");;
}

if (! -e $ba) {
    &usage("Could not locate brain_aligner program at location $ba");
}

if (! -d $templateDir) {
    &usage("Could not locate template directory at location $templateDir");
}

if (! -d $workingDir) {
    `mkdir $workingDir`;
    sleep(2);
    if (! -d $workingDir) {
	&usage("Could not find nor create working directory $workingDir");
    }
}

if (! -e $inputStack) {
    &usage("Could not locate input stack $inputStack");
}

&runInitialGlobalAlignment($inputStack);
&opticLocalAlignment($inputStack);
&opticLocalAlignment2($inputStack);
&generateOutputFiles($inputStack);
&cleanup($inputStack);

exit;

#######################################################################################################

sub usage {
    print STDERR $_[0] . "\n";
    die "Usage: -v <v3d exe path> -b <brain_aligner path> -t <template dir> -w <working dir> -i <input stack> \n";
}

sub sanitize {
    my $s = shift;
    $s =~ s/\n/ /g;
    $s =~ s/\s+/ /g;
    return $s;
}

sub getBaseNameFromFile {
    my $file=$_[0];
    my @arr=split /\//, $file;
    my $baseName=$arr[@arr-1];
    my @arr2=split /\./, $baseName;
    my $baseName2=$arr2[0];
    return $baseName2;
}

sub runInitialGlobalAlignment {
    print "Start runInitialGlobalAlignment\n";
    my $inputFile=$_[0];
    print "inputFile=$inputFile\n";
    my $baseName=&getBaseNameFromFile($inputFile);
    print "baseName=$baseName\n";
    my $outputFileBase="$workingDir\/$baseName";
    print "outputFileBase=$outputFileBase\n";
    my $logFile="$workingDir\/initialGlobalAlignment.log";

# 1) global alignment: 
# #    ./vaa3d -x imagereg.so -f rigidreg -o <output_global_aligned_image> -p “#t <target> #ct <reference_channel> #s <subject> #cs <reference_channel>”
#
# $V3D_PATH -x imagereg.so -f rigidreg -o $OUTPUT_DIR/GlobalAligned.v3draw -p "#t $TEMPLATE_DIR/global/ave_target1_rl.raw #ct 1 #s $INPUT_FILE #cs 4";
#
# $V3D_PATH -x refExtract.so -f refExtract -i $OUTPUT_DIR/GlobalAligned.v3draw -o "$OUTPUT_DIR/GlobalAlignedReference_8bit.v3draw" -p "#c 4";

    my $cmd = sanitize(qq{
        $v3d
        -x imagereg.so -f rigidreg 
        -o "$outputFileBase\_GlobalAligned.v3draw"
        -p "#t $templateDir/global/ave_target1_rl.raw #ct 1 #s $inputFile #cs 4"
    });

    print "cmd=$cmd\n";
    system( "$cmd 1>$logFile 2>&1" );

    $cmd = sanitize(qq{
        $v3d
        -x refExtract.so -f refExtract 
        -i "$outputFileBase\_GlobalAligned.v3draw"
        -o "$outputFileBase\_GlobalAlignedReference_8bit.v3draw" 
        -p "#c 4"
    });

    print "cmd=$cmd\n";
    system( "$cmd 1>>$logFile 2>&1" );
}

sub opticLocalAlignment {
    print "Start opticLocalAlignment\n";
    my $inputFile=$_[0];
    print "inputFile=$inputFile\n";
    my $baseName=&getBaseNameFromFile($inputFile);
    print "baseName=$baseName\n";
    my $outputFileBase="$workingDir\/$baseName";
    print "outputFileBase=$outputFileBase\n";
    my $logFile="$workingDir\/opticLocalAlignment.log";

# (a) ./brainaligner -t <target> -s <output_global_aligned_image_referencechannel_8bit> -w 10 -o <output1> -L <target.marker>
#
#$BRAINALIGN_PATH -t "$TEMPLATE_DIR/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif" -s "$OUTPUT_DIR/GlobalAlignedReference_8bit.v3draw" -o "$OUTPUT_DIR/LocalAlignedReference_8bit.v3draw" -w 10 -L "$TEMPLATE_DIR/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif.marker"
    
    my $cmd = sanitize(qq{
        $ba 
        -t "$templateDir/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif" 
        -s "$outputFileBase\_GlobalAlignedReference_8bit.v3draw" 
        -o "$outputFileBase\_LocalAlignedReference_8bit.v3draw" 
        -w 10 
        -L "$templateDir/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif.marker"
    });

    print "cmd=$cmd\n";
    system( "$cmd 1>$logFile 2>&1" );
}


sub opticLocalAlignment2 {
    print "Start opticLocalAlignment2\n";
    my $inputFile=$_[0];
    print "inputFile=$inputFile\n";
    my $baseName=&getBaseNameFromFile($inputFile);
    print "baseName=$baseName\n";
    my $outputFileBase="$workingDir\/$baseName";
    print "outputFileBase=$outputFileBase\n";
    my $logFile="$workingDir\/opticLocalAlignment2.log";

# (b) ./brainaligner -t <target> -s <output_global_aligned_image> -w 10 -o <final_output_warped_image> -L <output1_target.csv> -l <output1_subject.csv>
#
# $BRAINALIGN_PATH -t "$TEMPLATE_DIR/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif" -s $OUTPUT_DIR/GlobalAligned.v3draw -o $OUTPUT_DIR/LocalAligned.v3draw -w 10 -L $OUTPUT_DIR/LocalAligned.v3draw_target.csv -l $OUTPUT_DIR/LocalAligned.v3draw_subject.csv

    my $cmd = sanitize(qq{
        $ba 
        -t "$templateDir/local/target_C1-110506_Slide1_110411_57C10_pos15_ROL__L5_Sum.tif.8bit.tif" 
        -s $outputFileBase\_GlobalAligned.v3draw 
        -o $outputFileBase\_LocalAligned.v3draw 
        -w 10 
        -L $outputFileBase\_LocalAligned.v3draw_target.csv 
        -l $outputFileBase\_LocalAligned.v3draw_subject.csv
    });

    print "cmd=$cmd\n";
    system( "$cmd 1>$logFile 2>&1" );
}

sub generateOutputFiles {
    print "Start generateOutputFiles\n";
    my $inputFile=$_[0];
    print "inputFile=$inputFile\n";
    my $baseName=&getBaseNameFromFile($inputFile);
    print "baseName=$baseName\n";
    my $outputFileBase="$workingDir\/$baseName";
    print "outputFileBase=$outputFileBase\n";
    my $logFile="$workingDir\/generateOutputFiles.log";

    my $resultFile = "$outputFileBase\_LocalAligned.v3draw";
    die "Could not find result file $resultFile\n" unless (-e $resultFile);

    my $cmd = "$v3d -cmd image-loader -convert $resultFile $workingDir\/Aligned\.v3draw";
    #$cmd = "mv $resultFile $workingDir\/Aligned\.v3draw";
    #$cmd = "$v3d -cmd image-loader -mapchannels $resultFile $workingDir\/Aligned\.v3draw \"3,0,0,1,1,2,2,3\"";
    print "cmd=$cmd\n";
    system( "$cmd 1>$logFile 2>&1" );
}

sub cleanup {
    print "Start cleanup\n";
    my $inputFile=$_[0];
    print "inputFile=$inputFile\n";
    my $baseName=&getBaseNameFromFile($inputFile);
    print "baseName=$baseName\n";
    my $outputFileBase="$workingDir\/$baseName";
    print "outputFileBase=$outputFileBase\n";
    my $logFile="$workingDir\/cleanup.log";

    my $cmd = "rm $outputFileBase" . "*\.v3draw";
    print "cmd=$cmd\n";
    system( "$cmd 1>$logFile 2>&1" );
}


__END__

