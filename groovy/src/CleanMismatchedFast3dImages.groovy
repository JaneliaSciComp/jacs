import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanMismatchedFast3dImagesScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private int numUpdated;
	
	public CleanMismatchedFast3dImagesScript() {
		f = new JacsUtils(null, !DEBUG)
	}
	
	public void run() {
		
		println "Finding problem cases.."
		
		List<Entity> entities = f.e.getEntitiesByNameAndTypeName(null, "AlignedFlyVNC.v3dpbd", EntityConstants.TYPE_IMAGE_3D)
		
		println "Checking "+entities.size()+" AlignedFlyVNC.v3dpbd entities"
		
		for(Entity entity : entities) {
			processImage(entity);
            //if (numUpdated>0) break;
			entity.setEntityData(null)
		}
        println "Completed processing"
        println "  Fixed "+numUpdated+" images"
	}
	
	public void processImage(Entity vncImage) {
		f.loadChildren(vncImage)
		
		Entity supportingData = f.e.getAncestorWithType(vncImage.ownerKey, vncImage.id, EntityConstants.TYPE_SUPPORTING_DATA)
		f.loadChildren(supportingData)
		
		Entity alignment = f.e.getAncestorWithType(supportingData.ownerKey, supportingData.id, EntityConstants.TYPE_ALIGNMENT_RESULT)
		f.loadChildren(alignment)
		
		// We've identified a problem case
		println "Fixing alignment "+alignment.id+" ("+alignment.ownerKey+")"
		
		Entity brainImage = EntityUtils.findChildWithName(supportingData, "AlignedFlyBrainIntRescaled.v3dpbd")
		Entity defaultImage = alignment.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
		
		Entity fast3dBrain = findFast3dImage(alignment, brainImage)
		Entity fast3dVNC = findFast3dImage(alignment, vncImage)
		
		fixFast3dImage(brainImage, fast3dBrain)
		fixFast3dImage(vncImage, fast3dVNC)
		
        numUpdated++;
	}
	
	private void fixFast3dImage(Entity image, Entity fast3dImage) {
		
		if (image==null) return
		if (fast3dImage==null) {
			println "  No fast 3d image for "+image.id+" : "+image.name
			return
		}
		
		println "  Fixing image "+image.id+" : "+image.name
		
		EntityData movieEd = image.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)
		if (movieEd!=null) {
			if (!DEBUG) f.e.deleteEntityData(movieEd.ownerKey, movieEd.id);
		}
		
		if (!DEBUG) {
			String filepath = fast3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
			EntityData ed = f.e.addEntityToParent(image.ownerKey, image.id, fast3dImage.id, null, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)
			ed.setValue(filepath)
			f.e.saveOrUpdateEntityData(ed.ownerKey, ed)
		}
	}
	
	private Entity findFast3dImage(Entity alignment, Entity image) {
		// Find the brain movie and assign it to the Brain image
		Entity brainSep = findSeparation(alignment, image)
		if (brainSep!=null) {
			return findFast3dImage(brainSep)
		}
		return null
	}

	private Entity findSeparation(Entity resultEntity, Entity default3dImage) {
		Entity foundEntity = null;
		for(Entity separation : EntityUtils.getChildrenOfType(resultEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
			f.loadChildren(separation)
			// We'll take the latest if we don't find one matching our input image, because not all entity types keep track of the Input Image attribute
			foundEntity = separation;
			EntityData inputImageEd = foundEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_INPUT_IMAGE);
			if (inputImageEd!=null && inputImageEd.getChildEntity()!=null && inputImageEd.getChildEntity().getId().equals(default3dImage.getId())) {
				// Found it, so let's stop looking
				break;
			}
		}
		return foundEntity;
	}

	private Entity findFast3dImage(Entity separation) throws Exception {

		Entity supportingFiles = EntityUtils.getSupportingData(separation);
		if (supportingFiles==null) return null;
		f.loadChildren(supportingFiles)

		Entity signalVolume = EntityUtils.findChildWithName(supportingFiles, "ConsolidatedSignal.v3dpbd");
		if (signalVolume!=null) {
			f.loadChildren(signalVolume)
			Entity fast3dImage = signalVolume.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE);
			if (fast3dImage!=null) {
				return fast3dImage;
			}
		}

		return null
	}
}

CleanMismatchedFast3dImagesScript script = new CleanMismatchedFast3dImagesScript();
script.run();
println "Done"
System.exit(0);