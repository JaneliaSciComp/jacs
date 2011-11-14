
if [ -f /etc/fedora-release ] ; then
    export QTDIR="/home/rokickik/local/Qt-4.7.4"
else
    export QTDIR="/groups/scicomp/jacsData/servers/rokicki-ws/executables/install/Qt-4.7.4"
fi

export QTLIB=$QTDIR/lib
export QTINC=$QTDIR/include

export PATH=$QTDIR/bin:$PATH
export PATH=$HOME/bin:$PATH

./build.linux -B -m -j4
export BASE=`pwd`

# build brainaligner
cd $BASE/v3d_main/jba/c++
make
cp $BASE/v3d_main/jba/c++/brainaligner $BASE/v3d/

# build lobeseg
cd $BASE/released_plugins/v3d_plugins/lobeseg/lobeseg_main
cp main_backgnd_lobeseg.makefile Makefile
make
cp $BASE/released_plugins/v3d_plugins/lobeseg/lobeseg_main/lobe_seger $BASE/v3d/

# copy libs
cp -R ./v3d_main/common_lib/lib $BASE/v3d

