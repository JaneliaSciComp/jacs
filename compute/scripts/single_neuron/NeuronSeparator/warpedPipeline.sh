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

CONSOLIDATED_LABEL="$OUTDIR/ConsolidatedLabel.v3draw"

if [ ! -s "$CONSOLIDATED_LABEL" ]; then
    CONSOLIDATED_LABEL="$OUTDIR/ConsolidatedLabel.v3dpbd"
    if [ ! -s "$CONSOLIDATED_LABEL" ]; then
        echo "ConsolidatedLabel file not found in output directory"
        exit 1
    fi
fi

EXT=${INPUT_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_INPUT_FILE=$INPUT_FILE
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_INPUT_FILE to $INPUT_FILE "
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE"
fi

if [ -s $INPUT_FILE ]; then

    echo "~ Generating full, aligned consolidated signal"
    CONSIGNAL="ConSignal3.v3draw"

    cat $INPUT_FILE | $NSDIR/v3draw_select_channels $SIGNAL_CHAN > $CONSIGNAL

    if [ ${#SIGNAL_CHAN} -lt 5 ] ; then
        # Less than 5 characters, which means less than 3 signal channels. 
        MAPPED_INPUT=ConSignal3Mapped.v3draw
        if [ ${#SIGNAL_CHAN} -lt 2 ] ; then
            # Single channel
            echo "Detected single channel image, duplicating channel 0 in channels 1 and 2"
            echo "$Vaa3D -cmd image-loader -mapchannels $CONSIGNAL $MAPPED_INPUT \"0,0,0,1,0,2\""
            $Vaa3D -cmd image-loader -mapchannels $CONSIGNAL $MAPPED_INPUT "0,0,0,1,0,2"
        else
            # Dual channel
            echo "Detected two channel image, duplicating channel 1 in channel 2"
            echo "$Vaa3D -cmd image-loader -mapchannels $CONSIGNAL $MAPPED_INPUT \"0,0,1,1,1,2\""
            $Vaa3D -cmd image-loader -mapchannels $CONSIGNAL $MAPPED_INPUT "0,0,1,1,1,2"
        fi
        CONSIGNAL=$MAPPED_INPUT
    fi

    echo "~ Generating 8-bit, y-flipped consolidated label"
    cat $CONSIGNAL | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_8bit > ConsolidatedSignal.v3draw

    echo "~ Generating aligned reference"
    cat $INPUT_FILE | $NSDIR/v3draw_select_channels $REF_CHAN > Reference.v3draw

    echo "~ Copying final output to: $OUTDIR"
    mv *.nsp $OUTDIR
    mv *.pbd $OUTDIR
    mv *.txt $OUTDIR
    
    echo "~ Compressing final output to: $OUTDIR"
    $Vaa3D -cmd image-loader -convert $CONSIGNAL $OUTDIR/ConsolidatedSignal3.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal.v3draw $OUTDIR/ConsolidatedSignal.v3dpbd
    $Vaa3D -cmd image-loader -convert Reference.v3draw $OUTDIR/Reference.v3dpbd
    
    EXT=${CONSOLIDATED_LABEL#*.}
    if [ $EXT == "v3draw" ]; then
        echo "~ Compressing consolidated label to PBD format"
        $Vaa3D -cmd image-loader -convert "$CONSOLIDATED_LABEL" "$OUTDIR/ConsolidatedLabel.v3dpbd"
        rm $CONSOLIDATED_LABEL
    fi
fi

if ls core* &> /dev/null; then
    echo "~ Error: core dumped in warped pipeline"
    touch $OUTDIR/core
fi

echo "~ Finished with separation pipeline"

if [ -s "$CONSOLIDATED_LABEL" ]; then
    echo "~ Launching artifact pipeline..."
    $DIR/artifactPipeline.sh $OUTDIR $NAME $INPUT_FILE "$SIGNAL_CHAN" "$REF_CHAN"
fi

echo "~ Removing temp files"
rm -rf $WORKING_DIR

echo "~ Removing temp files: $OUTDIR/tmp*"
rm -rf $OUTDIR/tmp*

