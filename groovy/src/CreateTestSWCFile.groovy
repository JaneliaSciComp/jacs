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
 * returns a step adder that adds a step in a direction similar to the previous
 * step (if there is no previous step, goes in +x direction)
 *
 * input angles are the half-width of the range from which the angle change
 * is uniformly sampled (theta = polar, phi = azimuthal angle)
 */
def angleStepAdder = {
    double dTheta, double dPhi, double stepsize, SWCNode parentNode, nodeList ->
    if (parentNode.getParentIndex() == -1) {
        dx = stepsize
        dy = 0
        dz = 0
    } else {
        x1 = parentNode.getX()
        y1 = parentNode.getY()
        z1 = parentNode.getZ()
        grandparentNode = nodeList[parentNode.getParentIndex() - 1]
        x2 = grandparentNode.getX()
        y2 = grandparentNode.getY()
        z2 = grandparentNode.getZ()

        r = Math.sqrt((x1 - x2) ** 2 + (y1 - y2) ** 2 + (z1 - z2) **2)
        phi = Math.atan2(y1 - y2, x1 - x2)
        theta = Math.acos((z1 - z2) / r)

        phi += dPhi * 2 * (Math.random() - 0.5)
        theta += dTheta * 2 * (Math.random() - 0.5)

        sintheta = Math.sin(theta)
        dx = sintheta * Math.cos(phi) * stepsize
        dy = sintheta * Math.sin(phi) * stepsize
        dz = Math.cos(theta) * stepsize
    }
    nodeList << new SWCNode(nodeList.size() + 1, 0,
        parentNode.getX() + dx,
        parentNode.getY() + dy,
        parentNode.getZ() + dz,
        1.0, parentNode.getIndex()
    )
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
 * create a new nodelist with one root node at the input location
 */
def createNeuronRoot = {
    originx, originy, originz -> [new SWCNode(1, 0, originx, originy, originz, 1.0, -1)]
}

/**
 * create a neuron where each "generation" of branches uses the same
 * algorithms
 */
def createSimpleNeuron = {
    nodeList, branchCounter, branchAdder, nGenerations ->
    nGenerations.times {nodeList = addGeneration(nodeList, branchCounter, branchAdder)}
    return nodeList
}

/**
 * create a new neuron where each "generation" is built using
 * a counter and adder from a list (one pair per generation)
 */
 def createComplexNeuron = {
    nodeList, counterAdderList ->
    // annoyingly, you can't do "for x, y in list", as far as I can tell
    for (pair in counterAdderList) {
        nodeList = addGeneration(nodeList, *pair)
    }
    return nodeList
 }



// given all that, you create a neuron by combining low-level
//  closures, currying out their "fixed" parameters, and passing
//  them to a neuron builder:
myBranchCounter = constantBranchCounter.curry(2)
myStepAdder = constantStepAdder.curry(2.0, 3.0, 5.0)
myBranchAdder = addBranch.curry(3, myStepAdder)
// nodeList = createSimpleNeuron(createNeuronRoot(0.0, 0.0, 0.0), myBranchCounter, myBranchAdder, 2)


// the numbers aren't great here, but it basically works
nodeList = createComplexNeuron(
    // these numbers correspond to the middle of sample 2014-02-27
    createNeuronRoot(7000.0, 10000.0, 3200.0),
    [
        [constantBranchCounter.curry(1), addBranch.curry(3, constantStepAdder.curry(100.0, 0.0, 0.0))],
        [constantBranchCounter.curry(2), addBranch.curry(1, angleStepAdder.curry(0.0, 0.5, 100.0))],
        [constantBranchCounter.curry(1), addBranch.curry(5, constantStepAdder.curry(100.0, 0.0, 0.0))],
        [constantBranchCounter.curry(2), addBranch.curry(1, angleStepAdder.curry(0.0, 0.5, 100.0))],
        [constantBranchCounter.curry(1), addBranch.curry(5, constantStepAdder.curry(100.0, 0.0, 0.0))]
    ])



// create the SWC data object and write out the file
neuronData = new SWCData(nodeList, defaultHeaderList)
neuronData.write(new File(filename))

// debug: read it back and print it out:
new File(filename).eachLine {line -> print line + '\n'}