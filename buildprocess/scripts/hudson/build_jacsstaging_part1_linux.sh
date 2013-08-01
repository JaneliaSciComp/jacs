#!/bin/sh
#
# Hudson build script for the Workstation (part 1)
#
# This build script is meant to run on a Linux-based executor box, with 
# similar characteristics as the final Linux deployment client. This will 
# build the server, binary server tools, and the Linux clients. It will also
# create the Linux client distribution and the first part of a Mac client 
# distribution. The latter is completed in part 2 on a Mac executor.
#
# Before executing this build, tags should be created manually, 
# or by using the codeFreeze.sh script. 
# 
# For example, to create manually build 0.1.1 from the trunk of all 3 repositories: 
# 
# JWVER=0.1.1
#
# svn copy https://svn.janelia.org/penglab/projects/vaa3d/trunk https://svn.janelia.org/penglab/projects/vaa3d/tags/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"
#
# svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"
#
# svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/tags/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"
#
#

# Exit after any error
set -o errexit

# Configure SGE for doing grid builds
. /sge/current/default/common/settings.sh

JWVER=$1
SERVER=$2
BUILD_VAA3D=$3
BUILD_NEUSEP=$4
BUILD_JACS=$5
BUILD_JANELIAWORKSTATION=$6
RUN_PART2=$7
PART2_BUILD_VAA3D=$8
PART2_BUILD_JANELIAWORKSTATION=$9
PART2_BUILD_JACSTEST=$10

JACSHOME_DIR="/home/jacs"
JACSDATA_DIR="/groups/jacs/jacsDev"
#JACSDATA_DIR="/groups/scicomp/jacsData"
EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"
SCRIPT_DIR="$JACSDATA_DIR/servers/$SERVER/scripts"
TEMPLATE_DIR="$JACSDATA_DIR/servers/$SERVER/templates"

SVN_OPTIONS="--trust-server-cert --non-interactive"

MAC_EXECUTOR_HOST="saffordt-wm1"
SSH_OPTIONS=""

COMPILE_DIR="$EXE_DIR/compile"
#VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_${JWVER}-redhat"
#VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_${JWVER}-fedora"
#NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_JaneliaWorkstation_${JWVER}-redhat"
#JACS_COMPILE_DIR="$COMPILE_DIR/jacs_JaneliaWorkstation_${JWVER}"


VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_Staging-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_Staging-fedora"
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_JaneliaWorkstation_Staging-redhat"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_JaneliaWorkstation_Staging"


STAGING_DIR="$EXE_DIR/JaneliaWorkstationStaging"
#TEMPLATE_DIR="$EXE_DIR/templates"

#PACKAGE_MAC_DIR="$STAGING_DIR/JaneliaWorkstation_${JWVER}"
#PACKAGE_LINUX_DIR="$STAGING_DIR/JaneliaWorkstation_linux_${JWVER}"

PACKAGE_MAC_DIR="$STAGING_DIR/JaneliaWorkstation_Staging"
PACKAGE_LINUX_DIR="$STAGING_DIR/JaneliaWorkstation_linux_Staging"
echo "Building Janelia Workstation version Staging (Part 1)"

################################################################
# Make sure the latest scripts are in an accessible place.
# (We already have these in $WORKSPACE, but only Hudson can 
# access that.)
################################################################
rm -rf $SCRIPT_DIR || true
#svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/JaneliaWorkstation_${JWVER}/buildprocess/scripts/hudson $SCRIPT_DIR
svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk/buildprocess/scripts/hudson $SCRIPT_DIR

################################################################
# Check out the versioned templates for making
# client packages 
################################################################
rm -rf $TEMPLATE_DIR || true
#svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/JaneliaWorkstation_${JWVER}/buildprocess/deployment/templates $TEMPLATE_DIR
svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk/buildprocess/deployment/templates $TEMPLATE_DIR


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
    #svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/tags/JaneliaWorkstation_${JWVER} $VAA3D_COMPILE_REDHAT_DIR
    svn $SVN_OPTIONS co https://svn.janelia.org/penglab/projects/vaa3d/trunk $VAA3D_COMPILE_REDHAT_DIR
    if [ ! -e $VAA3D_COMPILE_REDHAT_DIR ]; then
        #echo "SVN tag not found for Vaa3d: JaneliaWorkstation_${JWVER}"
        echo "SVN tag not found for Vaa3d: JaneliaWorkstation_Staging"
        exit 1
    fi
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/trunk/jba $VAA3D_COMPILE_REDHAT_DIR/jba
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/trunk/flybrainAlign/ireg $VAA3D_COMPILE_REDHAT_DIR/released_plugins/v3d_plugins/ireg
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/trunk/imath $VAA3D_COMPILE_REDHAT_DIR/released_plugins/v3d_plugins/imath

    cp "$SCRIPT_DIR/build_vaa3d_linux.sh" $VAA3D_COMPILE_REDHAT_DIR
    cp "$SCRIPT_DIR/qsub_vaa3d_build.sh" $VAA3D_COMPILE_REDHAT_DIR
    cp -R $VAA3D_COMPILE_REDHAT_DIR $VAA3D_COMPILE_FEDORA_DIR

    echo "  Building Vaa3D for the grid (in the background)"
    #echo "sh \"$VAA3D_COMPILE_REDHAT_DIR/qsub_vaa3d_build.sh\" $JWVER $SERVER" > "$VAA3D_COMPILE_REDHAT_DIR/build.sh"
    echo "sh \"$VAA3D_COMPILE_REDHAT_DIR/qsub_vaa3d_build.sh\" Staging $SERVER" > "$VAA3D_COMPILE_REDHAT_DIR/build.sh"
    qsub -l short=true -sync y "$VAA3D_COMPILE_REDHAT_DIR/build.sh" &
    VAA3D_QSUB_PID=$!

    echo "  Building Vaa3D for the linux client"
    cd $VAA3D_COMPILE_FEDORA_DIR
    sh build_vaa3d_linux.sh
fi

################################################################
# Build NeuronSeparator
################################################################
if [ $BUILD_NEUSEP == 1 ]; then
    echo "Building NeuronSeparator (not really; just checking it out and grabbing the binaries)"
    cd $COMPILE_DIR

    echo "  Removing $NEUSEP_COMPILE_REDHAT_DIR"
    rm -rf $NEUSEP_COMPILE_REDHAT_DIR || true
    
    echo "  Checking out from SVN"
    #svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/tags/JaneliaWorkstation_${JWVER} $NEUSEP_COMPILE_REDHAT_DIR
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/trunk $NEUSEP_COMPILE_REDHAT_DIR
    if [ ! -e $NEUSEP_COMPILE_REDHAT_DIR ]; then
        #echo "SVN tag not found for NeuronSeparator: JaneliaWorkstation_${JWVER}"
        echo "SVN tag not found for NeuronSeparator: JaneliaWorkstation_Staging"
        exit 1
    fi
    #cp "$SCRIPT_DIR/qsub_neusep_build.sh" $NEUSEP_COMPILE_REDHAT_DIR
    #echo "  Building NeuronSeparator for the grid (in the background)"
    #echo "sh \"$NEUSEP_COMPILE_REDHAT_DIR/qsub_neusep_build.sh\" $JWVER $SERVER" > "$NEUSEP_COMPILE_REDHAT_DIR/build.sh"
    #qsub -sync y "$NEUSEP_COMPILE_REDHAT_DIR/build.sh" &
    #NEUSEP_QSUB_PID=$!
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
    #svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/JaneliaWorkstation_${JWVER} $JACS_COMPILE_DIR
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk $JACS_COMPILE_DIR
    if [ ! -e $JACS_COMPILE_DIR ]; then
        #echo "SVN tag not found for jacs: JaneliaWorkstation_${JWVER}"
        echo "SVN tag not found for jacs: JaneliaWorkstation_Staging"
        exit 1
    fi

    echo "  Building Jacs"
    cd $JACS_COMPILE_DIR
    cd buildprocess
    ant -buildfile build-all.xml
    cd ../console
    ant "build-run-jar"
fi

################################################################
# Build Janelia Workstation Deployment Packages
################################################################
if [[ $SERVER == "jacs-staging" ]] && [[ $BUILD_JANELIAWORKSTATION == 1 ]]; then
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
    cp $VAA3D_COMPILE_FEDORA_DIR/bin/vaa3d $PACKAGE_LINUX_DIR
fi

echo "Waiting for Vaa3d qsub ($VAA3D_QSUB_PID)..." 
wait $VAA3D_QSUB_PID

echo "Waiting for Neusep qsub ($NEUSEP_QSUB_PID)..." 
wait $NEUSEP_QSUB_PID

echo "We're all done with part 1 compilations:" 
echo "  Vaa3d (Fedora): $VAA3D_COMPILE_FEDORA_DIR"
echo "  Vaa3d (Redhat): $VAA3D_COMPILE_REDHAT_DIR"
echo "  Neusep (Redhat): $NEUSEP_COMPILE_REDHAT_DIR"
echo "  Jacs: $JACS_COMPILE_DIR"
echo "  Linux Package: $PACKAGE_LINUX_DIR"
echo "  Mac Package (to be completed in part 2): $PACKAGE_MAC_DIR"
echo ""

if [ $RUN_PART2 == 1 ]; then
    echo "Now running part 2 on a Mac..."
    ssh $SSH_OPTIONS $MAC_EXECUTOR_HOST "sh $SCRIPT_DIR/build_jacsstaging_part2_mac.sh $JWVER $SERVER $PART2_BUILD_VAA3D $PART2_BUILD_JANELIAWORKSTATION"
fi

