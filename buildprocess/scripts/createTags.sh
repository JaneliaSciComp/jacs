#!/bin/sh
# 
# This script creates tags for the branches of all FlyWorkstation projects. 
# If a build needs a non-HEAD version of any project then you're on your 
# own with the SVN copying.
#
if [ ! $1 ]; then
    echo "Specify a branch version"
    exit;
fi
if [ ! $1 ]; then
    echo "Specify a tag version"
    exit;
fi
FWVER=$1
TAGVER=$1

svn copy https://svn.janelia.org/penglab/projects/vaa3d/branches/FlySuite_${FWVER} https://svn.janelia.org/penglab/projects/vaa3d/tags/FlySuite_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${FWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/branches/FlySuite_${FWVER} https://subversion.int.janelia.org/ScientificComputing/Projects/jacs/tags/FlySuite_${FWVER} -m "Creating a tag for release ${TAGVER} from branch ${FWVER}"

svn copy https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/branches/FlySuite_${FWVER} https://subversion.int.janelia.org/ScientificComputing/Projects/NeuronSeparator/tags/FlySuite_${TAGVER} -m "Creating a tag for release ${TAGVER} from branch ${FWVER}"

