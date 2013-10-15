#!/bin/sh
export MALLOC_CHECK_=0
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -Dsun.awt.disableMixing=true -Xms512m -Xmx2560m -XX:+UseParallelGC -XX:-UseGCOverheadLimit -jar workstation.jar &
#sleep 3
#./vaa3d -na &
