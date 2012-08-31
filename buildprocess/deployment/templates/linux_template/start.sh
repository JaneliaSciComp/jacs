#!/bin/sh
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -Dsun.awt.disableMixing=true -XX:+UseParallelGC -XX:-UseGCOverheadLimit -jar workstation.jar -Xms512m -Xmx2560m &
#sleep 3
#./vaa3d -na &
