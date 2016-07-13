package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import java.text.SimpleDateFormat

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

// Parameters
tilingPattern = "OPTIC_TILE"
targetFolderName = "30 Random Optic Lobes (5/1-5/15)"
username1 = "kimmelb"
username2 = "zugatesc"

// Globals
f1 = new JacsUtils(username1, false)
a1 = f1.a
f2 = new JacsUtils(username2, false)
a2 = f2.a
df = new SimpleDateFormat()
now = new Date()



// Main script
opticLobes = []
a1.getEntitiesWithAttributeValue(ATTRIBUTE_TILING_PATTERN, tilingPattern).each {
	if (((((now.time-it.creationDate.time)/1000)/60)/60) < (15*24)) {
		println it.name+" creationDate:"+df.format(it.creationDate) 
		tree = a1.getEntityTree(it.id)
		
//		supportingData = EntityUtils.findChildWithName(tree, "Supporting Files")
//		pairs = supportingData.getChildrenOfType(EntityConstants.TYPE_LSM_STACK_PAIR)
//		lsmFile = pairs.find().children.find()
//		filepath = lsmFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
//		PathTranslator pt = new PathTranslator()
//		File file = new File(pt.convertPath(filepath))
//		println file.lastModified()
//		println ((((now.time-file.lastModified())/1000)/60)/60)
		
		ids = getAllEntityIdsInTree(tree)
		annotations1 = a1.getAnnotationsForEntities(username1, ids)
		annotations2 = a1.getAnnotationsForEntities(username2, ids)
		
		println "        Annotations from "+username1+":"+annotations1.size()
		println "        Annotations from "+username2+":"+annotations2.size()
		if (annotations1.isEmpty() && annotations2.isEmpty()) {
			
			latestSep = tree.getLatestChildOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
			if (latestSep!=null) {
				frags = latestSep.getDescendantsOfType(EntityConstants.TYPE_NEURON_FRAGMENT)
				println "        "+latestSep.name+" frags:"+frags.size()+" creationDate:"+df.format(it.creationDate)
				opticLobes.add(it)
			}
			
		}
	}
	
}

Collections.shuffle(opticLobes)
println "Total lobes: "+opticLobes.size()

l1 = opticLobes.subList(0, 30) 
l2 = opticLobes.subList(30, 60) 

addToFolder(f1, targetFolderName, l1)
addToFolder(f2, targetFolderName, l2)

def addToFolder(JacsUtils f, String targetFolderName, list) {
	Entity newFolder = f.newEntity(targetFolderName, TYPE_FOLDER);
	newFolder.addAttributeAsTag(ATTRIBUTE_COMMON_ROOT);
	newFolder = f.a.saveOrUpdateEntity(newFolder);
	childIds = list.collect { it.id }
	f.a.addChildren(f.user.userLogin, newFolder.id, childIds, ATTRIBUTE_ENTITY)
}

def getAllEntityIdsInTree(Entity tree) {
	ids = []
	ids.add(tree.id)
	tree.children.each {
		ids.addAll(getAllEntityIdsInTree(it))
	}
	return ids
}

