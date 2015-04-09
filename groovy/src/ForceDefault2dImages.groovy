import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask
import org.janelia.it.jacs.model.user_data.Node
import org.janelia.it.jacs.shared.utils.EntityUtils

class ForceDefault2dImagesScript {
	
	private static final boolean DEBUG = false;
	private String pipelinePrefix = "YoshiMacro";
	private final JacsUtils f; 
	private String[] dataSetIdentifiers = [ "asoy_mb_polarity_case_1", "asoy_mb_polarity_case_2", "asoy_mb_polarity_case_3", "asoy_mb_polarity_case_4", "asoy_mb_split_mcfo_case_1" ]
	private int numSamples = 0
	private int numDefaultImagesSet = 0
	
	public ForceDefault2dImagesScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample") && !entity.name.endsWith("-Retired")) {
					processSample(entity)
				}
				entity.setEntityData(null)
			}
		}
        println "Considered "+numSamples+" samples. Forced "+numDefaultImagesSet+" 2d images."
		println "Done"
	}
	
	void processSample(Entity sample) {
		String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)
		f.loadChildren(sample)
		
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			Entity forced2dImage = null;
			childSamples.each {
				f.loadChildren(it)
				Entity image = processSubSample(it)
				if (image) {
					forced2dImage = image;
				} 
			}
			if (forced2dImage!=null) {
				println "  Forcing "+sample.name+" with "+forced2dImage
				setDefault2dImage(sample, forced2dImage)
			}
		}
		else {
			processSubSample(sample)
		}
	}
	
	Entity processSubSample(Entity sample) throws Exception {
		
		println "Checking sample "+sample.getName()
		numSamples++
		
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
		if (runs.isEmpty()) return;

		// Pick the 2d image to use. Either the one that matches the prefix given, or just the latest
		Entity latest2dImage = null;
		Entity forced2dImage = null;
		for(Entity pipelineRun : runs) {
			f.loadChildren(pipelineRun)
			EntityData imageEd = pipelineRun.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
			if (imageEd==null) continue;
			String process = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
			if (process.startsWith(pipelinePrefix)) {
				forced2dImage = imageEd.getChildEntity();
				//println "  Forcing "+pipelineRun.name+" (image.id="+forced2dImage.id+")"
			}
			latest2dImage = imageEd.getChildEntity();
		}
		if (forced2dImage) {
			latest2dImage = forced2dImage;
		}
		setDefault2dImage(sample, latest2dImage)
		return forced2dImage
	}
	
	// ----------------------------------------------------------------------
	// Below methods stolen from EntityHelper in compute module
	// ----------------------------------------------------------------------
	
	public void setDefault2dImage(Entity entity, Entity default2dImage) throws ComputeException {
		setImageIfNecessary(entity, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, default2dImage);
	}
	
	public void setImageIfNecessary(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null || entity==null) return;
		EntityData currImageEd = entity.getEntityDataByAttributeName(attributeName);
		if (currImageEd==null || currImageEd.getChildEntity()==null || !currImageEd.getChildEntity().getId().equals(image.getId())) {
			setImage(entity, attributeName, image);
		}
	}
	
	public void setImage(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null || entity==null) return;
		removeEntityDataForAttributeName(entity, attributeName);
		addImage(entity, attributeName, image);
		numDefaultImagesSet++;
	}
	
	public void removeEntityDataForAttributeName(Entity entity, String attributeName) throws ComputeException {
        if (DEBUG) {
			println "  Removing "+attributeName+" for "+entity.getName()+" (id="+entity.getId()+")";
			return;
        }
		
        Set<EntityData> toDelete = new HashSet<EntityData>();
        for(EntityData ed : entity.getEntityData()) {
        	if (ed.getEntityAttrName().equals(attributeName)) {
        		toDelete.add(ed);
        	}
        }
        
        for (EntityData ed : toDelete) {
            f.e.deleteEntityData(ed.ownerKey, ed.id);
			entity.getEntityData().remove(ed);
        }
    }
	
	/**
	 * Adds the given image as an image property to the given entity.
	 * @param entity
	 * @param image
	 * @param attributeName
	 * @throws ComputeException
	 */
	public void addImage(Entity entity, String attributeName, Entity image) throws ComputeException {
		if (image==null) return;
		if (DEBUG) {
			println "  Adding "+attributeName+" ("+image.getName()+") to "+entity.getName()+" (id="+entity.getId()+")";
			return;
		}
		String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		EntityData ed = f.e.addEntityToParent(entity.ownerKey, entity.id, image.id, null, attributeName);
		f.e.setOrUpdateValue(entity.ownerKey, entity.id, attributeName, filepath);		
	}
}

ForceDefault2dImagesScript script = new ForceDefault2dImagesScript();
script.run();
System.exit(0);
