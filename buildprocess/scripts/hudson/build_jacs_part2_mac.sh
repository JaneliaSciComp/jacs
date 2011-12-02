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

set -o errexit

FWVER=$1
JACSDATA_DIR="/Volumes/jacsData"
SCRIPT_DIR="$WORKSPACE"
STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
PACKAGE_DIR="$STAGING_DIR/workstation"
SVN_OPTIONS="--trust-server-cert --non-interactive"

################################################################
# Build Vaa3d for the Mac client
################################################################

svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} vaa3d_FlySuite_${FWVER}-mac
if [ ! -f "vaa3d_FlySuite_${FWVER}-redhat" ]; then
    echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
    exit
fi
cd vaa3d_FlySuite_${FWVER}-mac
$SCRIPT_DIR/build_vaa3d_mac.sh
cp -R v3d_main/v3d/vaa3d64.app $PACKAGE_DIR

################################################################
# Create the Mac Bundle
################################################################

WORKSTATION_JAR="$PACKAGE_DIR/workstation.jar"
WORKSTATION_LIB="$PACKAGE_DIR/workstation_lib"
VAA3D_BUNDLE="$PACKAGE_DIR/vaa3d64.app"
COMPARTMENT_MAP="$PACKAGE_DIR/flybraincompartmentmap.v3ds"
BUNDLE_SCRIPT="$PACKAGE_DIR/workstation.sh"

# We could use a profile...
#/usr/local/bin/platypus -P $STAGING_DIR/workstation/FlySuite.platypus $STAGING_DIR/workstation/FlySuite.app
# But being explicit allows us to customize the filepaths:
/usr/local/bin/platypus -a 'FlySuite' -o 'None' -p '/bin/sh' -u 'HHMI'  -V "${FWVER}"  -I 'org.janelia.FlySuite' -f "$WORKSTATION_JAR" -f "$WORKSTATION_LIB" -f "$VAA3D_BUNDLE" -f "$COMPARTMENT_MAP" -c "$BUNDLE_SCRIPT" "FlySuite_${FWVER}.app"

