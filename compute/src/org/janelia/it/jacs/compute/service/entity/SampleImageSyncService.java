package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Synchronizes the Samples with their latest results.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleImageSyncService implements IService {
	
    protected Logger logger;
    protected Task task;
    protected String ownerKey;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            ownerKey = task.getOwner();

            List<Entity> samples = entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE);
            
            for(Entity s : samples) {
                Entity sample = entityBean.getEntityTree(s.getId());
                if (!ownerKey.equals(sample.getOwnerKey())) continue;
                
                String sampleFilepath = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
                
                Entity result = EntityUtils.getLatestChildOfType(sample, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
                String resultFilepath = result==null ? null : result.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
                
                if (resultFilepath==null || "".equals(resultFilepath)) {
                	EntityData ed = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
                	if (ed != null) {
    	                logger.info("Removing default 2d image from Sample (id="+sample.getId()+") which has no results");
                		entityBean.deleteEntityData(ed);
                	}
                }
                else if (!resultFilepath.equals(sampleFilepath)) {
	                logger.info("Updating Sample (id="+sample.getId()+") with filepath from latest Separation Result (id="+result.getId()+")");
	                sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, resultFilepath);
	                sample = entityBean.saveOrUpdateEntity(sample);
                }
                else {
                	logger.info("Sample (id="+sample.getId()+") is already up-to-date");
                }
            }
            
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running SampleImageSyncService:" + e.getMessage(), e);
        }
    }
    
}
