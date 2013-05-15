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
    	
    	populateChildren(rootEntity);
    	Entity prevSeparation = EntityUtils.getLatestChildOfType(rootEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
    	if (prevSeparation != null) {
    	    logger.info("Found previous separation in the current result entity");
    	    putPrevResult(prevSeparation);
    	}
    	else {
    	    populateChildren(sampleEntity);
            List<Entity> runs = EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_PIPELINE_RUN);
            Collections.reverse(runs);
            
            boolean sepFound = false;
            
            for(Entity run : runs) {
                populateChildren(run);
                    
                Entity lastResult = null;
                boolean resultFound = false;
                for(Entity result : EntityUtils.getChildrenOfType(run, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                    lastResult = result;
                    if (result.getId().equals(rootEntityId)) {
                        resultFound = true;
                        break;
                    }
                }
                
                logger.info("Check pipeline run "+run.getId()+" containsCurrentResult?="+resultFound+" resultFound?="+(lastResult!=null));
                    
                if (!resultFound && lastResult!=null) {
                    populateChildren(lastResult);
                    List<Entity> separations = EntityUtils.getChildrenOfType(lastResult, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                    Collections.reverse(separations);
                    
                    for(Entity separation : separations) {
                        logger.info("Found previous separation in the previous pipeline run");
                        putPrevResult(separation);
                        sepFound = true;
                        break;    
                    }
                }
                
                if (sepFound) break;
            }
    	}
    }
    
    private void putPrevResult(Entity separation) throws Exception {

        logger.info("Getting previous result from separation with id="+separation.getId());
        
        populateChildren(separation);
        Entity supportingFiles = EntityUtils.getSupportingData(separation);
        populateChildren(supportingFiles);
        
        Entity prevResultFile = null;
        for(Entity file : supportingFiles.getChildren()) {
            if (file.getName().equals("SeparationResultUnmapped.nsp") || file.getName().equals("SeparationResult.nsp")) {
                prevResultFile = file;
                break;
            }
        }
        
        if (prevResultFile!=null) {
            String filepath = EntityUtils.getFilePath(prevResultFile);
            if (filepath!=null && !"".equals(filepath)) {
                logger.info("Putting "+filepath+" in PREVIOUS_RESULT_ID");
                processData.putItem("PREVIOUS_RESULT_FILENAME", filepath);
            }
        }
    }
}
