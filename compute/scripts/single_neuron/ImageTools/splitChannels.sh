#!/bin/sh
#
# Channel splitting pipeline
#

DIR=$(cd "$(dirname "$0")"; pwd)

####
# TOOLKITS
####

Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"

export TMPDIR=""

####
# Inputs
####

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3  ]
then
    echo " "
    echo " USAGE: sh $0 input_file output_dir output_extension"
    echo " "
    exit
fi

INPUT_FILE=$1
OUTPUT_DIR=$2
OUT_EXT=$3

WORKING_DIR=`mktemp -d -p $OUTPUT_DIR`
cd $WORKING_DIR

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Outputs: $OUTPUT_DIR/*.$OUT_EXT"

INPUT_NAME=`basename $INPUT_FILE`
ln -s $INPUT_FILE $WORKING_DIR/$INPUT_NAME
INPUT_FILE=$WORKING_DIR/$INPUT_NAME

echo "~ Splitting channels"
$Vaa3D -x ireg -f splitColorChannels -i $INPUT_FILE

INPUT_FILE_STUB=${INPUT_FILE%.*}

for FILE in ${INPUT_FILE_STUB}_*
do
    FILE_NAME=`basename $FILE`
    FILE_EXT=${FILE_NAME#*.}
    FILE_STUB=${FILE%.*}
    if [ "$FILE_EXT" != "$OUT_EXT" ]; then
        OUT_FILE="$FILE_STUB.$OUT_EXT"
        echo "~ Converting $FILE to $OUT_FILE"
        $Vaa3D -cmd image-loader -convert "$FILE" "$OUT_FILE"
    fi
done

echo "~ Copying outputs to $OUTPUT_DIR"
cp $WORKING_DIR/*.$OUT_EXT $OUTPUT_DIR

echo "~ Removing working directory $WORKING_DIR"
rm -rf $WORKING_DIR

