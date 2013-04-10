#!/bin/sh
#
# Run the neuron separation pipeline
#
# Usage:
# sh separatePipeline.sh <output dir> <name> <input file> 

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
    echo " sh separatePipeline.sh <output dir> <name> <input file> \"<signal channels>\" \"<ref channel>\" <prev result file (OPTIONAL)>"
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
echo "Reference chan (1-indexed): $REF_CHAN_ONE_INDEXED"

EXT=${INPUT_FILE#*.}
if [ $EXT == "zip" ]; then
    echo "~ Unzipping input file"
    ZIP_INPUT_FILE=$INPUT_FILE
    INPUT_FILE_STUB=`basename $ZIP_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}"
    unzip $ZIP_INPUT_FILE
fi

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

# Decrease "c" to get more neurons
# Decrease "s" to get more (smaller) neurons
# Decrease "e" to get more neurons
SETUP_PARAMS="-c6.0 -e4.5 -s800"

if [ ${#SIGNAL_CHAN} -eq 3 ] ; then
    channel_output=""
    channel_pbd=""
    for channel in $SIGNAL_CHAN
    do
        echo "~ Generating separation for channel $channel"
        $NSDIR/setup11 $SETUP_PARAMS --single_channel $channel SeparationResultUnmapped$channel $SEP_INPUT_FILE
        channel_output="$channel_output SeparationResultUnmapped$channel.nsp"
        channel_pbd="$channel_pbd SeparationResultUnmapped$channel.pbd"
    done
    echo "~ Generating final combined separation"
    $NSDIR/setup11 --concat 1 SeparationResultUnmapped $channel_output
    $NSDIR/setup11 --save_channel -r$REF_CHAN_ONE_INDEXED SeparationResultUnmapped $channel_pbd
else
    echo "~ Converting input file to 16 bit"
    SEP16_INPUT_FILE="Input16.v3draw"
    cat $SEP_INPUT_FILE | $NSDIR/v3draw_to_16bit > $SEP16_INPUT_FILE
    SEP_INPUT_FILE=$SEP16_INPUT_FILE
    echo "~ Generating separation"
    $NSDIR/setup10 $SETUP_PARAMS -r$REF_CHAN_ONE_INDEXED SeparationResultUnmapped $SEP_INPUT_FILE
fi

echo "~ Separation complete"
RESULT='SeparationResult.nsp'

if [ -s SeparationResultUnmapped.nsp ]; then

    if [ "$PREVFILE" ]; then
        echo "~ Mapping neurons to previous fragment indexes found in $PREVFILE"
        $NSDIR/map_neurons SeparationResultUnmapped.nsp $PREVFILE > SeparationResult.nsp
        if [ ! -s "SeparationResult.nsp" ]; then
            echo "~ Mapping was not successful, proceeding with unmapped result"
            RESULT='SeparationResultUnmapped.nsp'
        fi
    else
        RESULT='SeparationResultUnmapped.nsp'
    fi

    if [ -s $RESULT ]; then

        echo "~ Generating consolidated signal"
        cat $INPUT_FILE | $NSDIR/v3draw_select_channels $SIGNAL_CHAN | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_8bit > ConsolidatedSignal.v3draw

        echo "~ Generating consolidated label"
        $NSDIR/nsp10_to_labelv3draw16 < $RESULT > ConsolidatedLabel.v3draw

        echo "~ Generating reference"
        cat $INPUT_FILE | $NSDIR/v3draw_select_channels $REF_CHAN > Reference.v3draw

        echo "~ Copying final output to: $OUTDIR"
        cp $RESULT $OUTDIR
        cp *.pbd $OUTDIR # companion file for the result
        cp ConsolidatedSignal.v3draw $OUTDIR
        cp ConsolidatedLabel.v3draw $OUTDIR
        cp Reference.v3draw $OUTDIR
    fi
fi

if ls core* &> /dev/null; then
    touch $OUTDIR/core
fi

echo "~ Finished with separation pipeline"

if [ -s "$OUTDIR/ConsolidatedLabel.v3draw" ]; then
    echo "~ Launching artifact pipeline..."
    $DIR/artifactPipeline.sh $OUTDIR $NAME $INPUT_FILE "$SIGNAL_CHAN" "$REF_CHAN"
fi

echo "~ Removing temp files"
rm -rf $WORKING_DIR

