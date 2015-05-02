#!/bin/sh
# 
# Generate additional artifacts for an existing separation:
# 1) mips
# 2) fast load artifacts
# 3) mask/chan artifacts
#
# Assumes that the Consolidated and Reference files have aleady been generated and are
# present in the output directory. 

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
    echo " sh artifactPipeline.sh <output dir> <name> <input file> \"<signal channels>\" \"<ref channel>\""
    echo " Note: channel numbers are separated by spaces, and zero indexed."
    echo " "
    exit
fi

OUTDIR=$1
NAME=$2
INPUT_FILE=$3
SIGNAL_CHAN=$4
REF_CHAN=$5

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
CONSOLIDATED_SIGNAL=$OUTDIR/ConsolidatedSignal.v3draw
REFERENCE=$OUTDIR/Reference.v3draw

echo "~ Generating sample MIPs"
SIGNAL_MIP_PPM=ConsolidatedSignalMIP.ppm
cat $CONSOLIDATED_SIGNAL | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_flip_y | $NSDIR/v3draw_to_ppm > $SIGNAL_MIP_PPM 
if [[ ${#SIGNAL_CHAN} -lt 3 ]] ; then
    # single signal channel will come out as greyscale, so let's colorize it
    echo "Converting single channel signal MIP to red"
    $NETPBM_BIN/ppmtopgm $SIGNAL_MIP_PPM | $NETPBM_BIN/pgmtoppm "#FF0000" > tmp.ppm
    mv tmp.ppm $SIGNAL_MIP_PPM
fi
cat $SIGNAL_MIP_PPM | $NETPBM_BIN/pamtotiff -truecolor > ConsolidatedSignalMIP.tif

$Vaa3D -x ireg -f iContrastEnhancer -i ConsolidatedSignalMIP.tif -o ConsolidatedSignalMIP2.tif -p "#m 5.0"
$NETPBM_BIN/tifftopnm ConsolidatedSignalMIP2.tif | $NETPBM_BIN/pnmtopng > ConsolidatedSignalMIP.png

cat $REFERENCE | $NSDIR/v3draw_to_8bit | $NSDIR/v3draw_to_mip | $NSDIR/v3draw_to_ppm | $NETPBM_BIN/pamtotiff -truecolor > ReferenceMIP.tif
$Vaa3D -x ireg -f iContrastEnhancer -i ReferenceMIP.tif -o ReferenceMIP2.tif
$NETPBM_BIN/tifftopnm ReferenceMIP2.tif | $NETPBM_BIN/pnmtopng > ReferenceMIP.png

echo "~ Generating fragment MIPs with $NFE_MAX_THREAD_COUNT threads"
$Vaa3D -cmd neuron-fragment-editor -mode mips -sourceImage $CONSOLIDATED_SIGNAL -labelIndex $CONSOLIDATED_LABEL -outputDir . -outputPrefix $NAME.PR.neuron -maxThreadCount $NFE_MAX_THREAD_COUNT

for FILE in $NAME.PR.neuron*.tif
do
    $NETPBM_BIN/tifftopnm $FILE | $NETPBM_BIN/pnmtopng > "${FILE/.tif}".png
done

echo "~ Moving final artifacts to: $OUTDIR"
mv *.png $OUTDIR

if ls core* &> /dev/null; then
    echo "~ Error: core dumped in artifact pipeline"
    touch $OUTDIR/core
fi

echo "~ Finished with separation artifact pipeline"

if [ -s "$OUTDIR/ConsolidatedLabel.v3draw" ] || [ -s "$OUTDIR/ConsolidatedLabel.v3dpbd" ]; then
    echo "~ Launching fastLoad pipeline..."
    $DIR/fastLoadPipeline.sh $OUTDIR $INPUT_FILE
    echo "~ Launching maskChan pipeline..."
    $DIR/maskChanPipeline.sh $OUTDIR
fi

echo "~ Removing artifact temp files"
rm -rf $WORKING_DIR

