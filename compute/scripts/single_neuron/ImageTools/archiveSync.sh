#!/bin/bash
if [ $# -ne 2 ]; then
    echo "Usage: `basename $0` [source path] [target path]"
    exit 65
fi
SOURCE_FILE=${1%/}
TARGET_FILE=${2%/}
SOURCE_NAME=`basename $SOURCE_FILE`
TARGET_NAME=`basename $TARGET_FILE`
SOURCE_DIR=`dirname $SOURCE_FILE`
TARGET_DIR=`dirname $TARGET_FILE`
echo "Target dir: $TARGET_DIR"
if [ -s "$TARGET_FILE" ] && [ "$SOURCE_NAME" == "$TARGET_NAME" ]; then
    echo "Target already exists, using rsync"
    rsync -aW $SOURCE_FILE $TARGET_DIR
else
    if [ -s "$TARGET_FILE" ]; then
        echo "Removing existing file in target location"
        rm -rf $TARGET_FILE
    fi
    echo "Creating target parent: $TARGET_DIR"
    mkdir -p $TARGET_DIR
    export TMPDIR="$TARGET_DIR"
    TEMP_DIR=`mktemp -d`
    echo "Copying source to temporary target: $TEMP_DIR"
    cp -a $SOURCE_FILE $TEMP_DIR/tmp
    echo "Moving into final location: $TARGET_FILE"
    mv $TEMP_DIR/tmp $TARGET_FILE
    rm -rf $TEMP_DIR
fi


