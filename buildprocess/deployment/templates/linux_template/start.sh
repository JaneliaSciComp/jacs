#!/bin/sh
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -XX:+UseParallelGC -jar workstation.jar -Xms512m -Xmx2560m &
sleep 3
./vaa3d -na &
