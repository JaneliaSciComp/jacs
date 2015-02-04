package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Gets all the aligned images for an alignment and puts their ids in a list called IMAGE_ID.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetAlignedImagesService extends AbstractEntityService {

    public void execute() throws Exception {
        	
    	Long resultEntityId = data.getRequiredItemAsLong("RESULT_ENTITY_ID");
    	Entity resultEntity = entityBean.getEntityById(resultEntityId);
    	populateChildren(resultEntity);
    	
    	Entity supportingFiles = EntityUtils.getSupportingData(resultEntity);
    	populateChildren(supportingFiles);
    	
    	List<String> imageIds = new ArrayList<String>();    	
    	for(Entity image : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_3D)) {
    	    if (image.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)!=null) {
    	        imageIds.add(image.getId().toString());
    	    }
    	}
    	
    	if (imageIds.isEmpty()) {
    	    logger.info("No images with an objective for alignment "+resultEntityId+". Using the default 3d image.");
    	    Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
    	    imageIds.add(default3dImage.getId().toString());
    	}
    	
    	logger.info("Putting "+imageIds.size()+" image ids in IMAGE_ID");
    	processData.putItem("IMAGE_ID", imageIds);
    }
}
