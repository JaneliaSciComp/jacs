#/bin/sh
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

#export TMPDIR=""
#SCRATCH_DIR="/scratch/jacs/"

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 3 ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh separatePipeline.sh <output dir> <name> <input file> <prev result file (OPTIONAL)>"
    echo " "
    exit
fi

OUTDIR=$1
NAME=$2
INPUT_FILE=$3
PREVFILE=$4
#WORKING_DIR=`mktemp -d -p $SCRATCH_DIR`
WORKING_DIR=$OUTDIR/temp

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Output dir: $OUTDIR"

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

echo "~ Generating separation"
# -c6.0 -e4.5 -s800
$NSDIR/setup10 -c6.0 -e4.5 -s800 -r4 SeparationResultUnmapped $INPUT_FILE

if [ -s SeparationResultUnmapped.nsp ]; then

    if [ "$PREVFILE" ]; then
        echo "~ Mapping neurons to previous fragment indexes"
        $NSDIR/map_neurons SeparationResultUnmapped.nsp $PREVFILE > SeparationResult.nsp
        if [ ! -s "SeparationResult.nsp" ]; then
            echo "~ Mapping was not successful, proceeding with unmapped result"
            mv SeparationResultUnmapped.nsp SeparationResult.nsp
        fi
    else
        mv SeparationResultUnmapped.nsp SeparationResult.nsp
    fi

    if [ -s SeparationResult.nsp ]; then

        echo "~ Generating consolidated signal"
        cat $INPUT_FILE | $NSDIR/v3draw_select_channels 0 1 2 | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_8bit > ConsolidatedSignal.v3draw
        $Vaa3D -cmd image-loader -convert ConsolidatedSignal.v3draw ConsolidatedSignal.v3dpbd

        echo "~ Generating consolidated label"
        $NSDIR/nsp10_to_labelv3draw16 < SeparationResult.nsp > ConsolidatedLabel.v3draw
        $Vaa3D -cmd image-loader -convert ConsolidatedLabel.v3draw ConsolidatedLabel.v3dpbd

        echo "~ Generating reference"
        cat $INPUT_FILE | $NSDIR/v3draw_select_channels 3 > Reference.v3draw
        $Vaa3D -cmd image-loader -convert Reference.v3draw Reference.v3dpbd

        echo "~ Generating sample MIPs"
        cat ConsolidatedSignal.v3draw | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pnmtopng > ConsolidatedSignalMIP.png
        cat Reference.v3draw | $NSDIR/v3draw_to_8bit | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pnmtopng > ReferenceMIP.png

        echo "~ Generating fragment MIPs"
        $NSDIR/nsp10_to_neuron_mips "." $NAME SeparationResult.nsp

        echo "~ Copying final output to: $OUTDIR"
        cp SeparationResult.nsp $OUTDIR
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

echo "~ Finished"
