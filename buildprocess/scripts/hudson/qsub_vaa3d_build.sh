#!/bin/sh
FWVER=$1
SERVER=$2
DIR="/groups/scicomp/jacsData/servers/${SERVER}/executables/compile/vaa3d_FlySuite_${FWVER}-redhat"
echo "Building $DIR"
cd $DIR
sh build_vaa3d_linux.sh > build.out
