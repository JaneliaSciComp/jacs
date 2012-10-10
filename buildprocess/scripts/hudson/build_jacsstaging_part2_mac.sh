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

# Exit after any error
set -o errexit

FWVER=$1
SERVER=$2
BUILD_VAA3D=$3
BUILD_FLYSUITE=$4

JACSDATA_DIR="/groups/scicomp/jacsData"
SVN_OPTIONS="--trust-server-cert --non-interactive"

EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"
SCRIPT_DIR="$JACSDATA_DIR/servers/$SERVER/scripts"

COMPILE_DIR="$EXE_DIR/compile"
VAA3D_COMPILE_MAC_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-mac"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_FlySuite_${FWVER}"

STAGING_DIR="$EXE_DIR/FlySuiteStaging"
PACKAGE_MAC_DIR="$STAGING_DIR/FlySuite_${FWVER}"

echo "Building FlySuite version $FWVER (Part 2)"

################################################################
# Build Vaa3d for the Mac client
################################################################
if [ $BUILD_VAA3D == 1 ]; then 
    echo "Building Vaa3D"
    cd $COMPILE_DIR

    echo "  Removing $VAA3D_COMPILE_MAC_DIR"
    rm -rf "$VAA3D_COMPILE_MAC_DIR" || true

    echo "  Checking out from SVN"
    #svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/tags/FlySuite_${FWVER} $VAA3D_COMPILE_MAC_DIR
    svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/trunk $VAA3D_COMPILE_MAC_DIR
    if [ ! -e $VAA3D_COMPILE_MAC_DIR ]; then
        echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
        exit 1
    fi
    cd $VAA3D_COMPILE_MAC_DIR

    echo "  Building Vaa3D for the Mac client"
    sh $SCRIPT_DIR/build_vaa3d_mac.sh
fi

################################################################
# Create the Mac Bundle
################################################################

if [ $BUILD_FLYSUITE == 1 ]; then

    mkdir -p $PACKAGE_MAC_DIR # this should have been created by part 1
    cp -R $VAA3D_COMPILE_MAC_DIR/bin/vaa3d64.app $PACKAGE_MAC_DIR/vaa3d64.app

    ICON_FILE="$JACS_COMPILE_DIR/console/src/main/java/images/fly.png"
    WORKSTATION_JAR="$PACKAGE_MAC_DIR/workstation.jar"
    WORKSTATION_LIB="$PACKAGE_MAC_DIR/workstation_lib"
    VAA3D_BUNDLE="$PACKAGE_MAC_DIR/vaa3d64.app"
    BUNDLE_SCRIPT="$PACKAGE_MAC_DIR/workstation.sh"
    START_SCRIPT="$PACKAGE_MAC_DIR/start.sh"
    TMP_BUNDLE_FILE="/tmp/FlySuite.app"
    BUNDLE_FILE="$PACKAGE_MAC_DIR/FlySuite.app"

    rm -rf $BUNDLE_FILE || true
    /usr/local/bin/platypus -a 'FlySuite' -o 'None' -p '/bin/sh' -u 'HHMI'  -V "${FWVER}"  -I 'org.janelia.FlySuite' -i "$ICON_FILE" -f "$WORKSTATION_JAR" -f "$WORKSTATION_LIB" -f "$VAA3D_BUNDLE" -f "$START_SCRIPT" -c "$BUNDLE_SCRIPT" "$TMP_BUNDLE_FILE"

    mv "$TMP_BUNDLE_FILE" "$BUNDLE_FILE"
fi

echo "We're all done with part 2. The new Mac client bundle is available here:"
echo "  $BUNDLE_FILE"
echo "Run part 3 manually in order to deploy everything."
echo ""

