#!/bin/bash

SCRIPT_PATH=$1
CONFIG_PATH=$2
TEMPLATE_DIR=$3
TOOLKITS_DIR=$4
INPUT_FILE=$5
OUTPUT_DIR=$6
REF_CHANNEL=$7
OPTICAL_RESOLUTION=$8

WORKING_DIR=$OUTPUT_DIR/temp
rm -rf $WORKING_DIR
mkdir $WORKING_DIR
cd $WORKING_DIR

echo "~ Alignment Script: $SCRIPT_PATH"
echo "~ Alignment Config: $CONFIG_PATH"
echo "~ Template Dir: $TEMPLATE_DIR"
echo "~ Run Dir: $DIR"
echo "~ Working Dir: $WORKING_DIR"

CMD="$SCRIPT_PATH $CONFIG_PATH $TEMPLATE_DIR $TOOLKITS_DIR $WORKING_DIR $INPUT_FILE $REF_CHANNEL $OPTICAL_RESOLUTION"
echo $CMD
$CMD

echo "~ Computations complete"
echo "~ Space usage: " `du -h $WORKING_DIR`

echo "~ Moving final output to $FINAL_OUTPUT"
mv $WORKING_DIR/* $OUTPUT_DIR

echo "~ Removing temp directory"
rm -rf $WORKING_DIR

echo "~ Finished"

