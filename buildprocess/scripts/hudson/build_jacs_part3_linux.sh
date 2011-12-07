#!/bin/sh
#
# Hudson build script for the Fly Workstation (part 3)
#
# The final part of the build script actually deploys the newly compiled
# executables and the Jacs servers.
#

INSTALL_VAA3D=1
INSTALL_NEUSEP=1
INSTALL_JACS=1
INSTALL_JACSDATA=1

# Exit after any error
set -o errexit

FWVER=$1
SERVER=$2

JACSDATA_DIR="/groups/scicomp/jacsData"
EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"

COMPILE_DIR="$EXE_DIR/compile"
VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-fedora"
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_FlySuite_${FWVER}-redhat"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_FlySuite_${FWVER}"

INSTALL_DIR="$EXE_DIR/install"
VAA3D_INSTALL_REDHAT_DIR="$INSTALL_DIR/vaa3d_FlySuite-redhat"
NEUSEP_INSTALL_REDHAT_DIR="$INSTALL_DIR/neusep_FlySuite-redhat"

STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
PACKAGE_MAC_DIR="$STAGING_DIR/Staging_${FWVER}"
PACKAGE_LINUX_DIR="$STAGING_DIR/Staging_linux_${FWVER}"
FLYSUITE_INSTALL_DIR="$JACSDATA_DIR/FlySuite_${FWVER}"
FLYSUITE_LINUX_INSTALL_DIR="$JACSDATA_DIR/FlySuite_linux_${FWVER}"

echo "Installing FlySuite version $FWVER (Part 3)"

################################################################
# Install Vaa3d for Redhat (Grid) and Fedora (Client) 
################################################################
if [ $INSTALL_VAA3D == 1 ]; then
    echo "Installing Vaa3D in $VAA3D_INSTALL_REDHAT_DIR"
    mkdir -p $VAA3D_INSTALL_REDHAT_DIR
    cp -R VAA3D_COMPILE_REDHAT_DIR/v3d/* $VAA3D_INSTALL_REDHAT_DIR

    echo "Installing Vaa3D in $VAA3D_COMPILE_FEDORA_DIR"
    mkdir -p $VAA3D_COMPILE_FEDORA_DIR
    cp -R VAA3D_COMPILE_REDHAT_DIR/v3a/* $VAA3D_COMPILE_FEDORA_DIR
fi

################################################################
# Install NeuronSeparator
################################################################
if [ $INSTALL_NEUSEP == 1 ]; then
    echo "Installing NeuronSeparator in $NEUSEP_INSTALL_REDHAT_DIR"
    mkdir -p $NEUSEP_INSTALL_REDHAT_DIR
    cd "$NEUSEP_COMPILE_REDHAT_DIR/build_cmake.redhat"
    cp mylib/sampsepNALoadRaw $NEUSEP_INSTALL_REDHAT_DIR
    cp tools/setup4 $NEUSEP_INSTALL_REDHAT_DIR
    cp tools/finish4 $NEUSEP_INSTALL_REDHAT_DIR
fi

################################################################
# Install Jacs to jacs and jacs-data servers
################################################################
if [ $BUILD_JACS == 1 ]; then
    echo "Installing Jacs"
    cd $JACS_COMPILE_DIR
    
    #TODO: stop server

    

    #TODO: start server

fi

################################################################
# Install FlySuite Deployment Packages
################################################################
if [ $BUILD_FLYSUITE == 1 ]; then
    echo "Installing deployment packages"
    cp -R $PACKAGE_MAC_DIR $FLYSUITE_INSTALL_DIR
    cp -R $PACKAGE_LINUX_DIR $FLYSUITE_LINUX_INSTALL_DIR
fi

echo "FlySuite Version ${FWVER} was successfully installed into the following locations:"
echo "  Mac: $FLYSUITE_INSTALL_DIR"
echo "  Linux: $FLYSUITE_LINUX_INSTALL_DIR"
echo ""

