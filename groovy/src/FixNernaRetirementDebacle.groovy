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

int totalNumSamplesProcessed = 0
int totalNumFixed = 0

String subjectKey = null
f = new JacsUtils(subjectKey, false)
e = f.e

retiredDataFolders = e.getUserEntitiesByNameAndTypeName(subjectKey, "Retired Data", TYPE_FOLDER)
for(Entity retiredDataFolder : retiredDataFolders) {
	
	int numSamplesProcessed = 0
	int numFixed = 0
	
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
		
		Entity retiredSample20x = retiredSample20xEd==null?null:retiredSample20xEd.childEntity
		Entity retiredSample63x = retiredSample63xEd==null?null:retiredSample63xEd.childEntity
		
		String realName = retiredSample.name.replaceAll("-Retired","")
		realSamples = e.getEntitiesByNameAndTypeName(retiredSample.ownerKey, realName, TYPE_SAMPLE)
		if (realSamples.isEmpty()) {
			continue
		}
		println "  "+realSamples.size()+" duplicate samples"
		
		boolean matched20x = false
		boolean matched63x = false
		
		for (Entity realSample : realSamples) {
			
			f.loadChildren(realSample)
			
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
			
			if (realSample20x!=null && retiredSample20x!=null && retiredSample20x.id.equals(realSample20x.id)) {
				matched20x = true;
			}
			if (realSample63x!=null && retiredSample63x!=null && retiredSample63x.id.equals(realSample63x.id)) {
				matched63x = true;
			}
			
			if (matched20x && matched63x) {
				println "  Found matching active sample: "+realSample.id
				foundMatch = true
			}
			numSamplesProcessed++
			realSample.setEntityData(null)
		}
		
		if (matched20x) {
			println "  deleting retired 20x Eds "+retiredSample20xEd.id
			if (!DEBUG) {
				e.deleteEntityData(retiredSample20xEd.ownerKey, retiredSample20xEd.id)
			}
			numFixed++
		}
		
		if (matched63x) {
			println "  deleting retired 63x Eds "+retiredSample63xEd.id
			if (!DEBUG) {
				e.deleteEntityData(retiredSample63xEd.ownerKey, retiredSample63xEd.id)
			}
			numFixed++
		}
		
		retiredSample.setEntityData(null)
		
	}
	
	totalNumSamplesProcessed += numSamplesProcessed
	totalNumFixed += numFixed
	
	println "Processed "+numSamplesProcessed+" samples and fixed "+numFixed+" samples for "+retiredDataFolder.ownerKey
}

println "Processed "+totalNumSamplesProcessed+" samples and fixed "+totalNumFixed+" samples for all users"
