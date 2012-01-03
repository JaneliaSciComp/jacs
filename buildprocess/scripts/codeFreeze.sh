#!/bin/sh
# 
# This script creates tags for the trunks of all FlyWorkstation projects. 
# If a build needs a non-trunk version then you're on your own with the SVN copying.
#
if [ ! $1 ]; then
    echo "Specify a build version"
    exit;
fi
FWVER=$1

svn copy https://svn.janelia.org/penglab/projects/vaa3d/trunk https://svn.janelia.org/penglab/projects/vaa3d/tags/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}" 

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/trunk https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/tags/FlySuite_${FWVER} -m "Creating a branch for Workstation Suite, release ${FWVER}"

