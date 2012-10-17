#!/bin/bash

DIR=$(cd "$(dirname "$0")"; pwd)

SCRIPT_PATH=$1
CONFIG_PATH=$2
TEMPLATE_DIR=$3
INPUT_FILE=$4
OUTPUT_DIR=$5
REF_CHANNEL=$6
OPTICAL_RESOLUTION=$7

WORKING_DIR=$OUTPUT_DIR/temp
rm -rf $WORKING_DIR
mkdir $WORKING_DIR
cd $WORKING_DIR

echo "~ Run Dir: $DIR"
echo "~ Working Dir: $WORKING_DIR"

$SCRIPT_PATH $CONF_FILE "$TEMPLATE_DIR" "$TOOLKITS_DIR" "$WORKING_DIR" "$INPUT_FILE" "$REF_CHANNEL" $OPTICAL_RESOLUTION

echo "~ Computations complete"
echo "~ Space usage: " `du -h $WORKING_DIR`

echo "~ Moving final output to $FINAL_OUTPUT"
mv $WORKING_DIR/* $OUTPUT_DIR

echo "~ Removing temp directory"
rm -rf $WORKING_DIR

echo "~ Finished"

