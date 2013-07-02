package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.shared.utils.EntityUtils;


/**
 * Extracts sub-samples based on their objective.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetObjectiveSamplesService extends AbstractEntityService {
	
    public void execute() throws Exception {
        	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
        
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityType().getName())) {
    		throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
    	}
    	
    	logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");
    	populateChildren(sampleEntity);
        List<Entity> subsamples = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE);
        
        if (!subsamples.isEmpty()) {

            logger.info("Putting '"+sampleEntity.getId()+"' in PARENT_SAMPLE_ID");
            processData.putItem("PARENT_SAMPLE_ID", sampleEntity.getId().toString());
            
        	for(Entity subsample : subsamples) {
        	    String objective = subsample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        	    if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                    logger.info("Putting '"+subsample.getId()+"' in SAMPLE_20X_ID");
                    processData.putItem("SAMPLE_20X_ID", subsample.getId().toString());
        	    }
        	    else if (Objective.OBJECTIVE_40X.getName().equals(objective)) {
        	        logger.info("Putting '"+subsample.getId()+"' in SAMPLE_40X_ID");
        	        processData.putItem("SAMPLE_40X_ID", subsample.getId().toString());
                }
        	    else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
        	        logger.info("Putting '"+subsample.getId()+"' in SAMPLE_63X_ID");
        	        processData.putItem("SAMPLE_63X_ID", subsample.getId().toString());
                }
        	}
        }
        else {
            logger.info("No subsamples found");
            String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (objective!=null) {
                logger.info("Putting the parent sample '"+sampleEntity.getId()+"' in SAMPLE_20X_ID");
                processData.putItem("SAMPLE_"+objective.toUpperCase()+"_ID", sampleEntity.getId().toString());
            }
        }
    }
}
