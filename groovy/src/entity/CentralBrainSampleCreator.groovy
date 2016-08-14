package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

// Parameters
username = "system"
rootEntityName = "FlyLight Single Neuron Data"
targetFolder = "Central Brains 2"
tiles = ["Left Dorsal Brain", "Right Dorsal Brain", "Ventral Brain"]

// Globals
f = new JacsUtils(username, false)
a = f.a

// Main script
dataFolder = f.loadChildren(a.getEntitiesByName(rootEntityName).find({ it.user.userLogin==username }))
centralBrainsFolder = f.loadChildren(f.verifyOrCreateChildFolder(dataFolder, targetFolder))
a.getEntitiesWithAttributeValue(ATTRIBUTE_TILING_PATTERN, "WHOLE_BRAIN").findAll({ it.user.userLogin==username }).each {
	sample = ensureCorrespondingCentralBrainSampleExists(it, centralBrainsFolder);
} 


def ensureCorrespondingCentralBrainSampleExists(sample, folder) {
	centralName = sample.name+"_Central_Brain_Tiles";
	println "Ensure that central brain sample "+centralName+" exists in " +folder.name
	
	newSample = folder.children.find({ it.entityTypeName=="Sample" && it.name==centralName })
	if (newSample!=null) return newSample
	
	newSample = f.createSample(centralName, "CENTRAL_BRAIN")
	f.addToParent(folder, newSample, null, ATTRIBUTE_ENTITY)

	newSupportingFiles = f.createSupportingFilesFolder()
	f.addToParent(newSample, newSupportingFiles, 0, ATTRIBUTE_SUPPORTING_FILES)
 
	supportingFiles = a.getEntityTree(sample.getChildByAttributeName(ATTRIBUTE_SUPPORTING_FILES).id)
	
	for(lsmStackPair in supportingFiles.children.findAll({ it.name in tiles })) {
		f.addToParent(newSupportingFiles, cloneLsmStackPair(lsmStackPair), null, ATTRIBUTE_ENTITY)
	}
	
	return sample
}

def cloneLsmStackPair(lsmStackPair) {
	newLsmStackPair = f.newEntity(lsmStackPair.name, TYPE_LSM_STACK_PAIR)
	newLsmStackPair = f.save(newLsmStackPair)
	println "Saved LSM stack pair for '"+newLsmStackPair.name+"' as "+newLsmStackPair.id
	
	lsmEntity1 = lsmStackPair.getChildByAttributeName(ATTRIBUTE_LSM_STACK_1)
	println "Adding LSM file to sample: "+lsmEntity1.name
	f.addToParent(newLsmStackPair, cloneLsmStack(lsmEntity1), 0, ATTRIBUTE_LSM_STACK_1)

    lsmEntity2 = lsmStackPair.getChildByAttributeName(ATTRIBUTE_LSM_STACK_2)
	println "Adding LSM file to sample: "+lsmEntity2.name
	f.addToParent(newLsmStackPair, cloneLsmStack(lsmEntity2), 1, ATTRIBUTE_LSM_STACK_2)
	
	return newLsmStackPair
}

def cloneLsmStack(lsmStack) {
	newLsmStack = f.newEntity(lsmStack.name, TYPE_LSM_STACK)
	newLsmStack.setValueByAttributeName(ATTRIBUTE_FILE_PATH, lsmStack.getValueByAttributeName(ATTRIBUTE_FILE_PATH))
	newLsmStack = f.save(newLsmStack)
	println "Saved LSM stack as "+newLsmStack.id 
	return newLsmStack
}

