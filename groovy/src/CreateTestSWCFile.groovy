import org.janelia.it.workstation.shared.util.SWCData
import org.janelia.it.workstation.shared.util.SWCNode


/**
 * create test swc files for use with large volume viewer
 *
 * djo, 7/14
 *
 */


// constants


nNodes = 10;

filename = "/Users/olbrisd/Downloads/myNeuron.swc"

originx = 0.0;
originy = 0.0;
originz = 0.0;

stepsize = 100.0



// create a fake neuron

// a staight line is a perfectly good neuron
nodeList = []
for (i in 1..nNodes) {
    nodeList << new SWCNode(i, 0, originx + i * stepsize, originy, originz, 1.0, i - 1)
}
// this is a hack
nodeList[0].setParentIndex(-1)


headerList = []
headerList << "# ORIGINAL_SOURCE CreateTestSWCFile.groovy"
headerList << "# OFFSET 0  0  0"

neuronData = new SWCData(nodeList, headerList)

// write it out
neuronData.write(new File(filename))

