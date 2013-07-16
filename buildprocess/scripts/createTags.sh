#!/bin/sh
# 
# This script creates tags for the branches of all Janelia Workstation projects.
# If a build needs a non-HEAD version of any project then you're on your 
# own with the SVN copying.
#
if [ ! $1 ]; then
    echo "Specify a branch version"
    exit;
fi
if [ ! $2 ]; then
    echo "Specify a tag version"
    exit;
fi
JWVER=$1
TAGVER=$2

svn copy https://svn.janelia.org/penglab/projects/vaa3d/branches/JaneliaWorkstation_${JWVER} https://svn.janelia.org/penglab/projects/vaa3d/tags/JaneliaWorkstation_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/JaneliaWorkstation_${JWVER} https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/JaneliaWorkstation_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/JaneliaWorkstation_${JWVER} https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/tags/JaneliaWorkstation_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${JWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/branches/JaneliaWorkstation_${JWVER} https://subversion.int.janelia.org/ScientificComputing/Projects/BrainAligner/tags/JaneliaWorkstation_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${JWVER}"
