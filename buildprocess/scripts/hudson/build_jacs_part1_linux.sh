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

BUILD_VAA3D=0
BUILD_NEUSEP=0
BUILD_JACS=1
BUILD_FLYSUITE=1

# Exit after any error
set -o errexit

# Configure SGE for doing grid builds
. /sge/6.2u5/default/common/settings.sh

#TODO: change this to jacs for production use
FWVER=$1
SERVER=$2

SCRIPT_DIR=`pwd`
JACSDATA_DIR="/groups/scicomp/jacsData"
EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"

SVN_OPTIONS="--trust-server-cert --non-interactive"

MAC_EXECUTOR_HOST="saffordt-wm1"
SSH_OPTIONS="-i ${JACSDATA_DIR}/.ssh/id_dsa_hudson"

COMPILE_DIR="$EXE_DIR/compile"
VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-fedora"
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_FlySuite_${FWVER}-redhat"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_FlySuite_${FWVER}"

STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
TEMPLATE_DIR="$STAGING_DIR/templates"

PACKAGE_MAC_DIR="$STAGING_DIR/workstation"
PACKAGE_LINUX_DIR="$STAGING_DIR/workstation_linux"

echo "Building FlySuite version $FWVER (Part 1)"

################################################################
# Build Vaa3d for Redhat (Grid) and Fedora (Client) 
################################################################
if [ $BUILD_VAA3D == 1 ]; then
    echo "Building Vaa3D"
    cd $COMPILE_DIR
    
    echo "  Removing $VAA3D_COMPILE_REDHAT_DIR"
    rm -rf $VAA3D_COMPILE_REDHAT_DIR || true
    
    echo "  Removing $VAA3D_COMPILE_FEDORA_DIR"
    rm -rf $VAA3D_COMPILE_FEDORA_DIR || true

    echo "  Checking out from SVN"
    svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} $VAA3D_COMPILE_REDHAT_DIR
    if [ ! -e $VAA3D_COMPILE_REDHAT_DIR ]; then
        echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
        exit 1
    fi
    cp "$SCRIPT_DIR/build_vaa3d_linux.sh" $VAA3D_COMPILE_REDHAT_DIR
    cp -R $VAA3D_COMPILE_REDHAT_DIR $VAA3D_COMPILE_FEDORA_DIR

    echo "  Building Vaa3D for the grid"
    echo "sh $SCRIPT_DIR/qsub_vaa3d_build.sh $FWVER $SERVER" > "$VAA3D_COMPILE_REDHAT_DIR/build.sh"
    qsub -sync y "$VAA3D_COMPILE_REDHAT_DIR/build.sh"

    echo "  Building Vaa3D for the linux client"
    cd $VAA3D_COMPILE_FEDORA_DIR
    sh build_vaa3d_linux.sh
fi

################################################################
# Build NeuronSeparator
################################################################
if [ $BUILD_NEUSEP == 1 ]; then
    echo "Building NeuronSeparator"
    cd $COMPILE_DIR

    echo "  Removing $NEUSEP_COMPILE_REDHAT_DIR"
    rm -rf $NEUSEP_COMPILE_REDHAT_DIR || true
    
    echo "  Checking out from SVN"
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/FlySuite_${FWVER} $NEUSEP_COMPILE_REDHAT_DIR
    if [ ! -e $NEUSEP_COMPILE_REDHAT_DIR ]; then
        echo "SVN tag not found for NeuronSeparator: FlySuite_${FWVER}"
        exit 1
    fi
    
    echo "  Building NeuronSeparator for the grid"
    echo "sh $SCRIPT_DIR/qsub_neusep_build.sh $FWVER $SERVER" > "$NEUSEP_COMPILE_REDHAT_DIR/build.sh"
    qsub -sync y "$NEUSEP_COMPILE_REDHAT_DIR/build.sh"
fi

################################################################
# Build Jacs
################################################################
if [ $BUILD_JACS == 1 ]; then
    echo "Building Jacs"
    cd $COMPILE_DIR
    
    echo "  Removing $JACS_COMPILE_DIR"
    rm -rf $JACS_COMPILE_DIR || true
    
    echo "  Checking out from SVN"
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/FlySuite_${FWVER} $JACS_COMPILE_DIR
    if [ ! -e $JACS_COMPILE_DIR ]; then
        echo "SVN tag not found for jacs: FlySuite_${FWVER}"
        exit 1
    fi

    echo "  Building Jacs"
    cd $JACS_COMPILE_DIR
    cd buildprocess
    ant -buildfile build-all.xml
    cd ../compute
    # TODO: deploy to jacs and jacs-data?
    #ant "deploy-[your-server]-dev"
    cd ../console
    ant "build-run-jar"
fi

################################################################
# Build FlySuite Deployment Packages
################################################################
if [ $BUILD_FLYSUITE == 1 ]; then
    echo "Creating deployment packages"
    
    echo "  Removing $PACKAGE_MAC_DIR"
    rm -rf $PACKAGE_MAC_DIR
    
    echo "  Removing $PACKAGE_LINUX_DIR"
    rm -rf $PACKAGE_LINUX_DIR
    
    echo "  Creating new Mac package in $PACKAGE_MAC_DIR"
    cp -R $TEMPLATE_DIR/mac_template $PACKAGE_MAC_DIR
    cp -R $JACS_COMPILE_DIR/console/build/jars/* $PACKAGE_MAC_DIR
    
    echo "  Creating new Linux package in $PACKAGE_LINUX_DIR"
    cp -R $TEMPLATE_DIR/linux_template $PACKAGE_LINUX_DIR
    cp -R $JACS_COMPILE_DIR/console/build/jars/* $PACKAGE_LINUX_DIR 
fi

echo "We're all done with part 1. The new Linux client package is available here:"
echo "  $PACKAGE_LINUX_DIR"
echo "Most of the Mac client package (to be completed in part 2) is available here:"
echo "  $PACKAGE_MAC_DIR"
echo ""
echo "Now running part 2 on a Mac..."
ssh $SSH_OPTIONS $MAC_EXECUTOR_HOST "sh $SCRIPT_DIR/build_jacs_part2_mac.sh $FWVER $SERVER"

