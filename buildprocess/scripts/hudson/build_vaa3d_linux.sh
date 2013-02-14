#!/bin/sh

export JACS_INSTALL_DIR="/groups/scicomp/jacsData/install"

if [ -f /etc/fedora-release ] ; then
    export QTDIR="${JACS_INSTALL_DIR}/Qt-4.7.4-fedora"
    export CMAKE_DIR="${JACS_INSTALL_DIR}/cmake-2.8.9-fedora"
    export BOOST_DIR="${JACS_INSTALL_DIR}/boost-1.51.0-fedora"
else
    export QTDIR="${JACS_INSTALL_DIR}/Qt-4.7.4-redhat"
    export CMAKE_DIR="${JACS_INSTALL_DIR}/cmake-2.8.9-redhat"
    export BOOST_DIR="${JACS_INSTALL_DIR}/boost-1.51.0-redhat"
fi

export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include
export PATH=$QTDIR/bin:$PATH
export PATH=$HOME/bin:$PATH
export BASE=`pwd`
export BUILD_DIR=$BASE/build_cmake
export BIN_DIR=$BASE/bin

# build vaa3d
mkdir -p $BUILD_DIR
cd $BUILD_DIR
${CMAKE_DIR}/bin/cmake -DUSE_FFMPEG=ON -DBoost_INCLUDE_DIR=${BOOST_DIR} ..
make

# build clonal select
cd $BUILD_DIR
touch ../released_plugins/v3d_plugins/clonalselect/clonalselect_gui.h
make clonalselect VERBOSE=1

# collect binaries
cp -R $BUILD_DIR/v3d/Linux_64/* $BIN_DIR
cp $BUILD_DIR/lobe_seger $BIN_DIR
cp $BUILD_DIR/brainaligner $BIN_DIR

