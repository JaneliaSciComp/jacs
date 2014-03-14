#!/bin/bash
#
# two image cross pipeline
#

DIR=$(cd "$(dirname "$0")"; pwd)

####
# TOOLKITS
####

Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"
MAGICK="$DIR/../../../ImageMagick-6.7.3-2"
TIFF="/groups/scicomp/jacsData/servers/jacs/executables/install/tiff"

export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$MAGICK/lib:$TIFF/lib"

export TMPDIR=""

####
# Inputs
####

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3  ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh imgIntersect.sh input1.v3dpbd input2.v3dpbd output.v3dpbd <Method> <GaussianKernelSize>"
    echo " "
    exit
fi

INPUT1=$1
INPUT2=$2
FINAL_OUTPUT=$3
METHOD=$4
KERNEL_SIZE=$5
FINAL_DIR=${FINAL_OUTPUT%/*}
FINAL_STUB=${FINAL_OUTPUT%.*}
OUTPUT_FILENAME=`basename $FINAL_OUTPUT`
SCRATCH_DIR=$FINAL_DIR
WORKING_DIR=`mktemp -d -p $SCRATCH_DIR`

mkdir $WORKING_DIR
cd $WORKING_DIR

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file 1: $INPUT1"
echo "Input file 2: $INPUT2"
echo "Final Output Dir: $FINAL_DIR"


EXT=${INPUT1#*.}
if [ $EXT == "v3dpbd" ]; then
    echo "~ Converting $INPUT1 to v3draw"
    PBD_INPUT_FILE=$INPUT1
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT1="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT1"
fi

EXT=${INPUT2#*.}
if [ $EXT == "v3dpbd" ]; then
    echo "~ Converting $INPUT2 to v3draw"
    PBD_INPUT_FILE=$INPUT2
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT2="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT2"
fi

OUTPUT="$WORKING_DIR/out.v3draw"
$Vaa3D -x imath -f intersection -o "$OUTPUT" -p "#s $INPUT1 #t $INPUT2 #m $METHOD #w $KERNEL_SIZE #n 1"

OUTPUT_MIP="$WORKING_DIR/out.png"
$Vaa3D -cmd image-loader -mip "$OUTPUT" "$WORKING_DIR/out.tif"
$MAGICK/bin/convert "$WORKING_DIR/out.tif" "$OUTPUT_MIP"

EXT=${FINAL_OUTPUT##*.}
if [ "$EXT" == "v3dpbd" ]
then
    COMPRESSED="$WORKING_DIR/out.v3dpbd"
    echo "~ Compressing output file to PBD: $COMPRESSED"
    $Vaa3D -cmd image-loader -convert $OUTPUT $COMPRESSED
    OUTPUT=$COMPRESSED
fi

echo "~ Moving output to final location: $FINAL_OUTPUT"
mv $OUTPUT $FINAL_OUTPUT
mv $OUTPUT_MIP "$FINAL_STUB.png"

echo "~ Removing working directory $WORKING_DIR"
rm -rf $WORKING_DIR

echo ""

