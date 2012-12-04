#!/bin/sh
DIR=$(cd "$(dirname "$0")"; pwd)
cd $DIR
java -XX:+UseParallelGC -jar workstation.jar -Xms512m -Xmx1024m &
sleep 3
./vaa3d -na &
