package org.janelia.it.jacs.compute.service.entity;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {

    private SampleHelper sampleHelper;
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

        addCompletionDates(ownerKey);
    }
    
    private void addCompletionDates(String subjectKey) throws Exception {
        for(Entity sample : entityBean.getUserEntitiesByTypeName(subjectKey, EntityConstants.TYPE_SAMPLE)) {
            if (sample.getName().contains("~")) continue;
            addCompletionDates(sample);
            // Free memory
            sample.setEntityData(null);
        }
    }
    
    private Date addCompletionDates(Entity sample) throws Exception {

        logger.info("Processing "+sample.getName()+" (id="+sample.getId()+")");
        
        entityLoader.populateChildren(sample);
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample");
        if (!childSamples.isEmpty()) {
            Date latestDate = null;
            for(Entity childSample : childSamples) {
                Date subSampleDate = addCompletionDates(childSample);
                if (latestDate==null||latestDate.before(subSampleDate)) {
                    latestDate = subSampleDate;
                }
            }
            if (latestDate!=null) {
                setCompletionDate(sample, latestDate, false);
                return latestDate;
            }
            return null;
        }
        
        Entity pipelineRun = getLatestSuccessfulRun(sample);
        if (pipelineRun!=null) {
            setCompletionDate(sample, pipelineRun.getCreationDate(), true);
            return pipelineRun.getCreationDate();
        }
        return null;
    }
    
    private Entity getLatestSuccessfulRun(Entity sample) throws Exception {

        List<Entity> pipelineRuns = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
        Collections.reverse(pipelineRuns);
        for(Entity pipelineRun : pipelineRuns) {
            entityLoader.populateChildren(pipelineRun);
            if (EntityUtils.findChildWithType(pipelineRun, EntityConstants.TYPE_ERROR) != null) {
                continue;
            }
            return pipelineRun;
        }
        
        return null;
    }
    
    private void setCompletionDate(Entity sample, Date date, boolean setLsmAttributes) throws Exception {

        String completionDate = sampleHelper.format(date);
        logger.info("Setting completion "+completionDate+" on "+sample.getName()+" (id="+sample.getId()+")");
        
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
