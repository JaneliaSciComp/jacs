import javax.ejb.EntityContext;

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanDuplicateTilesScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "ditp_initial_splits", "ditp_mcfo_case_1", "ditp_polarity_case_3" ]
    private int numUpdated;
	private int totalNumUpdated;
	
	public CleanDuplicateTilesScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			numUpdated = 0
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample")) {
					processSample(entity);
				}
				entity.setEntityData(null)	
			}
			println "  Updated "+numUpdated+" samples for "+dataSetIdentifier
            totalNumUpdated += numUpdated
		}
        println "  Updated "+totalNumUpdated+" samples"
		println "Done"
	}
	
	public void processSample(Entity sample) {
		f.loadChildren(sample)
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				processSample(it)
			}
		}
		Entity sd = EntityUtils.getSupportingData(sample)
        f.loadChildren(sd)
        
        Map<String,EntityData> tiles = new HashMap<String,EntityData>()
        
		for(EntityData tileEd : sd.getOrderedEntityData()) {
            
            Entity tile = tileEd.getChildEntity()
            String tileName = tile.getName()
            
            EntityData prevEd = tiles.get(tileName)
            if (prevEd!=null) {
                Entity prev = prevEd.getChildEntity()
                String aa = prev.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
                String newAA = tile.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
                println "Removing duplicate tile: "+tileName+" (had AA "+aa+", replacing with "+newAA+") for "+sample.name
                numUpdated++;
                if (!DEBUG) {
                    f.e.deleteEntityData(prevEd.ownerKey, prevEd.id)
                    f.e.deleteEntityTreeById(tile.ownerKey, prev.id, true)
                }
            }

            tiles.put(tileName, tileEd)
		}
	}
}

CleanDuplicateTilesScript script = new CleanDuplicateTilesScript();
script.run();