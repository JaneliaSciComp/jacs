#
# Hudson build script for the Fly Workstation (part 1)
#
# This build script is meant to run on a Linux-based executor box, with 
# similar characteristics as the final Linux deployment client. This will 
# build the server, binary server tools, and the Linux clients. It will also
# create the Linux client distribution and the first part of a Mac client 
# distribution. The latter is completed in part 2 on a Mac executor.
#

export FWVER=$1
export JACSDATA_DIR="/groups/scicomp/jacsData"
export EXE_DIR="$JACSDATA_DIR/servers/jacs/executables"
export SCRIPT_DIR="$WORKSPACE"

export INSTALL_DIR="$EXE_DIR/install"
export VAA3D_DIR="$INSTALL_DIR/vaa3d.redhat"
export NEUSEP_DIR="$INSTALL_DIR/NeuronSeparator"

export STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
export TEMPLATE_DIR="$STAGING_DIR/templates"

# Build the Server Tools for the Grid
cd "$EXE_DIR/compile/"

################################################################
# Build Vaa3d for Redhat (Grid) and Fedora (Client) 
################################################################
svn co https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} vaa3d_FlySuite_${FWVER}-redhat
if [ ! -f "vaa3d_FlySuite_${FWVER}-redhat" ]; then
    echo "SVN tag not found for Vaa3d: FlySuite_${FWVER}"
    exit
fi
cp "$SCRIPT_DIR/build_vaa3d_linux.sh" vaa3d_FlySuite_${FWVER}-redhat/
cp -R vaa3d_FlySuite_${FWVER}-redhat vaa3d_FlySuite_${FWVER}-fedora

echo "Building Vaa3D for the grid"
qsub -sync "$SCRIPT_DIR/qsub_vaa3d_build.sh" $FWVER

echo "Building Vaa3D for the linux client"
cd vaa3d_FlySuite_${FWVER}-fedora
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
svn co https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/FlySuite_${FWVER} neusep_${FWVER}-redhat
if [ ! -f "vaa3d_FlySuite_${FWVER}-redhat" ]; then
    echo "SVN tag not found for NeuronSeparator: FlySuite_${FWVER}"
    exit
fi
echo "Building NeuronSeparator for the grid"
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
cd $WORKSPACE
svn co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/FlySuite_${FWVER} jacs_FlySuite_${FWVER}
if [ ! -f "jacs_FlySuite_${FWVER}" ]; then
    echo "SVN tag not found for jacs: FlySuite_${FWVER}"
    exit
fi

# TODO: deploy to jacs and jacs-data?


cd buildprocess
ant -buildfile build-all.xml
cd ../compute
ant "deploy-[your-server]-dev"
cd ../console
ant "build-run-jar"

echo "Creating deployment packages"
cd $STAGING_DIR
rm -rf workstation
rm -rf workstation_linux
cp -R $TEMPLATE_DIR/mac_template workstation
cp -R $TEMPLATE_DIR/linux_template workstation_linux
cp -R $WORKSPACE/jacs_FlySuite_${FWVER}/console/jars/* workstation/ 
cp -R $WORKSPACE/jacs_FlySuite_${FWVER}/console/jars/* workstation_linux/ 


