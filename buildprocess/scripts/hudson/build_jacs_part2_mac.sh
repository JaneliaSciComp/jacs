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

export FWVER=$1
export JACSDATA_DIR="/Volumes/jacsData"
export SCRIPT_DIR="$WORKSPACE"
export STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"

################################################################
# Build Vaa3d for the Mac client
################################################################

svn co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} vaa3d_FlySuite_${FWVER}-mac
if [ ! -f "vaa3d_FlySuite_${FWVER}-redhat" ]; then
    echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
    exit
fi
cd vaa3d_FlySuite_${FWVER}-mac
$SCRIPT_DIR/build_vaa3d_mac.sh
cp -R v3d_main/v3d/vaa3d64.app $STAGING_DIR/workstation/

################################################################
# Create the Mac Bundle
################################################################

/usr/local/bin/platypus -P $STAGING_DIR/workstation/FlySuite.platypus $STAGING_DIR/workstation/FlySuite.app

