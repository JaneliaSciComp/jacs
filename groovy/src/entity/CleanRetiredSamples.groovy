package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils


class CleanRetiredSamplesScript {
	
	private boolean DEBUG = false
	private boolean IGNORE_NO_ACTIVE = false
	private boolean FAST_AND_LOOSE = false
	private String ownerKey = "group:flylight"
    private JacsUtils f
	private int numProcessed = 0
	private int numMigrated = 0
	
	public void run() {
		f = new JacsUtils(ownerKey, false)
		println "Inspecting samples for "+ownerKey
		if (DEBUG) {
			println "Running in DEBUG mode, changes will not be saved"
		}
		
		List<Long> toDelete = []
		for(Entity folder : f.e.getUserEntitiesByNameAndTypeName(ownerKey, "Retired Data", "Folder")) {
			f.loadChildren(folder)
			for(Entity retiredSample : EntityUtils.getChildrenOfType(folder, "Sample")) {
		
				println ""
				
				Entity activeSample = getActiveSample(retiredSample)
				if (activeSample==null) {
					println ""+retiredSample.name+" ("+retiredSample.id+")"
				}
				else {
					println ""+retiredSample.name+" ("+retiredSample.id+" -> "+activeSample.id+")"
				}
				
				if (IGNORE_NO_ACTIVE && activeSample==null) {
					println "  Ignoring retired sample with no active counterpart"
					continue
				} 
				
				int count = f.a.getNumDescendantsAnnotated(retiredSample.id)
				
				List<Entity> annotations = new ArrayList<>(f.a.getAnnotationsForEntity(null, retiredSample.id))
				f.loadChildren(retiredSample)
				for(Entity subSample : EntityUtils.getChildrenOfType(retiredSample, TYPE_SAMPLE)) {
					annotations.addAll(f.a.getAnnotationsForEntity(null, subSample.id))
					f.loadChildren(subSample)
					for(Entity run : EntityUtils.getChildrenOfType(subSample, TYPE_PIPELINE_RUN)) {
						for(Entity annotation : f.a.getAnnotationsForEntity(null, run.id)) {
							if (!annotation.ownerKey.equals("group:workstation_users")) {
								annotations.add(annotation) 
							}
							else {
								println "  Skipping annotation: "+annotation.name
								count--
							}
						}
					}
				}
				for(Entity run : EntityUtils.getChildrenOfType(retiredSample, TYPE_PIPELINE_RUN)) {
					for(Entity annotation : f.a.getAnnotationsForEntity(null, run.id)) {
						if (!annotation.ownerKey.equals("group:workstation_users")) {
							annotations.add(annotation) 
						}
						else {
							println "  Skipping annotation: "+annotation.name
							count--
						}
					}
				}
				
				println "  Found "+annotations.size()+" sample annotations"
				
				boolean migrate = false;
				
				if (count==0) {
					println "  No annotations"
					migrate = true
				}
				else if (count<=annotations.size()) {
					if (activeSample!=null) {
						println "  Migrating "+annotations.size()+" sample annotations"
						if (!DEBUG) {
							for(Entity annotation : annotations) {
								annotation.setValueByAttributeName(ATTRIBUTE_ANNOTATION_TARGET_ID, activeSample.id+"")
								f.e.saveOrUpdateEntity(annotation.ownerKey, annotation)
							}
						}
						migrate = true
					}
					else if (count<2 && FAST_AND_LOOSE) {
						println "  !!! No active sample, discarding "+count+" annotations"
						migrate = true
					}
					else {
						println "  No active sample, cannot migrate "+annotations.size()+" sample annotations"
					}
				}
				else if (count<2 && FAST_AND_LOOSE) {
					println "  !!! Discarding "+count+" annotations"
					migrate = true
				}
				else {
					println "  Annotations cannot be migrated: "+count+" (on sample: "+annotations.size()+")"
				}
				
				if (migrate) {
		
					if (activeSample!=null) {
						for(EntityData ed : f.e.getParentEntityDatas(null, retiredSample.id)) {
							if (ed.parentEntity.id.equals(folder.id)) {
								println "  Leaving Retired Data folder reference: "+ed.id
							}
							else {
								println "  Migrating reference: "+ed.parentEntity.name;
								if (!DEBUG) {
									ed.childEntity = activeSample
									f.e.saveOrUpdateEntityData(ed.ownerKey, ed)
								}
							}
						}
					}
					
					println "  Will delete retired sample"
					toDelete.add(retiredSample.id)

					numMigrated++
				}
		
				retiredSample.setEntityData(null)
				numProcessed++
			}
		}
	
		if (!DEBUG) {
			for(Long retiredSampleId : toDelete) {
				println "Deleting "+retiredSampleId
				try {
					f.e.deleteEntityTreeById(ownerKey, retiredSampleId)
				}
				catch (Throwable t) {
					println "Error deleting sample: "+retiredSampleId
					t.printStackTrace()
				}
				
			}
		}
	
		println ""
		println "Processed "+numProcessed+" samples. Migrated "+numMigrated+" samples."
		println "Done"
	}
	
	private Entity getActiveSample(Entity retiredSample) {
		
		String realName = retiredSample.name.replaceAll("-Retired","")
		List<Entity> realSamples = f.e.getEntitiesByNameAndTypeName(ownerKey, realName, TYPE_SAMPLE)
		if (realSamples.isEmpty()) {
			return null
		}
		List<Entity> active = []
		for (Entity sample : realSamples) {
			String status = sample.getValueByAttributeName(ATTRIBUTE_STATUS)
			if (!status.equals(VALUE_DESYNC) && !status.equals(VALUE_RETIRED)) {
				active.add(sample)
			}
		}
		if (active.isEmpty()) {
			return null
		}
		if (active.size()>1) {
			println "WARNING: more than one active sample with name "+realName
		}
		return active.get(0)
	}
	
	
	
}

CleanRetiredSamplesScript script = new CleanRetiredSamplesScript()
script.run()
System.exit(0)