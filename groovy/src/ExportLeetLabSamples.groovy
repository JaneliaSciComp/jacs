
import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.compute.api.SolrBeanRemote
import org.janelia.it.jacs.model.entity.Entity;

// Globals
username = "system"
JacsUtils f = new JacsUtils(username, false)
e = f.e
c = f.c
SolrBeanRemote s = f.s

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

// +(subjects:"rokickik" OR subjects:"leetlab" OR subjects:"flylight" OR subjects:"admin") +doc_type:ENTITY 
// -entity_type:Ontology* AND ((+44F03 +BLM) OR rokickik_annotations:(+44F03 +BLM) OR rokickik_annotations_exact:(+44F03 +BLM) 
// OR leetlab_annotations:(+44F03 +BLM) OR leetlab_annotations_exact:(+44F03 +BLM) OR flylight_annotations:(+44F03 +BLM) 
// OR flylight_annotations_exact:(+44F03 +BLM) OR admin_annotations:(+44F03 +BLM) OR admin_annotations_exact:(+44F03 +BLM))
