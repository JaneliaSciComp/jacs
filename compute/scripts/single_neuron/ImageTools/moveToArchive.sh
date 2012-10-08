#!/bin/bash
#
# This script must be run in the directory that files are being moved from!
#

CWD=`pwd`
NEW_PATH=${CWD/groups/archive}

echo "mkdir -p $NEW_PATH"
mkdir -p $NEW_PATH

for f in $@
do
    if [ -h $f ]; then
        echo "$f is a symbolic link"
    else
        FILENAME=`basename $f`
        
        echo "mv $FILENAME $NEW_PATH"
        mv $FILENAME $NEW_PATH

        #echo "ln -s $NEW_PATH/$FILENAME $FILENAME"
        #ln -s $NEW_PATH/$FILENAME $FILENAME
    fi
done


