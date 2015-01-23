package org.janelia.it.jacs.compute.service.entity.sample;

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
	
	// Settings
	protected boolean isDebug = false;

	// Helpers
    protected SampleHelper sampleHelper;
    
    // Processing state
    protected Set<Long> visited = new HashSet<>();
    protected int numSamples;
    protected Entity retiredDataFolder;
    protected Set<Long> retiredIds;
    
    public void execute() throws Exception {

        if (isDebug) {
            logger.info("This is a test run. No entities will be moved or deleted.");
        }
        else {
            logger.info("This is the real thing. Entities will be moved and/or deleted!");
        }

        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        
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
            logger.info("Running desynced sample retirement for all "+ownerKey+" samples");
    	    
        	List<Entity> samples = entityBean.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_DESYNC);
        	if (null==samples) {
				logger.info("User "+ownerKey+" has null returned for samples");
				return;
			}
			logger.info("Processing "+samples.size()+" desynced samples");
            int counter = 0;
			for(Entity sample : samples) {
                try {
                    if (!sample.getOwnerKey().equals(ownerKey)) {
                        continue;
                    }
                    if (!sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
                        logger.warn("Ignoring non-Sample entity with desync status: "+sample.getId());
                        continue;
                    }
                    counter++;
					logger.info("Processing sample "+counter+" of "+samples.size()+": "+sample.getName()+" (id="+sample.getId()+")");
					retireSample(sample);
                }
                catch (Exception e) {
                    logger.error("Error processing sample: "+sample.getName(),e);
                }
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

		logger.info("Processed "+numSamples+" samples.");
    }
    
    public void retireSample(Entity sample) throws Exception {
    	
    	if (visited.contains(sample.getId())) return;
    	visited.add(sample.getId());
    	
		numSamples++;

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
    }
    
    
}
