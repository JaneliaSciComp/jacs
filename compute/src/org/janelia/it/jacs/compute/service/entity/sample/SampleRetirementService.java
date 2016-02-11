package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Retire all the samples which are in "Desync" status for the given user.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleRetirementService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(SampleRetirementService.class);
	private static final String RETIRED_SUFFIX = "-Retired";

    public transient static final String PARAM_testRun = "is test run";
    
	// Settings
	protected boolean isDebug = false;

	// Helpers
    protected SampleHelper sampleHelper;
    
    // Processing state
    protected Set<Long> visited = new HashSet<>();
    protected int numSamplesProcessed;
    protected int numSamplesRetired;
    protected Entity retiredDataFolder;
    protected Set<Long> retiredIds;
    
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
            isDebug = Boolean.parseBoolean(testRun);    
        }            
        
        if (isDebug) {
            logger.info("This is a test run. No samples will be retired.");
        }
        else {
            logger.info("This is the real thing. Samples will be retired!");
        }
        
        String dataSetName = data.getItemAsString("DATA_SET_NAME");
        Long maxSamples = data.getItemAsLong("MAX_SAMPLES");
        
        logger.info("Running sample retirement for "+ownerKey+" (dataSetName="+dataSetName+", maxSamples="+maxSamples+")");
        
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);

        String dataSetIdentifier = null;
        if (!StringUtils.isEmpty(dataSetName)) {
            for(Entity dataSet : entityBean.getEntitiesByNameAndTypeName(ownerKey, dataSetName, EntityConstants.TYPE_DATA_SET)) {
                dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                break;
            }
            sampleHelper.setDataSetNameFilter(dataSetName);
        }

        this.retiredDataFolder = sampleHelper.getRetiredDataFolder();
        this.retiredIds = new HashSet<Long>();
        Entity retiredDataFolder = sampleHelper.getRetiredDataFolder();
        for(EntityData ed : retiredDataFolder.getEntityData()) {
            if (ed.getChildEntity()!=null) {
                retiredIds.add(ed.getChildEntity().getId());
            }
        }
        
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (StringUtils.isEmpty(sampleEntityId)) {

            List<Entity> toRetire = new ArrayList<>();
			for(Entity sample : entityBean.getUserEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_DESYNC)) {
                if (dataSetIdentifier!=null && !dataSetIdentifier.equals(sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER))) {
                    continue;
                }
                if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                    logger.warn("Ignoring non-Sample entity with desync status: "+sample.getId());
                    continue;
                }
				toRetire.add(sample);
            }

			if (toRetire.isEmpty()) {
	            logger.info("No desynchronized samples found");
			    return;
			}
			
            if (maxSamples!=null && toRetire.size()>maxSamples) {
                logger.warn("Too many desynchronized samples for retirement processing ("+toRetire.size()+">"+maxSamples+")");
                return;
            }
            
            logger.info("Processing "+toRetire.size()+" desynced samples");

            int i = 1;
			for(Entity sample : toRetire) {
                logger.info("Processing sample "+i+" of "+toRetire.size()+": "+sample.getName()+" (id="+sample.getId()+")");
                try {
                    retireSample(sample);
                }
                catch (Exception e) {
                    logger.error("Error retiring sample: "+sample.getName(),e);
                }
                i++;
			}
    	}
    	else {
    		logger.info("Processing single sample: "+sampleEntityId);
    		Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
            retireSample(sampleEntity);
    	}

		logger.info("Processed "+numSamplesProcessed+" samples, retired "+numSamplesRetired+" samples.");
    }
    
    public void retireSample(Entity sample) throws Exception {
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		numSamplesProcessed++;

		String dataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        Entity dataSetFolder = sampleHelper.getDataSetFolderByIdentifierMap().get(dataSetIdentifier);
        
        EntityData ed = EntityUtils.findChildEntityDataWithChildId(dataSetFolder, sample.getId());
        if (ed==null) {
            logger.info("  Adding to '"+retiredDataFolder.getName()+"' folder");
            if (!isDebug) {
                retiredDataFolder.getEntityData().add(ed);
                entityBean.addEntityToParent(retiredDataFolder, sample, retiredDataFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
        }
        else {
            logger.info("  Moving from '"+dataSetFolder.getName()+"' to '"+retiredDataFolder.getName()+"'");
            if (!isDebug) {
                dataSetFolder.getEntityData().remove(ed);
                retiredDataFolder.getEntityData().add(ed);
                ed.setParentEntity(retiredDataFolder);
                entityBean.saveOrUpdateEntityData(ed);
            }
        }
        
        // Update sample status
        sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_RETIRED);
        logger.info("  Setting sample status to "+EntityConstants.VALUE_RETIRED);
        
        // Update the sample name if necessary
        if (!sample.getName().endsWith(RETIRED_SUFFIX)) {
            sample.setName(sample.getName()+RETIRED_SUFFIX);
            logger.info("  Renaming sample to "+sample.getName());
        }

        if (!isDebug) {
            entityBean.saveOrUpdateEntity(sample);
        }
        
        numSamplesRetired++;
    }
}
