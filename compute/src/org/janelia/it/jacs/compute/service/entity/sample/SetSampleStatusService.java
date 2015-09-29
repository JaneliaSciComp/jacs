package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.Date;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Sets the Status attribute of the Sample and updates the Completion Date if necessary. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetSampleStatusService extends AbstractEntityService {

    private SampleHelper sampleHelper;
    private boolean firstCompletion = false;
    
    public void execute() throws Exception {
        
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        
        String status = data.getRequiredItemAsString("STATUS");
    	Long entityId = data.getRequiredItemAsLong("ENTITY_ID");
    	Entity sample = entityBean.getEntityById(entityId);
    	Entity parentSample = entityBean.getAncestorWithType(sample, EntityConstants.TYPE_SAMPLE);

    	// We care only about the first completion of the child samples. Parent completion dates can be overridden if the child is newly completed.
        this.firstCompletion = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE)==null;
        
    	if (parentSample==null) {
    	    contextLogger.info("Setting status to "+status+" on parent-less sample "+sample.getName()+" (id="+entityId+")");
            setStatus(sample, status, true);
    	}
    	else {
    	    contextLogger.info("Setting status to "+status+" on sample "+sample.getName()+" (id="+entityId+")");
            setStatus(sample, status, true);
            
            // We update the parent sample status with any status except Complete, unless all child samples are completed
            if (!status.equals(EntityConstants.VALUE_COMPLETE) || allChildSamplesComplete(parentSample)) {
                contextLogger.info("Setting status to "+status+" on parent sample "+parentSample.getName()+" (id="+parentSample.getId()+")");
                setStatus(parentSample, status, false);
            }       
    	}
    }
    
    private void setStatus(Entity sample, String status, boolean setLsmAttributes) throws Exception {
        
        // Set sample status
        entityBean.setOrUpdateValue(sample.getId(), EntityConstants.ATTRIBUTE_STATUS, status);
        
        if (status.equals(EntityConstants.VALUE_COMPLETE) && firstCompletion) {
                    
            String completionDate = sampleHelper.format(new Date());
            contextLogger.info("Setting completion date on sample "+sample.getName()+" (id="+sample.getId()+")");
                        
            // Set completion date for sample
            entityBean.setOrUpdateValue(sample.getId(), EntityConstants.ATTRIBUTE_COMPLETION_DATE, completionDate);
            
            if (setLsmAttributes) {
                // Set completion date for LSMs
                entityLoader.populateChildren(sample);
                Entity supportingFolder = EntityUtils.getSupportingData(sample);
                entityLoader.populateChildren(supportingFolder);
                for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFolder, EntityConstants.TYPE_IMAGE_TILE)) {
                    entityLoader.populateChildren(imageTile);
                    for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                        entityBean.setOrUpdateValue(lsmStack.getId(), EntityConstants.ATTRIBUTE_COMPLETION_DATE, completionDate);
                    }
                }
            }
            
        }
    }
    
    private boolean allChildSamplesComplete(Entity parentSample) throws Exception {
        entityLoader.populateChildren(parentSample);
        for(Entity childSample : EntityUtils.getChildrenOfType(parentSample, EntityConstants.TYPE_SAMPLE)) {
            String childStatus = childSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
            if (childStatus!=null && !EntityConstants.VALUE_COMPLETE.equals(childStatus)) {
                // All child samples are not finished, so the parent cannot be finished
                return false;
            }
        }
        return true;
    }
}
