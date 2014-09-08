import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
boolean DEBUG = false
boolean PRINTS = false
int numSamplesProcessed = 0
int numTrulyRetired = 0

subjectKey = "user:nerna"
f = new JacsUtils(subjectKey, false)
e = f.e
    
Calendar cal = Calendar.getInstance();
cal.setTimeInMillis(0);
cal.set(2014, 8, 5, 0, 0, 0);
Date cutoff = cal.getTime();

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
	
	//if (!retiredSample.name.startsWith("GMR_SS00302-20130807_19_C4")) continue
	// GMR_SS00302-20130807_19_C4 20x not match
	// GMR_SS00784-20130926_19_H3 63 not match
	
	f.loadChildren(retiredSample)
	String realName = retiredSample.name.replaceAll("-Retired","")
	
	realSamples = e.getEntitiesByNameAndTypeName(subjectKey, realName, TYPE_SAMPLE)
	if (realSamples.isEmpty()) {
		System.out.println("  This is a truly retired sample, because we cannot find corresponding real sample: "+retiredSample.name)
		numTrulyRetired++
		continue;
	}
	
	List matchedSamples = new ArrayList()
	for(Entity realSample : realSamples) {
		if (realSample.creationDate.after(cutoff)) {
			matchedSamples.add(realSample)
		}
	}
	
	Entity retiredSample20x
	Entity retiredSample63x
	for(Entity subSample : EntityUtils.getChildrenOfType(retiredSample, TYPE_SAMPLE)) {
		objective = subSample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)
		if ("20x".equals(objective)) {
			retiredSample20x = subSample
		}
		else if ("63x".equals(objective)) {
			retiredSample63x = subSample
		}
	}
	
	boolean foundMatch = false
	for (Entity realSample : matchedSamples) {
		if (foundMatch) {
			System.out.println("WARNING: already dealt with retired sample "+retiredSample.id+". Skipping matched sample "+realSample.id)
			continue
		}
		if (realSample.creationDate.after(cutoff)) {
			
			f.loadChildren(realSample)
			
			Entity dataSetFolder = e.getAncestorWithType(subjectKey, realSample.id, TYPE_FOLDER)
			
			System.out.println(numSamplesProcessed+": "+retiredSample.name +" -> "+ realSample.name +" ("+dataSetFolder.name+")")
			
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
			
			if (!retiredSample20x.id.equals(realSample20x.id)) {
				System.out.println("  20x sub-samples do not match. Aborting sample processing for "+realName)
				continue
			}
			if (!retiredSample63x.id.equals(realSample63x.id)) {
				System.out.println("  63x sub-samples do not match. Aborting sample processing for "+realName)
				continue
			}
			
			if (PRINTS) System.out.println("  Removing current 'real' tree "+realSample.id)
			if (!DEBUG) f.e.deleteEntityTreeById(subjectKey, realSample.id)
			
			if (PRINTS) System.out.println("  Add retired sample to data set folder "+dataSetFolder.name)
			if (!DEBUG) {
				EntityData ed = f.e.addEntityToParent(subjectKey, dataSetFolder.id, retiredSample.id, dataSetFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
				dataSetFolder.entityData.add(ed)
			}
			
			if (PRINTS) System.out.println("  Removing from retired data folder")
			retiredDataFolder.entityData.remove(retiredSampleEd)
			if (!DEBUG) f.e.deleteEntityData(subjectKey, retiredSampleEd.id)
			
			if (PRINTS) System.out.println("  Renaming retired sample to "+realName)
			retiredSample.setName(realName)
			if (!DEBUG) retiredSample = f.e.saveOrUpdateEntity(subjectKey, retiredSample)
			
			if (PRINTS) System.out.println("  Renaming LSMs to include bz2 extension "+realName)
			Entity supportingData = EntityUtils.findChildWithName(retiredSample, "Supporting Files")
			if (supportingData==null) {
				System.out.println("  WARNING: could not find supporting files")
				continue
			}

			f.loadChildren(supportingData)
			for(Entity tile : EntityUtils.getChildrenOfType(supportingData, TYPE_IMAGE_TILE)) {
				f.loadChildren(tile)
				for(Entity lsm : EntityUtils.getChildrenOfType(tile, TYPE_LSM_STACK)) {
					String newName = lsm.name+".bz2"
					if (PRINTS) System.out.println("    Renaming "+lsm.name+" -> "+newName)
					lsm.setName(newName)
					if (!DEBUG) {
						lsm = f.e.saveOrUpdateEntity(subjectKey, lsm)
					}
				}
			}
			
			numSamplesProcessed++
			foundMatch = true
			
			//System.exit(1)
		}
		else {
			System.out.println("WARNING: retired sample is too old: "+retiredSample.name +" ("+realSample.creationDate+" < "+cutoff+")")
		}
		
		realSample.setEntityData(null)
	}
		
	if (!foundMatch) {
		System.out.println("  Considering this a truly retired sample because the sub-samples don't match the new sample")
		numTrulyRetired++
	}
	
	retiredSample.setEntityData(null)
}

println "Rescued "+numSamplesProcessed+" samples"
println "Consider "+numTrulyRetired+" as truly retired samples"

