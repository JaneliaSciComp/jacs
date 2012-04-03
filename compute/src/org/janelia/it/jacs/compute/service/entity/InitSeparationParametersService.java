package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Extracts metadata from the entity model to be used for the neuron separator. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InitSeparationParametersService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();

        	String resultEntityName = (String)processData.getItem("RESULT_ENTITY_NAME");
        	if (resultEntityName == null || "".equals(resultEntityName)) {
        		throw new IllegalArgumentException("RESULT_ENTITY_NAME may not be null");
        	}
        	
        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null || "".equals(sampleEntityId)) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}

        	List<Entity> children = sampleEntity.getOrderedChildren();
        	Collections.reverse(children);
        	
        	Entity prevResult = null;
        	for(Entity child : children) {
        		if (EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT.equals(child.getEntityType().getName()) 
        				&& child.getName().equals(resultEntityName)) {
        			prevResult = child;
        			break;
        		}
        	}
        	
    		if (prevResult != null) {
    			logger.info("Putting "+prevResult.getId()+" in PREVIOUS_RESULT_ID");
    			processData.putItem("PREVIOUS_RESULT_ID", prevResult.getId());
    			Entity supportingData = EntityUtils.getSupportingData(prevResult);
    			Entity prevResultFile = EntityUtils.findChildWithName(supportingData, "SeparationResult.nsp");
    			if (prevResultFile!=null) {
    				String filepath = EntityUtils.getFilePath(prevResultFile);
    				if (filepath!=null && !"".equals(filepath)) {
    					logger.info("Putting "+filepath+" in PREVIOUS_RESULT_ID");
    					processData.putItem("PREVIOUS_RESULT_FILENAME", filepath);
    				}
    			}
    		}
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
}
