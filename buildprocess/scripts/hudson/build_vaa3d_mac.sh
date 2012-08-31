#!/bin/sh

#export JACS_INSTALL_DIR="/groups/scicomp/jacsData/install"
#export QTDIR="/groups/scicomp/jacsData/install/Qt-4.7.4-mac"
#export CMAKE_DIR="${JACS_INSTALL_DIR}/cmake-2.8.9-mac/CMake-2.8-9.app/Contents"
#export BOOST_DIR="${JACS_INSTALL_DIR}/boost-1.51.0-mac"
#export QTLIB=$QTDIR/lib
#export QTINC=$QTDIR/include
#export PATH=$QTDIR/bin:$PATH
#export BASE=`pwd`
#export BUILD_DIR=$BASE/build_cmake
#export BIN_DIR=$BASE/bin

#mkdir -p $BUILD_DIR
#cd $BUILD_DIR
#${CMAKE_DIR}/bin/cmake -DUSE_FFMPEG=ON -DBoost_INCLUDE_DIR=${BOOST_DIR} ..
#make
#cp -R $BUILD_DIR/v3d/Mac_Fat/* $BIN_DIR
#mv bin/Vaa3d.app bin/vaa3d64.app

export QTDIR="/groups/scicomp/jacsData/install/Qt-4.7.4-mac"
export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include
export PATH=$QTDIR/bin:$PATH

./build.macx -B -m -j4 -norun
$QTDIR/bin/macdeployqt v3d_main/v3d/vaa3d64.app

