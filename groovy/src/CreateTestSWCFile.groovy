import org.janelia.it.workstation.shared.util.SWCData
import org.janelia.it.workstation.shared.util.SWCNode

/**
 * create test swc files for use with large volume viewer
 *
 * djo, 7/14
 *
 */


// constants
filename = "/Users/olbrisd/Downloads/myNeuron.swc"

defaultHeaderList = []
defaultHeaderList << "# ORIGINAL_SOURCE CreateTestSWCFile.groovy"
defaultHeaderList << "# OFFSET 0  0  0"


nNodes = 10;
originx = 0.0;
originy = 0.0;
originz = 0.0;
stepsize = 100.0

/**
 * returns a branch counter function that branches a constant number
 * of times
 */
def constantBranchCounter = {nBranches, node, nodeList -> nBranches}

/**
 * returns a step adder function that adds a constant step
 */
def constantStepAdder = {
    double dx, double dy, double dz, SWCNode parentNode, nodeList ->
    nodeList << new SWCNode(nodeList.size() + 1, 0,
        parentNode.getX() + dx, 
        parentNode.getY() + dy,
        parentNode.getZ() + dz, 
        1.0, parentNode.getIndex())
}

/**
 * given a nodelist and a step adder, add steps to a node list
 */
def addBranch = {
    nSteps, stepAdder, parentNode, nodeList ->
    nSteps.times {
        stepAdder(parentNode, nodeList)
        parentNode = nodeList.last()
    }
    return nodeList
}
// then:
// myBranchAdder = addBranch.curry(nsteps, stepadder)


/**
 * given a list of SWCNodes, return a list of those that have
 * no children
 */
def getTips(nodeList) {
    def tipNodes = [:]
    for (node in nodeList) {
        tipNodes[node.getIndex()] = node
        if (node.getParentIndex() in tipNodes) {
            tipNodes.remove(node.getParentIndex())
        }
    }
    return tipNodes.values()
}

/**
 * add a generation to a neuron
 * @param nodeList = existing list of nodes
 * @param branchCounter(node, nodeList): at node with nodeList, return
 *      how many branches to add at that node
 * @param branchAdder(node, nodeList) returns nodeList with branch added
 * @return nodeList = new (extended) list of nodes
 */
def addGeneration(nodeList, branchCounter, branchAdder) {
    for (tip in getTips(nodeList)) {
        branchCounter(tip, nodeList).times {
            nodeList = branchAdder(tip, nodeList)
        }
    }
    return nodeList
}

/**
 * create a neuron where each "generation" of branches uses the same
 * algorithms
 */
def createSimpleNeuron = {
    // probably ought to pass in the origin here, or maybe the start node?
    branchCounter, branchAdder, nGenerations ->
    nodeList = [new SWCNode(1, 0, 0.0, 0.0, 0.0, 1.0, -1)]
    nGenerations.times {nodeList = addGeneration(nodeList, branchCounter, branchAdder)}
    return nodeList
}

/*
// the obvious extension is different brancher and adder for each
//  generation (pseudocode):
createComplexNeuron(list (brancher, adder)) returns nodeList
new nodeList = [root]
for br, add in brancherAdderList:
    nodeList = addGeneration(nodeList, br, add)
return nodeList
 */


// given all that, you create a neuron by combining low-level
//  closures, currying out their "fixed" parameters, and passing
//  them to a neuron builder:
myBranchCounter = constantBranchCounter.curry(2)
myStepAdder = constantStepAdder.curry(2.0, 3.0, 5.0)
myBranchAdder = addBranch.curry(3, myStepAdder)
nodeList = createSimpleNeuron(myBranchCounter, myBranchAdder, 2)


// create the SWC data object and write out the file
neuronData = new SWCData(nodeList, defaultHeaderList)
neuronData.write(new File(filename))

// debug: read it back and print it out:
new File(filename).eachLine {line -> print line + '\n'}