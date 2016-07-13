package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanVNCSamplesScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private int numUpdated;
	
	public CleanVNCSamplesScript() {
		f = new JacsUtils(null, !DEBUG)
	}
	
	public void run() {
				
		for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED)) {
			if (entity.entityTypeName.equals("Sample")) {
				processSample(entity);
                //if (numUpdated>0) break;
			}
			entity.setEntityData(null)	
		}
        println "Completed processing"
        println "  Fixed "+numUpdated+" samples"
	}
	
	public void processSample(Entity sample) {
		f.loadChildren(sample)
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
        if (runs.isEmpty()) return;
        Collections.reverse(runs)
        
        //println "Checking "+sample.name+" "
        
        Entity sf = EntityUtils.getSupportingData(sample)
        f.loadChildren(sf)
        List<Entity> tiles = EntityUtils.getChildrenOfType(sf, "Image Tile")
        
        if (tiles.size()!=1) return;
        
        Entity lastRun = runs.get(0)
        f.loadChildren(lastRun)
        
        Entity error = EntityUtils.findChildWithType(sf, "Error")
        
        Entity tile = tiles.get(0)
        
        EntityData aaEd = tile.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
        
        if (!aaEd.value.equals("Ventral Nerve Cord")) return;
        
        Entity spr = EntityUtils.findChildWithType(lastRun, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
        
        if (!spr.name.equals("Sample Processing Results (Ventral Nerve Cord)")) return;
        
        //println "  Setting tile AA=VNC"
        if (!DEBUG) {
            f.e.setOrUpdateValue(tile.ownerKey, tile.id, EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, "VNC")
        }
        
        //println "  Setting SPR AA=VNC, and fixing name"
        if (!DEBUG) {
            spr.name = spr.name.replaceFirst("Ventral Nerve Cord", "VNC")
            f.e.saveOrUpdateEntity(spr.ownerKey, spr)
            f.e.setOrUpdateValue(tile.ownerKey, spr.id, EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, "VNC")
        }
        
        String status = null
        if (error!=null) {
            //println "  Setting status=Error"
            status = EntityConstants.VALUE_ERROR
        }
        else {
            //println "  Setting status=Complete"
            status = EntityConstants.VALUE_COMPLETE
        }
        if (!DEBUG) {
            f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, status)
        }
        
        String dataset = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
        println "Fixed "+sample.name+" ("+dataset+")"
        
        
        numUpdated++;
	}
}

CleanVNCSamplesScript script = new CleanVNCSamplesScript();
script.run();
println "Done"
System.exit(0);