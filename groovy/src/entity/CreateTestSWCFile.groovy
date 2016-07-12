package entity

import org.janelia.it.jacs.shared.swc.SWCData
import org.janelia.it.jacs.shared.swc.SWCNode

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


/*
// first basic example: you create a neuron by combining low-level
//  closures, currying out their "fixed" parameters, and passing
//  them to a neuron builder:
myBranchCounter = constantBranchCounter.curry(2)
myStepAdder = constantStepAdder.curry(2.0, 3.0, 5.0)
myBranchAdder = addBranch.curry(3, myStepAdder)
nodeList = createSimpleNeuron(createNeuronRoot(0.0, 0.0, 0.0), myBranchCounter, myBranchAdder, 2)
*/

/*
// more complicated example: create a neuron by alternating branching and nonbranching sections
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
*/

// sample 2014-03-14 test
// top middle is close to 5500, 1800, 1600; bottom is at about y = 10000
// build a neuron that covers a lot of that space, with a controllable
// total number of nodes

// we can express total number of nodes N in terms of
//  G generations of B branches, with each branch having S steps:
// N = S * (B ^ G - 1) / (B - 1)
// length in steps (if it were straight) L = G * S
/* useful numbers:
In [3]: for g in range(3, 11):
   ...:     for b in range(2, 4):
   ...:         print (b ** g - 1) / (b - 1),
   ...:     print
   ...:
7 13
15 40
31 121
63 364
127 1093
255 3280
511 9841
1023 29524
 */

// N ~ 300, L = 50
nGenerations = 5
nBranchings = 2
nSteps = 10
stepsize = 100.0

// halve the stepsize, double the generations from first
// N ~ 10000, L = 100
// nGenerations = 10
// nBranchings = 2
// nSteps = 10
// stepsize = 50.0

// aim for something in between:
// N ~ 1200, L = 70
// nGenerations = 7
// nBranchings = 2
// nSteps = 10
// stepsize = 100.0

creatorList = []
nGenerations.times {
    creatorList << [constantBranchCounter.curry(1), addBranch.curry(nSteps, constantStepAdder.curry(0.0, stepsize, 0.0))]
    creatorList << [constantBranchCounter.curry(nBranchings), addBranch.curry(1, angleStepAdder.curry(0.0, 0.5, stepsize))]
}
nodeList = createComplexNeuron(
    createNeuronRoot(5500.0, 1800.0, 1600.0),
    creatorList
)





// create the SWC data object and write out the file
neuronData = new SWCData(nodeList, defaultHeaderList)
neuronData.write(new File(filename))

// debug: read it back and print it out (or at least part of it...):
println nodeList.size() + " total nodes"
// new File(filename).eachLine {line -> print line + '\n'}
new File(filename).withReader {reader -> 10.times {println reader.readLine()} }


