#!/bin/sh
# 
# This script creates branches for the trunks of all Janelia Workstation projects.
# If a build needs a non-trunk version then you're on your own with the SVN copying.
#
if [ ! $1 ]; then
    echo "Specify a build version"
    exit;
fi
JWVER=$1

svn copy https://svn.janelia.org/penglab/projects/vaa3d/trunk https://svn.janelia.org/penglab/projects/vaa3d/branches/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/branches/JaneliaWorkstation_${JWVER} -m "Creating a branch for Workstation, release ${JWVER}"

