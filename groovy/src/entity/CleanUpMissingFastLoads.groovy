package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.model.entity.Entity;

// Globals
username = "system"
f = new JacsUtils(username, false)
e = f.e
c = f.c

int numTotal = 0
int numDeleted = 0

for(Entity fastLoad : e.getUserEntitiesByName(null, "Fast Load")) {
	
	println "Processing "+fastLoad.id
	f.loadChildren(fastLoad)
	
	numTotal++;
	if (fastLoad.children.size()!=28) {
		println separation.id+" "+fastLoad.children.size()
		f.e.deleteSmallEntityTree(username, fastLoad.id)
		numDeleted++;
	}

	fastLoad.entityData = null
}

println "Total: "+numTotal
println "Deleted: "+numDeleted

