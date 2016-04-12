import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

/**
 * This script goes through samples in Retired Data and finds corresponding unretired samples.
 * When the sub-samples are the same objects, they are unlinked from the retired sample.
 */

// Globals
boolean DEBUG = true
int numSamplesProcessed = 0
int numFixed = 0

subjectKey = "user:nerna"
f = new JacsUtils(subjectKey, false)
e = f.e

retiredDataFolders = e.getUserEntitiesByNameAndTypeName(subjectKey, "Retired Data", TYPE_FOLDER)
if (retiredDataFolders.size()>1) {
	throw new Exception("ERROR: More than one Retired Data folder")
}
Entity retiredDataFolder = retiredDataFolders.iterator().next()

f.loadChildren(retiredDataFolder)

List retiredEds = new ArrayList(retiredDataFolder.getOrderedEntityData())

for(EntityData retiredSampleEd : retiredEds) {
	
	if (retiredSampleEd.childEntity==null) continue
	Entity retiredSample = retiredSampleEd.childEntity
	
	println retiredSample.name+" ("+retiredSample.id+")"
	
	f.loadChildren(retiredSample)
	
	EntityData retiredSample20xEd
	EntityData retiredSample63xEd
	for(EntityData ed : retiredSample.getEntityData()) {
		Entity subSample = ed.childEntity
		if (subSample!=null && subSample.entityTypeName.equals(TYPE_SAMPLE)) {
			objective = subSample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
			if ("20x".equals(objective)) {
				retiredSample20xEd = ed
			}
			else if ("63x".equals(objective)) {
				retiredSample63xEd = ed
			}
		}
	}
	
	if (retiredSample20xEd==null||retiredSample63xEd==null) {
		continue
	}
	
	Entity retiredSample20x = retiredSample20xEd.childEntity
	Entity retiredSample63x = retiredSample63xEd.childEntity
	
	String realName = retiredSample.name.replaceAll("-Retired","")
	realSamples = e.getEntitiesByNameAndTypeName(subjectKey, realName, TYPE_SAMPLE)
	if (realSamples.isEmpty()) {
		continue
	}
	println "  "+realSamples.size()+" duplicate samples"
	
	boolean foundMatch = false
	
	for (Entity realSample : realSamples) {
		
		f.loadChildren(realSample)
		Entity dataSetFolder = e.getAncestorWithType(subjectKey, realSample.id, TYPE_FOLDER)
		
		Entity realSample20x
		Entity realSample63x
		for(Entity subSample : EntityUtils.getChildrenOfType(realSample, TYPE_SAMPLE)) {
			objective = subSample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
			if ("20x".equals(objective)) {
				realSample20x = subSample;
			}
			else if ("63x".equals(objective)) {
				realSample63x = subSample;
			}
		}
		
		matched20x = false
		if (realSample20x!=null && retiredSample20x.id.equals(realSample20x.id)) {
			matched20x = true;
		}
		matched63x = false
		if (realSample63x!=null && retiredSample63x.id.equals(realSample63x.id)) {
			matched63x = true;
		}
		
		if (matched20x && matched63x) {
			println "  Found matching active sample: "+realSample.id
			foundMatch = true
		}
		numSamplesProcessed++
		realSample.setEntityData(null)
	}
	
	if (foundMatch) {
		println "  deleting retired Eds "+retiredSample20xEd.id+" and"+retiredSample63xEd.id
		
		if (!DEBUG) {
			e.deleteEntityData(subjectKey, retiredSample20xEd.id)
			e.deleteEntityData(subjectKey, retiredSample63xEd.id)
		}
		
		numFixed++
	}
	
	retiredSample.setEntityData(null)
}

println "Processed "+numSamplesProcessed+" samples"
println "Fixed "+numFixed+" samples"

