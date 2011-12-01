#!/bin/sh

export QTDIR="/groups/scicomp/jacsData/install/Qt-4.7.4-mac-static"
export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include

export PATH="/Applications/CMake 2.8-6.app/Contents/bin":$PATH
export PATH=$QTDIR/bin:$PATH

./build.macx -B -m -j4 -noplugins -norun

$QTDIR/bin/macdeployqt v3d_main/v3d/vaa3d64.app

