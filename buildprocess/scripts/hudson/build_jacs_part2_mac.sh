#!/bin/sh
#
# Hudson build script for the Fly Workstation (part 2)
# 
# In part 1 of the build script, we compiled everything we could on a 
# Linux box. This script (part 2) must run on a Mac box in order to 
# compile the necessary tools for Mac, and build the Mac bundle.
#
# Prerequisites: the Mac executor box must be configured as follows...
# 1) Platypus installed, and the command-line tool installed via the Preferences pane
# 2) jacsData mounted on /Volumes/jacsData
#

BUILD_VAA3D=1
BUILD_FLYSUITE=1

# Exit after any error
set -o errexit

FWVER=$1
SERVER=$2

JACSDATA_DIR="/groups/scicomp/jacsData"
SVN_OPTIONS="--trust-server-cert --non-interactive"

EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"
COMPILE_DIR="$EXE_DIR/compile"
VAA3D_COMPILE_MAC_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-mac"

STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
PACKAGE_MAC_DIR="$STAGING_DIR/Staging_${FWVER}"

echo "Building FlySuite version $FWVER (Part 2)"

################################################################
# Build Vaa3d for the Mac client
################################################################
if [ $BUILD_VAA3D == 1 ]; then

    echo "Building Vaa3D"
    cd $COMPILE_DIR

    echo "  Removing VAA3D_COMPILE_MAC_DIR"
    rm -rf "$VAA3D_COMPILE_MAC_DIR" || true

    echo "  Checking out from SVN"
    svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} $VAA3D_COMPILE_MAC_DIR
    if [ ! -f $VAA3D_COMPILE_MAC_DIR ]; then
        echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
        exit 1
    fi
    cd $VAA3D_COMPILE_MAC_DIR

    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk/buildprocess/scripts/hudson

    echo "  Building Vaa3D for the Mac client"
    hudson/build_vaa3d_mac.sh
    cp -R v3d_main/v3d/vaa3d64.app $PACKAGE_MAC_DIR
fi

################################################################
# Create the Mac Bundle
################################################################

if [ $BUILD_FLYSUITE == 1 ]; then

    WORKSTATION_JAR="$PACKAGE_MAC_DIR/workstation.jar"
    WORKSTATION_LIB="$PACKAGE_MAC_DIR/workstation_lib"
    VAA3D_BUNDLE="$PACKAGE_MAC_DIR/vaa3d64.app"
    COMPARTMENT_MAP="$PACKAGE_MAC_DIR/flybraincompartmentmap.v3ds"
    BUNDLE_SCRIPT="$PACKAGE_MAC_DIR/workstation.sh"
    BUNDLE_FILE="$PACKAGE_MAC_DIR/FlySuite_${FWVER}.app"

    /usr/local/bin/platypus -a 'FlySuite' -o 'None' -p '/bin/sh' -u 'HHMI'  -V "${FWVER}"  -I 'org.janelia.FlySuite' -f "$WORKSTATION_JAR" -f "$WORKSTATION_LIB" -f "$VAA3D_BUNDLE" -f "$COMPARTMENT_MAP" -c "$BUNDLE_SCRIPT" "$BUNDLE_FILE"

fi

echo "We're all done with part 2. The new Mac client bundle is available here:"
echo "  $BUNDLE_FILE"
echo "Run part 3 manually in order to deploy everything."
echo ""

