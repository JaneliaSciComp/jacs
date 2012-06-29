#!/bin/sh
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -XX:+UseParallelGC -jar workstation.jar -Xms512m -Xmx2560m > console.log 2>&1 &
sleep 3
./vaa3d64.app/Contents/MacOS/vaa3d64 -na > vaa3d.log 2>&1 &

