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

/**
 * given a parent node, return a new child node in the direction given
 */
def addStraight(int index, SWCNode parentNode, double dx, double dy, double dz) {
    new SWCNode(index, 0, parentNode.getX() + dx, parentNode.getY() + dy,
        parentNode.getZ() + dz, 1.0, parentNode.getIndex())
}

/**
 * given a direction, return a closure that makes nodes in that direction
 */
def makeStraightAdder(double dx, double dy, double dz) {
    {int index, SWCNode parentNode -> addStraight(index, parentNode, dx, dy, dz)}
}



// create a fake neuron (basic)
// nodeList = [new SWCNode(1, 0, originx, originy, originz, 1.0, -1)]
// (2..nNodes).each {index -> nodeList << addStraight(index, nodeList[-1], stepsize, 0.0, 0.0)}


// create a fake neuron (more generic)
nodeList = [new SWCNode(1, 0, originx, originy, originz, 1.0, -1)]
adder = makeStraightAdder(stepsize, 0.0, 0.0)
(2..nNodes).each {index -> nodeList << adder(index, nodeList.last())}

// this works but is less clear and more ugly
// (2..nNodes).each {index -> nodeList << makeStraightAdder(stepsize, 0.0, 0.0) (index, nodeList.last())}





headerList = []
headerList << "# ORIGINAL_SOURCE CreateTestSWCFile.groovy"
headerList << "# OFFSET 0  0  0"

neuronData = new SWCData(nodeList, headerList)

// write it out
neuronData.write(new File(filename))

// debug: read it back and print it out:
new File(filename).eachLine {print it + '\n'}