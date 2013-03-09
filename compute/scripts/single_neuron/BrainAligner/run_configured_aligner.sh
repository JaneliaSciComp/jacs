#!/bin/bash
#
# Wrapper script for executing 'configured' alignment pipelines in a temporary directory
#

DIR=$(cd "$(dirname "$0")"; pwd)

SCRIPT_PATH=$1
OUTPUT_DIR=""
shift 1
ARGS="$@"

while getopts ":o:h:" opt
do case "$opt" in
    o)  OUTPUT_DIR="$OPTARG";;
    h) echo "Usage: $0 <alignmentScript> [-o output_dir] ..." >&2
        exit 1;;
    esac
done

#export TMPDIR="$OUTPUT_DIR"
#WORKING_DIR=`mktemp -d`
WORKING_DIR="$OUTPUT_DIR/temp"
rm -rf $WORKING_DIR
mkdir $WORKING_DIR
cd $WORKING_DIR

echo "~ Alignment Script: $SCRIPT_PATH"
echo "~ Working Dir: $WORKING_DIR"
echo "~ Output Dir: $OUTPUT_DIR"

ARGS=`echo $ARGS | sed -e "s/-o \S*/-w ${WORKING_DIR//\//\\/}/"`

echo "~ COMMAND:"
CMD="$SCRIPT_PATH $ARGS"
echo $CMD
eval $CMD

echo "~ Computations complete"
echo "~ Space usage: " `du -h $WORKING_DIR`

echo "~ Moving final output to $OUTPUT_DIR"
mv $WORKING_DIR/FinalOutputs/* $OUTPUT_DIR

echo "~ Removing temp directory"
rm -rf $WORKING_DIR

echo "~ Finished"

