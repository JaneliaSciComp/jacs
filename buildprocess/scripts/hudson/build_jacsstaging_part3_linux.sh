#!/bin/sh
#
# Hudson build script for the Workstation (part 3)
#
# The final part of the build script actually deploys the newly compiled
# executables and the Jacsstaging servers.
#

# Exit after any error
set -o errexit

JWVER=$1
SERVER=$2

SVN_OPTIONS="--trust-server-cert --non-interactive"

INSTALL_VAA3D=$3
INSTALL_NEUSEP=$4
INSTALL_SCRIPTS=$5
INSTALL_DATA_SERVER=$6
INSTALL_PROD_SERVER=$7
INSTALL_CLIENT=$8

JACSDATA_DIR="/groups/jacs/jacsDev"
EXE_DIR="$JACSDATA_DIR/servers/$SERVER/executables"

COMPILE_DIR="$EXE_DIR/compile"
#VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_${JWVER}-redhat"
#VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_${JWVER}-fedora"
#NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_JaneliaWorkstation_${JWVER}-redhat"
#JACS_COMPILE_DIR="$COMPILE_DIR/jacs_JaneliaWorkstation_${JWVER}"

VAA3D_COMPILE_REDHAT_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_Staging-redhat"
VAA3D_COMPILE_FEDORA_DIR="$COMPILE_DIR/vaa3d_JaneliaWorkstation_Staging-fedora"
NEUSEP_COMPILE_REDHAT_DIR="$COMPILE_DIR/neusep_JaneliaWorkstation_Staging-redhat"
JACS_COMPILE_DIR="$COMPILE_DIR/jacs_JaneliaWorkstation_Staging"

INSTALL_DIR="$EXE_DIR/install"
#VAA3D_INSTALL_REDHAT_DIR="$INSTALL_DIR/vaa3d_JaneliaWorkstation_${JWVER}-redhat"
#VAA3D_INSTALL_FEDORA_DIR="$INSTALL_DIR/vaa3d_JaneliaWorkstation_${JWVER}-fedora"
#NEUSEP_INSTALL_REDHAT_DIR="$INSTALL_DIR/neusep_JaneliaWorkstation_${JWVER}-redhat"

VAA3D_INSTALL_REDHAT_DIR="$INSTALL_DIR/vaa3d_JaneliaWorkstation_Staging-redhat"
VAA3D_INSTALL_FEDORA_DIR="$INSTALL_DIR/vaa3d_JaneliaWorkstation_Staging-fedora"
NEUSEP_INSTALL_REDHAT_DIR="$INSTALL_DIR/neusep_JaneliaWorkstation_Staging-redhat"

VAA3D_INSTALL_SYMLINK="$INSTALL_DIR/vaa3d-redhat"
NEUSEP_INSTALL_SYMLINK="$INSTALL_DIR/neusep-redhat"
#SCRIPTS_INSTALL_DIR="$INSTALL_DIR/scripts_${JWVER}"
SCRIPTS_INSTALL_DIR="$INSTALL_DIR/scripts_Staging"
SCRIPTS_INSTALL_SYMLINK="$INSTALL_DIR/scripts"
ALIGN_TEMPLATES_DIR="$JACSDATA_DIR/servers/$SERVER/AlignTemplates"
ALIGN_TEMPLATES_SYMLINK="$INSTALL_DIR/scripts/single_neuron/BrainAligner/AlignTemplates"

#JANELIAWORKSTATION_NAME="JaneliaWorkstation_${JWVER}"
#JANELIAWORKSTATION_LINUX_NAME="JaneliaWorkstation_linux_${JWVER}"

JANELIAWORKSTATION_NAME="JaneliaWorkstation_Staging"
JANELIAWORKSTATION_LINUX_NAME="JaneliaWorkstation_linux_Staging"

STAGING_DIR="$JACSDATA_DIR/servers/$SERVER/executables/JaneliaWorkstationStaging"
PACKAGE_MAC_DIR="$STAGING_DIR/$JANELIAWORKSTATION_NAME"
PACKAGE_LINUX_DIR="$STAGING_DIR/$JANELIAWORKSTATION_LINUX_NAME"

JANELIAWORKSTATION_CLIENTS_DIR="$JACSDATA_DIR/servers/$SERVER/executables/JaneliaWorkstation"
JANELIAWORKSTATION_INSTALL_DIR="$JANELIAWORKSTATION_CLIENTS_DIR/$JANELIAWORKSTATION_NAME"
JANELIAWORKSTATION_LINUX_INSTALL_DIR="$JANELIAWORKSTATION_CLIENTS_DIR/$JANELIAWORKSTATION_LINUX_NAME"
JANELIAWORKSTATION_TARBALL="$JANELIAWORKSTATION_INSTALL_DIR.tgz"
JANELIAWORKSTATION_LINUX_TARBALL="$JANELIAWORKSTATION_LINUX_INSTALL_DIR.tgz"

#echo "Installing Janelia Workstation version $JWVER (Part 3)"
echo "Installing Janelia Workstation version Staging (Part 3)"

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
    ln -s $ALIGN_TEMPLATES_DIR $ALIGN_TEMPLATES_SYMLINK
fi

################################################################
# Install Janelia Workstation Deployment Packages
################################################################
if [ $INSTALL_CLIENT == 1 ]; then
    echo "Installing deployment packages"
    
    rm -rf $JANELIAWORKSTATION_INSTALL_DIR || true
    mkdir -p $JANELIAWORKSTATION_INSTALL_DIR
    #cp -R $PACKAGE_MAC_DIR/JaneliaWorkstation.app $JANELIAWORKSTATION_INSTALL_DIR

    rm -rf $JANELIAWORKSTATION_LINUX_INSTALL_DIR || true
    cp -R $PACKAGE_LINUX_DIR $JANELIAWORKSTATION_LINUX_INSTALL_DIR

    #cd $JANELIAWORKSTATION_CLIENTS_DIR
    #echo "Sync filesystem"
    #sync
    #sleep 4

    echo "Create tarballs"
    cd $STAGING_DIR
    tar cvfz "$JANELIAWORKSTATION_TARBALL" $JANELIAWORKSTATION_NAME
    tar cvfz "$JANELIAWORKSTATION_LINUX_TARBALL" $JANELIAWORKSTATION_LINUX_NAME
    
    #echo "Janelia Workstation Version ${JWVER} (client) was successfully installed into the following locations:"
    echo "Janelia Workstation Version Staging (client) was successfully installed into the following locations:"
    echo "  Mac: $JANELIAWORKSTATION_INSTALL_DIR"
    echo "  Linux: $JANELIAWORKSTATION_LINUX_INSTALL_DIR"
    echo ""
    echo "Tarballs are also available (these are used by the auto-updater):"
    echo "  Mac: $JANELIAWORKSTATION_TARBALL"
    echo "  Linux: $JANELIAWORKSTATION_LINUX_TARBALL"
    echo ""
fi

################################################################
# Install Jacs to the jacs-data (data refresh) server
################################################################
if [ $INSTALL_DATA_SERVER == 1 ]; then
    echo "  Deploying to server 'jacs-data'..."
    cd $JACS_COMPILE_DIR/compute
    ant -Duser.server.machine=jacs-staging -Duser.server.login=jacstest "deploy-[your-server]-dev"
    #echo "Janelia Workstation Version ${JWVER} (JBoss server) was successfully deployed to the JACS STAGING production data-loading server."
    echo "Janelia Workstation Version Staging (JBoss server) was successfully deployed to the JACS STAGING production data-loading server."
    cd $JACS_COMPILE_DIR/jacs
    ant -Duser.server.machine=jacs-staging -Duser.server.login=jacstest "deploy-[your-server]-dev"
    #echo "Janelia Workstation Version ${JWVER} (Tomcat web front-end) was successfully deployed to the JACS STAGING production data-loading server."
    echo "Janelia Workstation Version Staging (Tomcat web front-end) was successfully deployed to the JACS STAGING production data-loading server."
fi 
    
################################################################
# Install Jacs to the jacs (production) server
################################################################
if [ $INSTALL_PROD_SERVER == 1 ]; then
    echo "  Deploying to server 'jacsstaging'..."
    cd $JACS_COMPILE_DIR/compute
    ant -Duser.server.machine=jacs-staging -Duser.server.login=jacstest "deploy-[your-server]-dev"
    #echo "Janelia Workstation Version ${JWVER} (JBoss server) was successfully deployed to the JACS STAGING production server."
    echo "Janelia Workstation Version Staging (JBoss server) was successfully deployed to the JACS STAGING production server."
    cd $JACS_COMPILE_DIR/jacsstaging
    ant -Duser.server.machine=jacs-staging -Duser.server.login=jacstest "deploy-[your-server]-dev"
    #echo "Janelia Workstation Version ${JWVER} (Tomcat web front-end) was successfully deployed to the JACS STAGING production server."
    echo "Janelia Workstation Version Staging (Tomcat web front-end) was successfully deployed to the JACS STAGING production server."
fi


