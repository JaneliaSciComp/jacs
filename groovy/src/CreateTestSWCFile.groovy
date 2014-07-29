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


def addStraight(int index, SWCNode parentNode, double dx, double dy, double dz) {
    new SWCNode(index, 0, parentNode.getX() + dx, parentNode.getY() + dy,
        parentNode.getZ() + dz, 1.0, parentNode.getIndex())
}


// create a fake neuron
nodeList = [new SWCNode(1, 0, originx, originy, originz, 1.0, -1)]
(2..nNodes).each {index -> nodeList << addStraight(index, nodeList[-1], stepsize, 0.0, 0.0)}


headerList = []
headerList << "# ORIGINAL_SOURCE CreateTestSWCFile.groovy"
headerList << "# OFFSET 0  0  0"

neuronData = new SWCData(nodeList, headerList)

// write it out
neuronData.write(new File(filename))

// debug: read it back and print it out:
new File(filename).eachLine {print it + '\n'}