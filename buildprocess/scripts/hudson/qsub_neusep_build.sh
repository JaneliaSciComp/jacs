#!/bin/sh
FWVER=$1
cd /groups/scicomp/jacsData/servers/jacs/executables/compile/neusep_FlySuite_${FWVER}-redhat
mkdir build_cmake.redhat
cd build_cmake.redhat
/groups/scicomp/jacsData/install/cmake-2.8.6-redhat/bin/cmake .. ; make

