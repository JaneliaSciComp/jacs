#!/bin/sh
#
# Hudson build script for the Fly Workstation (part 1)
#
# This build script is meant to run on a Linux-based executor box, with 
# similar characteristics as the final Linux deployment client. This will 
# build the server, binary server tools, and the Linux clients. It will also
# create the Linux client distribution and the first part of a Mac client 
# distribution. The latter is completed in part 2 on a Mac executor.
#
# Before executing this build, branches should be created manually. 
# For example, to create build 0.1.1 from the trunk of all 3 repositories: 
# 
# FWVER=0.1.1
#
# svn copy https://svn.janelia.org/penglab/projects/vaa3d/trunk https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}"
#
# svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}" 
#
# svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}"
#
#

# Exit after any error
set -o errexit

# Configure SGE for doing grid builds
. /sge/6.2u5/default/common/settings.sh

FWVER=$1
JACSDATA_DIR="/groups/scicomp/jacsData"
EXE_DIR="$JACSDATA_DIR/servers/jacs/executables"
SCRIPT_DIR="$WORKSPACE"
COMPILE_DIR="$EXE_DIR/compile"
INSTALL_DIR="$EXE_DIR/install"
VAA3D_DIR="$INSTALL_DIR/vaa3d.redhat"
NEUSEP_DIR="$INSTALL_DIR/NeuronSeparator"
STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
TEMPLATE_DIR="$STAGING_DIR/templates"
SVN_OPTIONS="--trust-server-cert --non-interactive"

echo "Building FlySuite version $FWVER (Part 1)"

################################################################
# Build Vaa3d for Redhat (Grid) and Fedora (Client) 
################################################################
echo "Building Vaa3D"
cd $COMPILE_DIR
VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-fedora"
rm -rf $VAA3D_COMPILE_REDHAT_DIR || true
svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} $VAA3D_COMPILE_REDHAT_DIR
if [ ! -e $VAA3D_COMPILE_REDHAT_DIR ]; then
    echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
    exit 1
fi
cp "$SCRIPT_DIR/build_vaa3d_linux.sh" $VAA3D_COMPILE_REDHAT_DIR
cp -R $VAA3D_COMPILE_REDHAT_DIR $VAA3D_COMPILE_FEDORA_DIR

echo "Building Vaa3D for the grid"
qsub -sync "$SCRIPT_DIR/qsub_vaa3d_build.sh" $FWVER

echo "Building Vaa3D for the linux client"
cd $VAA3D_COMPILE_FEDORA_DIR
sh build_v3d_linux.sh

################################################################
# Install Vaa3d
################################################################
echo "Installing Vaa3D in $VAA3D_DIR"
mkdir -p $VAA3D_DIR
cp -R v3d $VAA3D_DIR

################################################################
# Build NeuronSeparator
################################################################
echo "Building NeuronSeparator for the grid"
cd $COMPILE_DIR
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_FlySuite_${FWVER}-redhat"
rm -rf $NEUSEP_COMPILE_REDHAT_DIR || true
svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/FlySuite_${FWVER} $NEUSEP_COMPILE_REDHAT_DIR
if [ ! -e $NEUSEP_COMPILE_REDHAT_DIR ]; then
    echo "SVN tag not found for NeuronSeparator: FlySuite_${FWVER}"
    exit 1
fi
qsub -sync "$SCRIPT_DIR/qsub_neusep_build.sh" $FWVER

################################################################
# Install NeuronSeparator
################################################################
echo "Installing NeuronSeparator in $NEUSEP_DIR"
mkdir -p $NEUSEP_DIR
cp mylib/sampsepNALoadRaw $NEUSEP_DIR
cp tools/setup4 $NEUSEP_DIR
cp tools/finish4 $NEUSEP_DIR

################################################################
# Build Jacs
################################################################
echo "Building Jacs"
cd $COMPILE_DIR
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_FlySuite_${FWVER}"
rm -rf $JACS_COMPILE_DIR || true
svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/FlySuite_${FWVER} $JACS_COMPILE_DIR
if [ ! -e $JACS_COMPILE_DIR ]; then
    echo "SVN tag not found for jacs: FlySuite_${FWVER}"
    exit 1
fi

cd $JACS_COMPILE_DIR
cd buildprocess
ant -buildfile build-all.xml
cd ../compute
# TODO: deploy to jacs and jacs-data?
#ant "deploy-[your-server]-dev"
cd ../console
ant "build-run-jar"

echo "Creating deployment packages"
cd $STAGING_DIR
rm -rf workstation
rm -rf workstation_linux
cp -R $TEMPLATE_DIR/mac_template workstation
cp -R $TEMPLATE_DIR/linux_template workstation_linux
cp -R $WORKSPACE/$JACS_COMPILE_DIR/console/jars/* workstation/ 
cp -R $WORKSPACE/$JACS_COMPILE_DIR/console/jars/* workstation_linux/ 

