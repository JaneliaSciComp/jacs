#!/bin/sh
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -Dsun.awt.disableMixing=true -Xms512m -Xmx2560m -XX:+UseParallelGC -XX:-UseGCOverheadLimit -jar workstation.jar > console.log 2>&1 &
#sleep 3
#./vaa3d64.app/Contents/MacOS/vaa3d64 -na > vaa3d.log 2>&1 &

