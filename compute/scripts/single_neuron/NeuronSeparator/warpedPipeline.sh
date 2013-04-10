#!/bin/sh
#
# Run the neuron separation pipeline starting from a consolidated label file 
# that was warped by an alignment
#

DIR=$(cd "$(dirname "$0")"; pwd)

NETPBM_PATH="$DIR/../../../netpbm-redhat/"
NETPBM_BIN="$NETPBM_PATH/bin"
Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"
NSDIR="$DIR/../../../neusep-redhat"
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$NETPBM_PATH/lib"

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3 ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh warpedPipeline.sh <output dir> <name> <aligned input file> \"<signal channels>\" \"<ref channel>\""
    echo " Note: channel numbers are separated by spaces, and zero indexed."
    echo " "
    exit
fi

OUTDIR=$1
NAME=$2
INPUT_FILE=$3
SIGNAL_CHAN=$4
REF_CHAN=$5
PREVFILE=$6
REF_CHAN_ONE_INDEXED=`expr $REF_CHAN + 1`

export TMPDIR="$OUTDIR"
WORKING_DIR=`mktemp -d`
cd $WORKING_DIR

echo "Neuron Separator Dir: $NSDIR"
echo "Vaa3d Dir: $Vaa3D"
echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Output dir: $OUTDIR"
echo "Signal channels: $SIGNAL_CHAN"
echo "Reference channel: $REF_CHAN"

CONSOLIDATED_LABEL=$OUTDIR/ConsolidatedLabel.v3draw

EXT=${INPUT_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_INPUT_FILE=$INPUT_FILE
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_INPUT_FILE to $INPUT_FILE "
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE"
fi

SEP_INPUT_FILE=$INPUT_FILE
echo "Seperator input file: $SEP_INPUT_FILE"

echo "~ Generating aligned consolidated signal"
cat $INPUT_FILE | $NSDIR/v3draw_select_channels $SIGNAL_CHAN | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_8bit > ConsolidatedSignal.v3draw

echo "~ Generating aligned reference"
cat $INPUT_FILE | $NSDIR/v3draw_select_channels $REF_CHAN > Reference.v3draw

echo "~ Copying final output to: $OUTDIR"
cp ConsolidatedSignal.v3draw $OUTDIR
cp Reference.v3draw $OUTDIR

if ls core* &> /dev/null; then
    touch $OUTDIR/core
fi

echo "~ Finished with separation pipeline"

if [ -s "$CONSOLIDATED_LABEL" ]; then
    echo "~ Launching artifact pipeline..."
    $DIR/artifactPipeline.sh $OUTDIR $NAME $INPUT_FILE "$SIGNAL_CHAN" "$REF_CHAN"
fi

echo "~ Removing temp files"
rm -rf $WORKING_DIR

