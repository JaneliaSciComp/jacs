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

##################
# inputs
##################

NUMPARAMS=$#
if [ $NUMPARAMS -lt 1 ]
then
    echo " "
    echo " USAGE ::  "
    echo " sh fastLoadPipeline.sh <separate dir>"
    echo " "
    exit
fi

MV_SIZES=( 25 50 100 200 ) # subsample sizes, in millions of voxels
SEPDIR=$1 # e.g. /groups/scicomp/jacsData/filestore/.../separate
INPUT_FILE=$2 # e.g. /groups/scicomp/jacsData/filestore/.../stitched-1679282762445488226.v3draw

REF_FILE="$SEPDIR/Reference.v3dpbd"
LABEL_FILE="$SEPDIR/ConsolidatedLabel.v3dpbd"
OUTDIR=$SEPDIR/fastLoad
WORKING_DIR=$OUTDIR/temp

echo "Run Dir: $DIR"
echo "Working Dir: $WORKING_DIR"
echo "Input file: $INPUT_FILE"
echo "Output dir: $OUTDIR"

if [ "$INPUT_FILE" = "" ]; then
    echo "Getting input file from neuSepConfiguration.1..."
    INPUT_FILE=`cat $SEPDIR/sge_config/neuSepConfiguration.1 | sed -n 3p`
    echo "    Got $INPUT_FILE"
fi 

if [ "$INPUT_FILE" = "" ]; then
    echo "Getting input file from neuSepCmd.sh..."
    INPUT_FILE= `cat $SEPDIR/sge_config/neuSepCmd.sh | tail -2 | head -1 | awk '{print $(NF)}'`
    echo "    Got $INPUT_FILE"
fi

mkdir -p $OUTDIR
mkdir -p $WORKING_DIR
cd $WORKING_DIR


EXT=${INPUT_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_INPUT_FILE=$INPUT_FILE
    INPUT_FILE_STUB=`basename $PBD_INPUT_FILE`
    INPUT_FILE="$WORKING_DIR/${INPUT_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_INPUT_FILE to $INPUT_FILE "
    $Vaa3D -cmd image-loader -convert "$PBD_INPUT_FILE" "$INPUT_FILE"
fi

EXT=${LABEL_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_LABEL_FILE=$LABEL_FILE
    LABEL_FILE_STUB=`basename $PBD_LABEL_FILE`
    LABEL_FILE="$WORKING_DIR/${LABEL_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_LABEL_FILE to $LABEL_FILE "
    $Vaa3D -cmd image-loader -convert "$PBD_LABEL_FILE" "$LABEL_FILE"
fi

EXT=${REF_FILE#*.}
if [ $EXT == "v3dpbd" ]; then
    PBD_REF_FILE=$REF_FILE
    REF_FILE_STUB=`basename $PBD_REF_FILE`
    REF_FILE="$WORKING_DIR/${REF_FILE_STUB%.*}.v3draw"
    echo "~ Converting $PBD_REF_FILE to $REF_FILE "
    $Vaa3D -cmd image-loader -convert "$PBD_REF_FILE" "$REF_FILE"
fi

echo "~ Creating full size 16-bit files"
cat $INPUT_FILE | $NSDIR/v3draw_select_channels 0 1 2 > ConsolidatedSignal3.v3draw
cat $LABEL_FILE | $NSDIR/v3draw_flip_y > ConsolidatedLabel3.v3draw
# Note that Reference3.v3dpbd is later created as a link 

echo "~ Creating full size 8-bit color corrected files"
cat ConsolidatedSignal3.v3draw | $NSDIR/v3draw_hdrgamma 0.40 1.00 0.46 2> ConsolidatedSignal2.colors | $NSDIR/v3draw_to_8bit > ConsolidatedSignal2.v3draw
# Note that ConsolidatedLabel2.v3dpbd is later created as a link
cat $REF_FILE | $NSDIR/v3draw_hdrgamma 0.20 1.00 0.46 2> Reference2.colors | $NSDIR/v3draw_to_8bit > Reference2.v3draw

echo "~ Creating single color files"
cat ConsolidatedSignal2.v3draw | $NSDIR/v3draw_select_channels 0 > ConsolidatedSignal2Red.v3draw
cat ConsolidatedSignal2.v3draw | $NSDIR/v3draw_select_channels 1 > ConsolidatedSignal2Green.v3draw
cat ConsolidatedSignal2.v3draw | $NSDIR/v3draw_select_channels 2 > ConsolidatedSignal2Blue.v3draw

echo "~ Creating metadata files"
cat ConsolidatedSignal2.colors > ConsolidatedSignal2.metadata
cat Reference2.colors > Reference2.metadata

for MV in ${MV_SIZES[@]}
do
    echo "~ Creating subsampled files for MV=$MV"
    cat ConsolidatedSignal3.v3draw | $NSDIR/v3draw_resample ${MV}000000 2> ConsolidatedSignal2_$MV.sizes | $NSDIR/v3draw_hdrgamma 0.40 1.00 0.46 2> ConsolidatedSignal2_$MV.colors | $NSDIR/v3draw_to_8bit > ConsolidatedSignal2_$MV.v3draw
    cat ConsolidatedLabel3.v3draw | $NSDIR/v3draw_resample_labels ${MV}000000 > ConsolidatedLabel2_$MV.v3draw
    cat $REF_FILE | $NSDIR/v3draw_resample ${MV}000000 2> Reference2_$MV.sizes | $NSDIR/v3draw_hdrgamma 0.20 1.00 0.46 2> Reference2_$MV.colors | $NSDIR/v3draw_to_8bit > Reference2_$MV.v3draw

    echo "~ Creating single color files for MV=$MV"
    cat ConsolidatedSignal2_$MV.v3draw | $NSDIR/v3draw_select_channels 0 > ConsolidatedSignal2Red_$MV.v3draw    
    cat ConsolidatedSignal2_$MV.v3draw | $NSDIR/v3draw_select_channels 1 > ConsolidatedSignal2Green_$MV.v3draw
    cat ConsolidatedSignal2_$MV.v3draw | $NSDIR/v3draw_select_channels 2 > ConsolidatedSignal2Blue_$MV.v3draw

    echo "~ Creating metadata file for MV=$MV"
    cat ConsolidatedSignal2_$MV.colors ConsolidatedSignal2_$MV.sizes > ConsolidatedSignal2_$MV.metadata
    cat Reference2_$MV.colors Reference2_$MV.sizes > Reference2_$MV.metadata
done

echo "~ Creating final output in: $OUTDIR"

$Vaa3D -cmd image-loader -convert ConsolidatedLabel3.v3draw $OUTDIR/ConsolidatedLabel3.v3dpbd
$Vaa3D -cmd image-loader -convert ConsolidatedSignal3.v3draw $OUTDIR/ConsolidatedSignal3.v3dpbd
$Vaa3D -cmd image-loader -convert Reference3.v3draw $OUTDIR/Reference3.v3dpbd

ln -s $OUTDIR/ConsolidatedLabel3.v3dpbd $OUTDIR/ConsolidatedLabel2.v3dpbd
$Vaa3D -cmd image-loader -convert ConsolidatedSignal2.v3draw $OUTDIR/ConsolidatedSignal2.v3dpbd
$Vaa3D -cmd image-loader -convert ConsolidatedSignal2.v3draw $OUTDIR/ConsolidatedSignal2.mp4
$Vaa3D -cmd image-loader -convert Reference2.v3draw $OUTDIR/Reference2.v3dpbd
$Vaa3D -cmd image-loader -convert Reference2.v3draw $OUTDIR/Reference2.mp4
$Vaa3D -cmd image-loader -convert ConsolidatedSignal2Red.v3draw $OUTDIR/ConsolidatedSignal2Red.mp4
$Vaa3D -cmd image-loader -convert ConsolidatedSignal2Green.v3draw $OUTDIR/ConsolidatedSignal2Green.mp4
$Vaa3D -cmd image-loader -convert ConsolidatedSignal2Blue.v3draw $OUTDIR/ConsolidatedSignal2Blue.mp4

for MV in ${MV_SIZES[@]}
do
    echo "~ Compressing files for MV=$MV"
    $Vaa3D -cmd image-loader -convert ConsolidatedLabel2_$MV.v3draw $OUTDIR/ConsolidatedLabel2_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2_$MV.v3draw $OUTDIR/ConsolidatedSignal2_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert Reference2_$MV.v3draw $OUTDIR/Reference2_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Red_$MV.v3draw $OUTDIR/ConsolidatedSignal2Red_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Green_$MV.v3draw $OUTDIR/ConsolidatedSignal2Green_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Blue_$MV.v3draw $OUTDIR/ConsolidatedSignal2Blue_$MV.v3dpbd
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2_$MV.v3draw $OUTDIR/ConsolidatedSignal2_$MV.mp4
    $Vaa3D -cmd image-loader -convert Reference2_$MV.v3draw $OUTDIR/Reference2_$MV.mp4
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Red_$MV.v3draw $OUTDIR/ConsolidatedSignal2Red_$MV.mp4
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Green_$MV.v3draw $OUTDIR/ConsolidatedSignal2Green_$MV.mp4
    $Vaa3D -cmd image-loader -convert ConsolidatedSignal2Blue_$MV.v3draw $OUTDIR/ConsolidatedSignal2Blue_$MV.mp4
done

cp *.metadata $OUTDIR # 10 files

if ls core* &> /dev/null; then
    touch $OUTDIR/core
fi

echo "~ Removing temp files"
#rm -rf $WORKING_DIR

echo "~ Finished"

