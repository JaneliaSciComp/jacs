package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

/**
 * This script walks all parent samples and makes sure that the LSMs listed at the parent level have
 * the same names as the LSMs in the child level. 
 */

// Globals
boolean DEBUG = true
boolean PRINTS = true
subjectKey = "user:system"
f = new JacsUtils(subjectKey, false)

int totalSamplesProcessed = 0
int totalLsmsRenamed = 0

println "Building list of users with data sets..."
Set<String> subjectKeys = new HashSet<String>()
for(Entity dataSet : f.e.getUserEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
	subjectKeys.add(dataSet.getOwnerKey());
}

for(String subjectKey : subjectKeys) {
	
	int numSamplesProcessed = 0
	int numLsmsRenamed = 0
	
	 println "Processing LSMs for "+subjectKey
	 f = new JacsUtils(subjectKey, false)

	 for(Entity sample : f.e.getUserEntitiesByTypeName(subjectKey, TYPE_SAMPLE)) {
		 
		 String objective = sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
		 if (objective!=null) continue
		 
		 f.loadChildren(sample)
		 childSamples = EntityUtils.getChildrenOfType(sample, TYPE_SAMPLE)
		 
		 if (childSamples.isEmpty()) continue
		 //println "Processing "+subjectKey+": "+sample.name
		 
		 Set<String> childLsms = new HashSet<String>()
		 for(Entity subSample : childSamples) {
					 
			f.loadChildren(subSample)
			Set<String> names = new HashSet<String>()
			Entity supportingData = EntityUtils.findChildWithName(subSample, "Supporting Files")
			if (supportingData==null) {
				println("  WARNING: could not find supporting files in sample#"+subSample.id)
				continue
			}
			f.loadChildren(supportingData)
			for(Entity tile : EntityUtils.getChildrenOfType(supportingData, TYPE_IMAGE_TILE)) {
				f.loadChildren(tile)
				for(Entity lsm : EntityUtils.getChildrenOfType(tile, TYPE_LSM_STACK)) {
					String filepath = lsm.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
					File file = new File(filepath)
					pathName = file.getName()
					if (!pathName.equals(lsm.name)) {
						println "  WARNING: lsm filepath does not match name ("+pathName+"!="+lsm.name+") on lsm#"+lsm.id
					}
					childLsms.add(lsm.name)
				}
			}
		 }
		 
		 if (childLsms.isEmpty()) continue;
		 
		 //println "  Checking for "+childLsms.size()+" LSMs in parent sample"
		 
		 Set<String> names = new HashSet<String>()
		 Entity supportingData = EntityUtils.findChildWithName(sample, "Supporting Files")
		 if (supportingData==null) {
			 println("  WARNING: could not find supporting files in sample#"+sample.id)
			 continue
		 }
		 f.loadChildren(supportingData)
		 for(Entity tile : EntityUtils.getChildrenOfType(supportingData, TYPE_IMAGE_TILE)) {
			 f.loadChildren(tile)
			 for(Entity lsm : EntityUtils.getChildrenOfType(tile, TYPE_LSM_STACK)) {
				 if (!childLsms.contains(lsm.name)) {
					 for(String childLsmName : childLsms) {
						 if (childLsmName.startsWith(lsm.name)) {
							 if (PRINTS) println "  Renaming "+lsm.name+" to "+childLsmName
							 lsm.setName(childLsmName)
							 if (!DEBUG) f.e.saveOrUpdateEntity(subjectKey, lsm)
				 			 numLsmsRenamed++
							 names.add(lsm.name)
							 break
						 }
					 } 
				 }
				 else {
					 names.add(lsm.name)
				 }
			 }
		 }
		 
		 if (!names.equals(childLsms)) {
			 println "  WARNING: LSM sets do not match: "+names+"!="+childLsms+" for sample#"+sample.id
		 }
 
		 sample.setEntityData(null)
		 numSamplesProcessed++
	 }
	 
	 println "Processed "+numSamplesProcessed+" "+subjectKey+" Samples"
	 println "Renamed "+numLsmsRenamed+" "+subjectKey+" LSMs"
	 totalSamplesProcessed += numSamplesProcessed
	 totalLsmsRenamed += numLsmsRenamed
}

println "Processed "+totalSamplesProcessed+" Samples"
println "Renamed "+totalLsmsRenamed+" LSMs"

