#!/bin/sh
JWVER=$1
SERVER=$2
DIR="/groups/scicomp/jacsData/servers/${SERVER}/executables/compile/neusep_JaneliaWorkstation_${JWVER}-redhat"
echo "Building $DIR"
cd $DIR
mkdir build_cmake.redhat
cd build_cmake.redhat
/groups/scicomp/jacsData/install/cmake-2.8.6-redhat/bin/cmake .. ; make > build.out

