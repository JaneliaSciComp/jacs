package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class FixSampleStatusScript {
	
	private static final boolean DEBUG = true;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "nerna_optic_lobe_right" ]
	private int numMarked = 0
    private int numError = 0
    private int numComplete = 0
    
	public FixSampleStatusScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			numMarked = numError = numComplete = 0
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample") && !entity.name.endsWith("-Retired")) {
					processSample(entity, entity);
				}
				entity.setEntityData(null)	
			}
			println "  Marked: "+numMarked
            println "  Error: "+numError
            println "  Complete: "+numComplete
		}
		println "Done"
	}
	
	public boolean processSample(Entity parentSample, Entity sample) {
		
		String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
		
        if (status.equals(EntityConstants.VALUE_BLOCKED) || status.equals(EntityConstants.VALUE_RETIRED) ) return false
        
		f.loadChildren(sample)
        
        // TODO: process sub-samples
//		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
//		if (!childSamples.isEmpty()) {
//			childSamples.each {
//				if (processSample(sample, it)) {
//					problem = true
//				}
//			}
//			return problem
//		}
        
        println "Processing "+sample.name
        
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
        Collections.reverse(runs)
        
        boolean error = true
        boolean blocked = false
        Entity latestNonError = null
        
		for(Entity run : runs) {
			f.loadChildren(run)
			List<Entity> errors = EntityUtils.getChildrenOfType(run, "Error")
			if (errors.isEmpty()) {
				error = false;
                latestNonError = run
                break
			}
		}
        
        if (latestNonError!=null) {
            List<Entity> results = EntityUtils.getChildrenForAttribute(latestNonError, "Result")
            for(Entity result : results) {
                if (!result.entityTypeName.equals("LSM Summary Result")) {
                    f.loadChildren(result)
                    if (result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)==null) {
                        blocked = true;
                    }
                }
            }
        }
        
		String newStatus = null;
		if (blocked) {
			newStatus = EntityConstants.VALUE_MARKED
            numMarked++
		}
		else if (error) {
            newStatus = EntityConstants.VALUE_ERROR
            numError++
        }
        else {
            newStatus = EntityConstants.VALUE_COMPLETE
            numComplete++
        }
        
        println "  "+newStatus
        
        if (!DEBUG && newStatus!=null) {
            f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, newStatus)
        }
        
		return true
	}
}

FixSampleStatusScript script = new FixSampleStatusScript();
script.run();