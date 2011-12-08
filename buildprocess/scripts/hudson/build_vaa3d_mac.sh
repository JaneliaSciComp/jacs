#!/bin/sh

export QTDIR="/groups/scicomp/jacsData/install/Qt-4.7.4-mac"
export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include

export PATH=$QTDIR/bin:$PATH

./build.macx -B -m -j4 -norun

$QTDIR/bin/macdeployqt v3d_main/v3d/vaa3d64.app

