package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

class CleanCompletionDatesScript {
	
	private static final boolean DEBUG = false;
    private String ownerKey = "user:nerna";
	private Long folderId = 2047918390982475874;
    private final JacsUtils f;
    
	public CleanCompletionDatesScript() {
		f = new JacsUtils(ownerKey, false)
	}
	
	public void run() {
		Entity folder = f.e.getEntityById(ownerKey, folderId);
		f.loadChildren(folder)
		
		int c = 0
		for(Entity sample : folder.getChildren()) {
			processSample(sample);
			c++;
			sample.setEntityData(null)
		}
		
		print "Processed "+c+" samples"
		
        println "Done"
        System.exit(0)
	}
    
    private void processSample(Entity parentSample) {
		if (parentSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE)==null) {
			
			String completionDate = null;
			String compressionType = null;
			for(Entity dup : f.e.getEntitiesByName(ownerKey, parentSample.name)) {
				if (dup.id.equals(parentSample.id)) continue;
				completionDate = dup.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE)
				compressionType = dup.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMRESSION_TYPE)
				if (completionDate!=null) break; 
			}
			
			if (completionDate!=null) {
				println "Applying surrogate completion date for sample: "+parentSample.name+" ("+parentSample.id+")"
				f.e.setOrUpdateValue(ownerKey, parentSample.getId(), EntityConstants.ATTRIBUTE_COMPLETION_DATE, completionDate);
				f.e.setOrUpdateValue(ownerKey, parentSample.getId(), EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_COMPLETE);
				if (compressionType!=null) {
					f.e.setOrUpdateValue(ownerKey, parentSample.getId(), EntityConstants.ATTRIBUTE_COMRESSION_TYPE, compressionType);
				}
			}	
			else {
				println "Sample has no completion date: "+parentSample.name+" ("+parentSample.id+")"
			}
		}
		else {
			println "Sample already has completion date: "+parentSample.name+" ("+parentSample.id+")"
		}
    }

}

CleanCompletionDatesScript script = new CleanCompletionDatesScript();
script.run();