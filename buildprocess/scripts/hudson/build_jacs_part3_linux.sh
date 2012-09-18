#!/bin/sh
#
# Hudson build script for the Fly Workstation (part 3)
#
# The final part of the build script actually deploys the newly compiled
# executables and the Jacs servers.
#

# Exit after any error
set -o errexit

FWVER=$1
SERVER=$2

SVN_OPTIONS="--trust-server-cert --non-interactive"

INSTALL_VAA3D=$3
INSTALL_NEUSEP=$4
INSTALL_SCRIPTS=$5
INSTALL_DATA_SERVER=$6
INSTALL_PROD_SERVER=$7
INSTALL_CLIENT=$8

JACSDATA_DIR="/groups/scicomp/jacsData"
EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"

COMPILE_DIR="$EXE_DIR/compile"
VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_FlySuite_${FWVER}-fedora"
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_FlySuite_${FWVER}-redhat"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_FlySuite_${FWVER}"

INSTALL_DIR="$EXE_DIR/install"
VAA3D_INSTALL_REDHAT_DIR="$INSTALL_DIR/vaa3d_FlySuite_${FWVER}-redhat"
VAA3D_INSTALL_FEDORA_DIR="$INSTALL_DIR/vaa3d_FlySuite_${FWVER}-fedora"
NEUSEP_INSTALL_REDHAT_DIR="$INSTALL_DIR/neusep_FlySuite_${FWVER}-redhat"
VAA3D_INSTALL_SYMLINK="$INSTALL_DIR/vaa3d-redhat"
NEUSEP_INSTALL_SYMLINK="$INSTALL_DIR/neusep-redhat"
SCRIPTS_INSTALL_DIR="$INSTALL_DIR/scripts_${FWVER}"
SCRIPTS_INSTALL_SYMLINK="$INSTALL_DIR/scripts"
ALIGN_TEMPLATES_DIR="$JACSDATA_DIR/AlignTemplates"
ALIGN_TEMPLATES_SYMLINK="$INSTALL_DIR/scripts/single_neuron/BrainAligner/AlignTemplates"

FLYSUITE_NAME="FlySuite_${FWVER}"
FLYSUITE_LINUX_NAME="FlySuite_linux_${FWVER}"

STAGING_DIR="$JACSDATA_DIR/FlySuiteStaging"
PACKAGE_MAC_DIR="$STAGING_DIR/$FLYSUITE_NAME"
PACKAGE_LINUX_DIR="$STAGING_DIR/$FLYSUITE_LINUX_NAME"

FLYSUITE_CLIENTS_DIR="$JACSDATA_DIR/FlySuite"
FLYSUITE_INSTALL_DIR="$FLYSUITE_CLIENTS_DIR/$FLYSUITE_NAME"
FLYSUITE_LINUX_INSTALL_DIR="$FLYSUITE_CLIENTS_DIR/$FLYSUITE_LINUX_NAME"
FLYSUITE_TARBALL="$FLYSUITE_INSTALL_DIR.tgz"
FLYSUITE_LINUX_TARBALL="$FLYSUITE_LINUX_INSTALL_DIR.tgz"

STAGING_FLYSUITE_LINUX_INSTALL_DIR="$STAGING_FLYSUITE_CLIENTS_DIR/$FLYSUITE_LINUX_NAME"

echo "Installing FlySuite version $FWVER (Part 3)"

################################################################
# Install Vaa3d for Redhat (Grid) and Fedora (Client) 
################################################################
if [ $INSTALL_VAA3D == 1 ]; then
    echo "Installing Vaa3D in $VAA3D_INSTALL_REDHAT_DIR"
    rm -rf $VAA3D_INSTALL_REDHAT_DIR || true
    cp -R $VAA3D_COMPILE_REDHAT_DIR/bin $VAA3D_INSTALL_REDHAT_DIR

    echo "Creating symbolic link at $VAA3D_INSTALL_SYMLINK"
    rm $VAA3D_INSTALL_SYMLINK || true
    ln -s $VAA3D_INSTALL_REDHAT_DIR $VAA3D_INSTALL_SYMLINK

    echo "Installing Vaa3D in $VAA3D_INSTALL_FEDORA_DIR"
    rm -rf $VAA3D_INSTALL_FEDORA_DIR || true
    cp -R $VAA3D_COMPILE_FEDORA_DIR/bin $VAA3D_INSTALL_FEDORA_DIR
fi

################################################################
# Install NeuronSeparator
################################################################
if [ $INSTALL_NEUSEP == 1 ]; then
    echo "Installing NeuronSeparator in $NEUSEP_INSTALL_REDHAT_DIR"
    rm -rf $NEUSEP_INSTALL_REDHAT_DIR || true
    mkdir -p $NEUSEP_INSTALL_REDHAT_DIR
    #cd "$NEUSEP_COMPILE_REDHAT_DIR/build_cmake.redhat"
    #cp mylib/sampsepNALoadRaw $NEUSEP_INSTALL_REDHAT_DIR
    #cp tools/setup4 $NEUSEP_INSTALL_REDHAT_DIR
    #cp tools/finish4 $NEUSEP_INSTALL_REDHAT_DIR
    cd $NEUSEP_COMPILE_REDHAT_DIR
    cp bin/* $NEUSEP_INSTALL_REDHAT_DIR

    rm $NEUSEP_INSTALL_SYMLINK || true
    echo "Creating symbolic link at $NEUSEP_INSTALL_SYMLINK"
    ln -s $NEUSEP_INSTALL_REDHAT_DIR $NEUSEP_INSTALL_SYMLINK
fi

################################################################
# Install Scripts
################################################################
if [ $INSTALL_SCRIPTS == 1 ]; then
    echo "Installing single neuron scripts in $SCRIPTS_INSTALL_DIR"
    rm -rf $SCRIPTS_INSTALL_DIR || true
    mkdir -p $SCRIPTS_INSTALL_DIR
    svn $SVN_OPTIONS co https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk/compute/scripts/single_neuron $SCRIPTS_INSTALL_DIR/single_neuron

    rm $SCRIPTS_INSTALL_SYMLINK || true
    echo "Creating symbolic link at $SCRIPTS_INSTALL_SYMLINK"
    ln -s $SCRIPTS_INSTALL_DIR $SCRIPTS_INSTALL_SYMLINK 

    rm $ALIGN_TEMPLATES_SYMLINK || true
    echo "Creating symbolic links at $ALIGN_TEMPLATES_SYMLINK"
    if [ $SERVER == "jacs" ]; then
       $ln -s $ALIGN_TEMPLATES_DIR $ALIGN_TEMPLATES_SYMLINK
    fi
    if [ $SERVER == "jacs-staging" ]; then
       $ln -s $STAGING_ALIGN_TEMPLATES_DIR $ALIGN_TEMPLATES_SYMLINK
    fi
fi

################################################################
# Install FlySuite Deployment Packages
################################################################
if [ $INSTALL_CLIENT == 1 ] && [ $SERVER == "jacs" ]; then
    echo "Installing deployment packages"
    
    rm -rf $FLYSUITE_INSTALL_DIR || true
    mkdir -p $FLYSUITE_INSTALL_DIR
    cp -R $PACKAGE_MAC_DIR/FlySuite.app $FLYSUITE_INSTALL_DIR

    rm -rf $FLYSUITE_LINUX_INSTALL_DIR || true
    cp -R $PACKAGE_LINUX_DIR $FLYSUITE_LINUX_INSTALL_DIR

    #cd $FLYSUITE_CLIENTS_DIR
    #echo "Sync filesystem"
    #sync
    #sleep 4

    echo "Create tarballs"
    cd $STAGING_DIR
    tar cvfz "$FLYSUITE_TARBALL" $FLYSUITE_NAME
    tar cvfz "$FLYSUITE_LINUX_TARBALL" $FLYSUITE_LINUX_NAME
    
    echo "FlySuite Version ${FWVER} (client) was successfully installed into the following locations:"
    echo "  Mac: $FLYSUITE_INSTALL_DIR"
    echo "  Linux: $FLYSUITE_LINUX_INSTALL_DIR"
    echo ""
    echo "Tarballs are also available (these are used by the auto-updater):"
    echo "  Mac: $FLYSUITE_TARBALL"
    echo "  Linux: $FLYSUITE_LINUX_TARBALL"
    echo ""
fi

if [ $INSTALL_CLIENT == 1 ] && [ $SERVER == "jacs-staging" ]; then
    echo "Installing deployment packages"
    
    rm -rf $STAGING_FLYSUITE_INSTALL_DIR || true
    mkdir -p $STAGING_FLYSUITE_INSTALL_DIR
    cp -R $STAGING_PACKAGE_MAC_DIR/FlySuite.app $STAGING_FLYSUITE_INSTALL_DIR

    rm -rf $STAGING_FLYSUITE_LINUX_INSTALL_DIR || true
    cp -R $STAGING_PACKAGE_LINUX_DIR $STAGING_FLYSUITE_LINUX_INSTALL_DIR

    cd $STAGING_FLYSUITE_CLIENTS_DIR
    echo "Sync filesystem"
    sync
    sleep 2

    echo "Create tarballls"
    tar cvfz $FLYSUITE_TARBALL $FLYSUITE_NAME
    tar cvfz $FLYSUITE_LINUX_TARBALL $FLYSUITE_LINUX_NAME
    
    echo "FlySuite Version ${FWVER} (client) was successfully installed into the following locations:"
    echo "  Mac: $STAGING_FLYSUITE_INSTALL_DIR"
    echo "  Linux: $STAGING_FLYSUITE_LINUX_INSTALL_DIR"
    echo ""
    echo "Tarballs are also available (these are used by the auto-updater):"
    echo "  Mac: $STAGING_FLYSUITE_CLIENTS_DIR/$FLYSUITE_TARBALL"
    echo "  Linux: $STAGING_FLYSUITE_CLIENTS_DIR/$FLYSUITE_LINUX_TARBALL"
    echo ""
fi

################################################################
# Install Jacs to the jacs-data (data refresh) server
################################################################
if [ $INSTALL_DATA_SERVER == 1 ]; then
    echo "  Deploying to server 'jacs-data'..."
    cd $JACS_COMPILE_DIR/compute
    ant -Duser.server.machine=jacs-data -Duser.server.login=jacs "deploy-[your-server]-dev"
    echo "FlySuite Version ${FWVER} (JBoss server) was successfully deployed to the JACS production data-loading server."
    cd $JACS_COMPILE_DIR/jacs
    ant -Duser.server.machine=jacs-data -Duser.server.login=jacs "deploy-[your-server]-dev"
    echo "FlySuite Version ${FWVER} (Tomcat web front-end) was successfully deployed to the JACS production data-loading server."
fi 
    
################################################################
# Install Jacs to the jacs (production) server
################################################################
if [ $INSTALL_PROD_SERVER == 1 ]; then
    echo "  Deploying to server 'jacs'..."
    cd $JACS_COMPILE_DIR/compute
    ant -Duser.server.machine=jacs -Duser.server.login=jacs "deploy-[your-server]-dev"
    echo "FlySuite Version ${FWVER} (JBoss server) was successfully deployed to the JACS production server."
    cd $JACS_COMPILE_DIR/jacs
    ant -Duser.server.machine=jacs -Duser.server.login=jacs "deploy-[your-server]-dev"
    echo "FlySuite Version ${FWVER} (Tomcat web front-end) was successfully deployed to the JACS production server."
fi


