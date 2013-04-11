#!/bin/sh
#
# Generate mask and chan files for all neurons in a neuron separation
#
# Usage:
# sh maskChanPipeline.sh <separate dir> <working dir>

DIR=$(cd "$(dirname "$0")"; pwd)

. $DIR/../ImageTools/common.sh

NETPBM_PATH="$DIR/../../../netpbm-redhat/"
NETPBM_BIN="$NETPBM_PATH/bin"
Vaa3D="$DIR/../../../vaa3d-redhat/vaa3d"
NSDIR="$DIR/../../../neusep-redhat"
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$NETPBM_PATH/lib"

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 1 ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh maskChanPipeline.sh <separate dir> <working dir>"
    exit
fi

SEPDIR=$1 # e.g. /groups/scicomp/jacsData/filestore/.../separate
WORK_DIR=$2
if [ "$WORK_DIR" == "" ]; then
    WORK_DIR=$SEP_DIR
fi

LABEL_FILE="$SEPDIR/ConsolidatedLabel.v3draw"
SIGNAL_FILE="$SEPDIR/ConsolidatedSignal.v3draw"

if [ ! -f "$LABEL_FILE" ]; then
    LABEL_FILE="$SEPDIR/ConsolidatedLabel.v3dpbd"
    if [ ! -f "$LABEL_FILE" ]; then
        echo "Label file not found: $LABEL_FILE"
        exit
    fi
fi

if [ ! -f "$SIGNAL_FILE" ]; then
    SIGNAL_FILE="$SEPDIR/ConsolidatedSignal.v3dpbd"
    if [ ! -f "$SIGNAL_FILE" ]; then
        echo "Signal file not found: $SIGNAL_FILE"
        exit
    fi
fi

OUTDIR=$SEPDIR/archive/maskChan
export TMPDIR="$SEPDIR"
WORKING_DIR=`mktemp -d`
cd $WORKING_DIR

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Label file: $LABEL_FILE"
echo "Signal file: $SIGNAL_FILE"
echo "Output dir: $OUTDIR"

echo "~ Creating mask/chan files"
echo "$Vaa3D -cmd neuron-fragment-editor -mode reverse-label -sourceImage $SIGNAL_FILE -labelIndex $LABEL_FILE -outputDir $WORKING_DIR -outputPrefix neuron"
$Vaa3D -cmd neuron-fragment-editor -mode reverse-label -sourceImage $SIGNAL_FILE -labelIndex $LABEL_FILE -outputDir $WORKING_DIR -outputPrefix neuron

mkdir -p $OUTDIR
if ls core* &> /dev/null; then
    echo "~ Error: core dumped"
    touch $OUTDIR/core
else
    echo "~ Moving files to final output directory"
    mv $WORKING_DIR/* $OUTDIR
fi

echo "~ Removing maskChan temp files"
rm -rf $WORKING_DIR

echo "~ Finished with maskChan pipeline"

