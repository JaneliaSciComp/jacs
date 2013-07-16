#!/bin/sh
JWVER=$1
SERVER=$2
DIR="/groups/jacs/jacsHosts/servers/${SERVER}/executables/compile/vaa3d_JaneliaWorkstation_${JWVER}-redhat"
echo "Building $DIR"
cd $DIR
sh build_vaa3d_linux.sh > build.out
