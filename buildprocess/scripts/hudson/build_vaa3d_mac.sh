#!/bin/sh

export JACS_INSTALL_DIR="/groups/scicomp/jacsData/install"
export QTDIR="/usr/"
export CMAKE_DIR="${JACS_INSTALL_DIR}/cmake-2.8.9-mac/CMake-2.8-9.app/Contents"
export BOOST_DIR="${JACS_INSTALL_DIR}/boost-1.51.0-mac"
export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include
export PATH=$QTDIR/bin:$PATH
export BASE=`pwd`
export BUILD_DIR=$BASE/build_cmake
export BIN_DIR=$BASE/bin

mkdir -p $BUILD_DIR
cd $BUILD_DIR
${CMAKE_DIR}/bin/cmake -DUSE_FFMPEG=ON -DBoost_INCLUDE_DIR=${BOOST_DIR} -DQT_QMAKE_EXECUTABLE=${QTDIR}/bin/qmake ..
make

echo "Moving to bin dir..."
rm -rf $BIN_DIR
mkdir $BIN_DIR
cp -R $BUILD_DIR/v3d/Mac_Fat/Vaa3d.app $BIN_DIR/vaa3d64.app

echo "Fixing executable scripts..."
MACOSDIR=$BIN_DIR/vaa3d64.app/Contents/MacOS
mv $MACOSDIR/vaa3d $MACOSDIR/vaa3dbin
sed "s/vaa3d/vaa3dbin \$\*/" $MACOSDIR/vaa3d_script > $MACOSDIR/vaa3d64
cp $MACOSDIR/vaa3d64 $MACOSDIR/vaa3d
cp $MACOSDIR/vaa3d64 $MACOSDIR/vaa3d_script
chmod +x $MACOSDIR/*

