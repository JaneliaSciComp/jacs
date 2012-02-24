#!/bin/sh

export JACS_INSTALL_DIR="/groups/scicomp/jacsData/install"

if [ -f /etc/fedora-release ] ; then
    export QTDIR="${JACS_INSTALL_DIR}/Qt-4.7.4-fedora"
    export INCLUDEPATH="${JACS_INSTALL_DIR}/tiff-3.9.5-fedora/include"
else
    export QTDIR="${JACS_INSTALL_DIR}/Qt-4.7.4-redhat"
    export INCLUDEPATH="${JACS_INSTALL_DIR}/tiff-3.9.5-redhat/include"
fi

export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include

export PATH=$QTDIR/bin:$PATH
export PATH=$HOME/bin:$PATH

./build.linux -B -m -j4
export BASE=`pwd`

# build brainaligner
cd $BASE/jba/c++
make
cp $BASE/jba/c++/brainaligner $BASE/bin/

# build lobeseg
cd $BASE/released_plugins/v3d_plugins/lobeseg/lobeseg_main
cp main_backgnd_lobeseg.makefile Makefile
make
cp $BASE/released_plugins/v3d_plugins/lobeseg/lobeseg_main/lobe_seger $BASE/bin/

# build optic lobe aligner
#cd $BASE/released_plugins/v3d_plugins/ssd_registration
#echo "INCLUDEPATH += $INCLUDEPATH" >> plugin_PQ_imagereg.pro
#qmake
#make
#cp -R $BASE/released_plugins/v3d/plugins/optic_lobe_aligner $BASE/v3d/plugins

# copy libs
cp -R $BASE/v3d_main/common_lib/lib $BASE/bin/

