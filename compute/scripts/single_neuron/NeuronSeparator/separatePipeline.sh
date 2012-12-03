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
WORKING_DIR=$OUTDIR/temp

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Output dir: $OUTDIR"
echo "Signal channels: $SIGNAL_CHAN"
echo "Reference channel: $REF_CHAN"

mkdir $WORKING_DIR
cd $WORKING_DIR

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

#Usage: setup4  [-c<double(5.0)>] [-e<double(3.5)>] [-s<int(300)>] 
#                <output:FILE>  <inputs:FILE> ...
#Usage: finish4  [-3BM] [-nr] [-cl]
#                 <folder:FOLDER>  <core:STRING>  <input:FILE>

# Decrease "c" to get more neurons
# Decrease "s" to get more (smaller) neurons
# Decrease "e" to get more neurons

# The following assumes that the reference channel comes after the signal channels. 
# It could be more robust.

SEP_INPUT_FILE=$INPUT_FILE

if [ `echo $SIGNAL_CHAN | wc -m` -lt 5 ] ; then
    # Less than 5 characters, which means less than 3 signal channels. 
    MAPPED_INPUT=mapped.v3draw
    if [ `echo $SIGNAL_CHAN | wc -m` -lt 2 ] ; then
        # Single channel
        echo "Detected single channel image, duplicating channel 0 in channels 1 and 2"
        echo "$Vaa3D -cmd image-loader -mapchannels $INPUT_FILE $MAPPED_INPUT \"0,0,0,1,0,2,${REF_CHAN},3\""
        $Vaa3D -cmd image-loader -mapchannels $INPUT_FILE $MAPPED_INPUT "0,0,0,1,0,2,${REF_CHAN},3"
    else
        # Dual channel
        echo "Detected two channel image, duplicating channel 1 in channel 2"
        echo "$Vaa3D -cmd image-loader -mapchannels $INPUT_FILE $MAPPED_INPUT \"0,0,1,1,1,2,${REF_CHAN},3\""
        $Vaa3D -cmd image-loader -mapchannels $INPUT_FILE $MAPPED_INPUT "0,0,1,1,1,2,${REF_CHAN},3"
    fi
    SEP_INPUT_FILE=$MAPPED_INPUT
fi

# The above logic forces the reference onto the fourth channel
REF_CHAN_ONE_INDEXED=4
echo "Reference chan (1-indexed): $REF_CHAN_ONE_INDEXED"

echo "~ Converting input file to 16 bit"
SEP16_INPUT_FILE="Input16.v3draw"
cat $SEP_INPUT_FILE | $NSDIR/v3draw_to_16bit > $SEP16_INPUT_FILE
SEP_INPUT_FILE=$SEP16_INPUT_FILE

echo "~ Generating separation"
$NSDIR/setup10 -c6.0 -e4.5 -s800 -r$REF_CHAN_ONE_INDEXED SeparationResultUnmapped $SEP_INPUT_FILE

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
        $Vaa3D -cmd image-loader -convert ConsolidatedSignal.v3draw ConsolidatedSignal.v3dpbd

        echo "~ Generating consolidated label"
        $NSDIR/nsp10_to_labelv3draw16 < $RESULT > ConsolidatedLabel.v3draw
        $Vaa3D -cmd image-loader -convert ConsolidatedLabel.v3draw ConsolidatedLabel.v3dpbd

        echo "~ Generating reference"
        cat $INPUT_FILE | $NSDIR/v3draw_select_channels $REF_CHAN > Reference.v3draw
        $Vaa3D -cmd image-loader -convert Reference.v3draw Reference.v3dpbd

        echo "~ Generating sample MIPs"
        cat ConsolidatedSignal.v3draw | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pamtotiff -truecolor > ConsolidatedSignalMIP.tif
        $Vaa3D -x ireg -f iContrastEnhancer -i ConsolidatedSignalMIP.tif -o ConsolidatedSignalMIP2.tif -p "#m 5.0"
        $NETPBM_BIN/tifftopnm ConsolidatedSignalMIP2.tif | $NETPBM_BIN/pnmtopng > ConsolidatedSignalMIP.png

        cat Reference.v3draw | $NSDIR/v3draw_to_8bit | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pamtotiff -truecolor > ReferenceMIP.tif
        $Vaa3D -x ireg -f iContrastEnhancer -i ReferenceMIP.tif -o ReferenceMIP2.tif
        $NETPBM_BIN/tifftopnm ReferenceMIP2.tif | $NETPBM_BIN/pnmtopng > ReferenceMIP.png

        echo "~ Generating fragment MIPs"
        $NSDIR/nsp10_to_neuron_mips "." $NAME $RESULT

        echo "~ Copying final output to: $OUTDIR"
        cp $RESULT $OUTDIR
        cp *.pbd $OUTDIR # companion file for the result
        cp ConsolidatedSignal.v3dpbd $OUTDIR
        cp ConsolidatedLabel.v3dpbd $OUTDIR
        cp Reference.v3dpbd $OUTDIR
        cp *.png $OUTDIR
    
    fi
fi

if ls core* &> /dev/null; then
    touch $OUTDIR/core
fi

echo "~ Removing temp files"
rm -rf $WORKING_DIR

echo "~ Finished with separation pipeline"

if [ -s "$OUTDIR/ConsolidatedLabel.v3dpbd" ]; then
    echo "~ Launching fastLoad pipeline..."
    $DIR/fastLoadPipeline.sh $OUTDIR $INPUT_FILE
fi

