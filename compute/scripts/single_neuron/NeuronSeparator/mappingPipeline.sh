#!/bin/sh
#
# Run the mapping pipeline between two separations
#
# Usage:
# sh mappingPipeline.sh <outdir> <input file 1> <input file 2> 

DIR=$(cd "$(dirname "$0")"; pwd)
Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"
NSDIR="$DIR/../../../neusep-redhat"

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3 ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh mappingPipeline.sh <output dir> <source input file> <target input file>"
    echo " "
    exit
fi

OUTDIR=$1
INPUT_FILE_1=$3
INPUT_FILE_2=$4

export TMPDIR="$OUTDIR"
WORKING_DIR=`mktemp -d`
cd $WORKING_DIR

echo "Neuron Separator Dir: $NSDIR"
echo "Vaa3d Dir: $Vaa3D"
echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file 1 (old labels): $INPUT_FILE_1"
echo "Input file 2 (new labels): $INPUT_FILE_2"

EXT=${INPUT_FILE_1#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_INPUT_FILE=$INPUT_FILE_1
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE_1="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_INPUT_FILE to $INPUT_FILE_1"
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE_1"
fi

EXT=${INPUT_FILE_2#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_INPUT_FILE=$INPUT_FILE_2
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_INPUT_FILE to $INPUT_FILE_2"
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE_2"
fi

echo "~ Mapping neurons"
$NSDIR/map_neurons $INPUT_FILE_2 $INPUT_FILE_1 > /dev/null 2>mapping_issues.txt

echo "~ Copying final output to: $OUTDIR"
cp *.txt $OUTDIR

echo "~ Finished with neuron mapping pipeline"

echo "~ Removing working files: $WORKING_DIR"
rm -rf $WORKING_DIR

echo "~ Removing temp files: $OUTDIR/tmp*"
rm -rf $OUTDIR/tmp*

