#!/bin/bash
#
# 40x fly brain alignment pipeline 1.0, June 11, 2012
#

DIR=$(cd "$(dirname "$0")"; pwd)

####
# TOOLKITS
####

Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3 ]
then
echo " "
echo " USAGE ::  "
echo " sh brainalign.sh <template_dir> <input_file> <output_dir>"
echo " "
exit
fi

TEMPLATE_DIR=$1
INPUT_FILE=$2
FINAL_OUTPUT=$3
FINAL_DIR=${FINAL_OUTPUT%/*}
FINAL_STUB=${FINAL_OUTPUT%.*}
OUTPUT_FILENAME=`basename $FINAL_OUTPUT`

WORKING_DIR=$FINAL_DIR/temp
mkdir $WORKING_DIR
cd $WORKING_DIR

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Final Output Dir: $FINAL_DIR"

EXT=${INPUT_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    echo "~ Converting v3dpbd to v3draw"
    PBD_INPUT_FILE=$INPUT_FILE
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE"
fi

# template
CMPBND="$TEMPLATE_DIR/wfb_atx_template_rec2_boundaries.tif"
TMPMIPNULL="$TEMPLATE_DIR/templateMIPnull.tif"
ATLAS="$TEMPLATE_DIR/wfb_atx_template_ori.tif"

# output
OUTPUT="$WORKING_DIR/wb40x_"
SUBPREFIX=$OUTPUT

# subject
SUBJECT=$INPUT_FILE
SUBJET_REFNO=3

# target
TARGET="$TEMPLATE_DIR/wfb_atx_template_rec2.tif"
TARGET_REFNO=1
TARGET_MARKER="$TEMPLATE_DIR/wfb_atx_template_rec2.marker"

# mips
MIP1=$SUBPREFIX"_mip1.tif"
MIP2=$SUBPREFIX"_mip2.tif"
MIP3="_mip3.tif"


####################################
# sampling from 0.3um 0.58um to 0.62um
####################################

SUBSS=$SUBPREFIX"Subsampled.v3draw"
echo "~ Running isampler on $SUBJECT"
$Vaa3D -x ireg -f isampler -i $SUBJECT -o $SUBSS -p "#x 0.4839 #y 0.4839 #z 0.9355"

####################################
# flip along z
####################################

SUBSSFLIP=$SUBPREFIX"ssFliped.v3draw"
echo "~ Running zflip on $SUBSS"
$Vaa3D -x ireg -f zflip -i $SUBSS -o $SUBSSFLIP

####################################
# resizing
####################################

SUBPP=$SUBPREFIX"Preprocessed.v3draw"
echo "~ Running prepare20xData on $SUBSSFLIP"
$Vaa3D -x ireg -f prepare20xData -o $SUBPP -p "#s $SUBSSFLIP #t $TARGET"

####################################
# brain alignment
####################################

##################
# global alignment
##################

GAOUTPUT=$SUBPREFIX"Global.v3draw"
TARGET_REFNO=`expr $TARGET_REFNO - 1`;
SUBJECT_REFNO=`expr $SUBJECT_REFNO - 1`;
echo "~ Running global alignment"
$BRAINALIGNER -t $TARGET -C $TARGET_REFNO -s $SUBPP -c $SUBJECT_REFNO -w 0 -o $GAOUTPUT -B 1280 -H 2

##################
# local alignment
##################

# GAOUTPUT_C3 is the reference
GAOUTPUT_C0=$SUBPREFIX"Global_c0.v3draw"
GAOUTPUT_C1=$SUBPREFIX"Global_c1.v3draw"
GAOUTPUT_C2=$SUBPREFIX"Global_c2.v3draw"
echo "~ Running splitColorChannels on $GAOUTPUT"
$Vaa3D -x ireg -f splitColorChannels -i $GAOUTPUT

LAOUTPUT_C0=$SUBPREFIX"Local_c0.v3draw"
LAOUTPUT_C1=$SUBPREFIX"Local_c1.v3draw"
LAOUTPUT_C2=$SUBPREFIX"Local_c2.v3draw"
CSVT=$LAOUTPUT_C2"_target.csv"
CSVS=$LAOUTPUT_C2"_subject.csv"

echo "~ Running local alignment on $GAOUTPUT_C2"
$BRAINALIGNER -t $TARGET -s $GAOUTPUT_C2 -w 10 -o $LAOUTPUT_C2 -L $TARGET_MARKER -B 1280 -H 2

echo "~ Running local alignment on $GAOUTPUT_C0"
$BRAINALIGNER -t $TARGET -s $GAOUTPUT_C0 -w 10 -o $LAOUTPUT_C0 -L $CSVT -l $CSVS -B 1280 -H 2

echo "~ Running local alignment on $GAOUTPUT_C1"
$BRAINALIGNER -t $TARGET -s $GAOUTPUT_C1 -w 10 -o $LAOUTPUT_C1 -L $CSVT -l $CSVS -B 1280 -H 2

LA_OUTPUT=$SUBPREFIX"Warped.v3draw"
echo "~ Running mergeColorChannels"
$Vaa3D -x ireg -f mergeColorChannels -i $LAOUTPUT_C1 $LAOUTPUT_C0 $LAOUTPUT_C2 $CMPBND -o $LA_OU
TPUT

####################################
# resize output
####################################

PREPARED_OUTPUT=$SUBPREFIX"_Aligned.v3draw"
echo "~ Running prepare20xData to generate final output"
$Vaa3D -x ireg -f prepare20xData -o $PREPARED_OUTPUT -p "#s $LA_OUTPUT #t $ATLAS"

####################################
# MIPS
####################################

# GAOUTPUT_C3 is the reference
AOUTPUT_C0=$SUBPREFIX"_Aligned_c0.v3draw"
AOUTPUT_C1=$SUBPREFIX"_Aligned_c1.v3draw"
AOUTPUT_C2=$SUBPREFIX"_Aligned_c2.v3draw"
AOUTPUT_C3=$SUBPREFIX"_Aligned_c3.v3draw"

echo "~ Running splitColorChannels on $FINAL_OUTPUT"
$Vaa3D -x ireg -f splitColorChannels -i $FINAL_OUTPUT
TMPOUTPUT=$OUTPUT"_tmp.v3draw"

echo "~ Running mergeColorChannels to generate $TMPOUTPUT"
$Vaa3D -x ireg -f mergeColorChannels -i $AOUTPUT_C0 $AOUTPUT_C1 $AOUTPUT_C2 -o $TMPOUTPUT
echo "~ Running ireg's zmip on $TMPOUTPUT"
$Vaa3D -x ireg -f zmip -i $TMPOUTPUT -o $MIP3

SUBPREFIX=`echo $MIP3 | awk -F\. '{print $1}'`
TOUTPUT_C0=$SUBPREFIX"_c0.v3draw"
TOUTPUT_C1=$SUBPREFIX"_c1.v3draw"
TOUTPUT_C2=$SUBPREFIX"_c2.v3draw"

echo "~ Running splitColorChannels on $MIP3"
$Vaa3D -x ireg -f splitColorChannels -i $MIP3
echo "~ Running mergeColorChannels to generate $MIP2"
$Vaa3D -x ireg -f mergeColorChannels -i $TOUTPUT_C0 $TMPMIPNULL $TOUTPUT_C2 -o $MIP2
echo "~ Running mergeColorChannels to generate $MIP3"
$Vaa3D -x ireg -f mergeColorChannels -i $TMPMIPNULL $TOUTPUT_C1 $TOUTPUT_C2 -o $MIP1

echo "~ Running iContrastEnhancer"
$Vaa3D -x ireg -f iContrastEnhancer -i $MIP1 -o $MIP1 -p "#m 5"
$Vaa3D -x ireg -f iContrastEnhancer -i $MIP2 -o $MIP2 -p "#m 5"
$Vaa3D -x ireg -f iContrastEnhancer -i $MIP3 -o $MIP3 -p "#m 5"

EXT=${FINAL_OUTPUT##*.}
if [ "$EXT" == "v3dpbd" ]
then
    ALIGNED_COMPRESSED="$WORKING_DIR/Aligned.v3dpbd"
    echo "~ Compressing output file to 8-bit PBD: $ALIGNED_COMPRESSED"
    $Vaa3D -cmd image-loader -convert8 $OUTPUT $ALIGNED_COMPRESSED
    PREPARED_OUTPUT=$ALIGNED_COMPRESSED
fi

echo "~ Computations complete"
echo "~ Space usage: " `du -h $WORKING_DIR`

echo "~ Moving final output to $FINAL_OUTPUT"
mv $PREPARED_OUTPUT $FINAL_OUTPUT
mv $MIP1 $FINAL_DIR
mv $MIP2 $FINAL_DIR
mv $MIP3 $FINAL_DIR

echo "~ Removing temp files"
rm -rf $WORKING_DIR

echo "~ Finished"

