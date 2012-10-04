package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts metadata from the entity model to be used for the neuron separator. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSeparationParametersService extends AbstractEntityService {
    
    public void execute() throws Exception {

    	String resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
    	if (resultEntityName == null || "".equals(resultEntityName)) {
    		throw new IllegalArgumentException("RESULT_ENTITY_NAME may not be null");
    	}

    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null || "".equals(sampleEntityId)) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

    	String rootEntityId = (String)processData.getItem("ROOT_ENTITY_ID");
    	if (rootEntityId == null || "".equals(rootEntityId)) {
    		throw new IllegalArgumentException("ROOT_ENTITY_ID may not be null");
    	}

    	Entity rootEntity = entityBean.getEntityById(rootEntityId);
    	if (rootEntity == null) {
    		throw new IllegalArgumentException("Root entity not found with id="+sampleEntityId);
    	}
    	
    	populateChildren(sampleEntity);
    	List<Entity> children = sampleEntity.getOrderedChildren();
    	Collections.reverse(children);
    	
    	Entity prevRun = null;
    	for(Entity child : children) {
    		if (EntityConstants.TYPE_PIPELINE_RUN.equals(child.getEntityType().getName()) 
    				&& child.getName().equals(resultEntityName)) {
    			prevRun = child;
    			break;
    		}
    	}
    	
		if (prevRun != null) {
			
			Entity prevResult = null;
			for(Entity result : prevRun.getChildren()) {
				if (result.getName().equals(rootEntity.getName())) {
					prevResult = result;
				}
			}
			
			if (prevResult != null) {
    			
    			Entity prevResultFile = null;
    			for(Entity file : EntityUtils.getSupportingData(prevResult).getChildren()) {
    				if (file.getName().endsWith(".nsp")) {
    					prevResultFile = file;
    					break;
    				}
    			}
    			
    			if (prevResultFile!=null) {
    				String filepath = EntityUtils.getFilePath(prevResultFile);
    				if (filepath!=null && !"".equals(filepath)) {
    	    			logger.info("Putting "+prevRun.getId()+" in PREVIOUS_RESULT_ID");
    	    			processData.putItem("PREVIOUS_RESULT_ID", prevResult.getId());
    					logger.info("Putting "+filepath+" in PREVIOUS_RESULT_ID");
    					processData.putItem("PREVIOUS_RESULT_FILENAME", filepath);
    				}
    			}
			}
		}
    }
}
