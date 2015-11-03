import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanUnreusedPostProcessingResultsScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "senr_silencing_hits_mcfo_case_1" ]
	private int numUpdated;
	
	public CleanUnreusedPostProcessingResultsScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			numUpdated = 0
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample")) {
					processSample(entity);
//                    if (numUpdated>0) break;
				}
				entity.setEntityData(null)	
			}
			println "  Updated "+numUpdated+" pipeline runs for "+dataSetIdentifier
		}
		println "Done"
	}
	
	public void processSample(Entity sample) {
		f.loadChildren(sample)
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				processSample(it)
			}
			return
		}
        
        Entity prev = null;
        Entity last = null;
        
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
        Collections.reverse(runs);
        
        
		for(Entity run : runs) {
            if ("Paired Sample Pipeline 63x Results".equals(run.name)) {
                if (last==null) {
                    last = run;
                }
                else if (prev==null) {
                    prev = run;
                    break;
                }
            }
		}
        
        if (prev!=null && last!=null) {
            
            Entity prevPost = findPost(prev)
            Entity lastPost = findPost(last)
            
            if (prevPost!=null && lastPost==null) {
                
                println ""+sample.name
//                println "  "+prev.getCreationDate()+" "+prevPost
//                println "  "+last.getCreationDate()+" "+lastPost
                
                int i = 1
                Integer firstAlignmentIndex = null
                
                for(EntityData ed : last.getOrderedEntityData()) {
                    
                    if (!ed.entityAttrName.equals("Result")) continue
                    
                    Entity child = ed.childEntity
                    if (child.entityTypeName.equals("Alignment Result")) {
                        if (firstAlignmentIndex==null) {
                            firstAlignmentIndex = i
                        }
                        
                        println "  Updating "+child.name+" to index "+(i+1)
                        if (!DEBUG) {
                            // Why is parent entity null here?
                            ed.parentEntity = last
                            f.e.updateChildIndex(last.ownerKey, ed, i+1)
                        }
                    }
                    else {
                        println "  Child "+child.name+" has index "+ed.orderIndex
                    }
                    
                    i++;
                }
                
                if (firstAlignmentIndex==null) {
                    firstAlignmentIndex = i
                }
                
                println "  Adding "+prevPost.name+" at "+firstAlignmentIndex
                if (!DEBUG) {
                    f.e.addEntityToParent(last.ownerKey, last.id, prevPost.id, firstAlignmentIndex, "Result")
                }
                
                numUpdated++;
            }
            
        }
	}
    
    private Entity findPost(Entity run) {
        f.loadChildren(run)
        List<Entity> posts = EntityUtils.getChildrenOfType(run, "Sample Post Processing Result")
        if (posts.isEmpty()) return null
        return posts.get(posts.size()-1)
    }
}

CleanUnreusedPostProcessingResultsScript script = new CleanUnreusedPostProcessingResultsScript();
script.run();