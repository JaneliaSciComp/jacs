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
SCRATCH_DIR="/scratch/jacs/"
WORKING_DIR=`mktemp -d -p $SCRATCH_DIR`

export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$NETPBM_PATH/lib"

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
INPUTFILE=$3
PREVFILE=$4

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUTFILE"
echo "Output dir: $OUTDIR"
cd $WORKING_DIR

#Usage: setup4  [-c<double(5.0)>] [-e<double(3.5)>] [-s<int(300)>] 
#                <output:FILE>  <inputs:FILE> ...
#Usage: finish4  [-3BM] [-nr] [-cl]
#                 <folder:FOLDER>  <core:STRING>  <input:FILE>

# Decrease "c" to get more neurons
# Decrease "s" to get more (smaller) neurons
# Decrease "e" to get more neurons

echo "~ Generating separation"
$NSDIR/setup10 -c6.0 -e4.5 -s800 -r4 SeparationResultUnmapped $INPUTFILE

if [ "$PREVFILE" ]; then
    echo "~ Mapping neurons to previous fragment indexes "
    $NSDIR/map_neurons SeparationResultUnmapped.nsp $PREVFILE > SeparationResult.nsp
else
    mv SeparationResultUnmapped.nsp SeparationResult.nsp
fi

echo "~ Generating consolidated signal"
cat $INPUTFILE | $NSDIR/v3draw_select_channels 0 1 2 | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_8bit > ConsolidatedSignal.v3draw
$Vaa3D -cmd image-loader -convert ConsolidatedSignal.v3draw ConsolidatedSignal.v3dpbd

echo "~ Generating consolidated label"
$NSDIR/nsp10_to_labelv3draw16 < SeparationResult.nsp > ConsolidatedLabel.v3draw
$Vaa3D -cmd image-loader -convert ConsolidatedLabel.v3draw ConsolidatedLabel.v3dpbd

echo "~ Generating reference"
cat $INPUTFILE | $NSDIR/v3draw_select_channels 3 > Reference.v3draw
$Vaa3D -cmd image-loader -convert Reference.v3draw Reference.v3dpbd

echo "~ Generating sample MIPs"
cat ConsolidatedSignal.v3draw | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pnmtopng > ConsolidatedSignalMIP.png
cat Reference.v3draw | $NSDIR/v3draw_to_8bit | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pnmtopng > ReferenceMIP.png

echo "~ Generating fragment MIPs"
$NSDIR/nsp10_to_neuron_mips "." $NAME SeparationResult.nsp

echo "~ Copying output to final location: $OUTDIR"
cp SeparationResult.nsp $OUTDIR
cp ConsolidatedSignal.v3dpbd $OUTDIR
cp ConsolidatedLabel.v3dpbd $OUTDIR
cp Reference.v3dpbd $OUTDIR
cp *.png $OUTDIR

echo "~ Removing temp dir $WORKING_DIR"
rm -rf $WORKING_DIR

echo ""
