package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.ISO8601Utils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Synchronize the folder hierarchy for a Fly Line Release. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncReleaseFoldersService extends AbstractEntityService {
	
    private Entity topLevelFolder;
    private SampleHelper sampleHelper;
    private Entity releaseFolder;
    private Multimap<String,Entity> samplesByLine = ArrayListMultimap.<String,Entity>create();
    
    public void execute() throws Exception {
        
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        
    	Long releaseEntityId = data.getRequiredItemAsLong("RELEASE_ENTITY_ID");
    	Entity releaseEntity = entityBean.getEntityById(releaseEntityId);
    	if (releaseEntity == null) {
    		throw new IllegalArgumentException("Release entity not found with id="+releaseEntityId);
    	}

        String releaseDateStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_RELEASE_DATE);
        if (releaseDateStr == null) {
            throw new IllegalArgumentException("Release has no release date: "+releaseEntityId);
        }
        
        DateTime releaseDate = new DateTime(ISO8601Utils.parse(releaseDateStr));
        logger.info("Release date: "+releaseDate);
        
        DateTime cutoffDate = null;
        String lagTimeStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_LAG_TIME_MONTHS);
        if (lagTimeStr != null) {
            int lagTime = Integer.parseInt(lagTimeStr);
            cutoffDate = releaseDate.minus(Period.months(lagTime));
            logger.info("Cutoff date: "+cutoffDate);
        }
        
    	loadTopLevelFolder();
    	this.releaseFolder = sampleHelper.verifyOrCreateChildFolder(topLevelFolder, releaseEntity.getName());

    	Set<String> includedDataSets = new HashSet<String>();
    	String dataSetStr = releaseEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SETS);
    	if (!StringUtils.isEmpty(dataSetStr)) {
    	    for(String dataSetIdentifier : dataSetStr.split(",")) {
    	        includedDataSets.add(dataSetIdentifier);
    	    }
    	}
    	
    	List<Entity> dataSets = new ArrayList<>();
    	for(Entity dataSetEntity : entityBean.getEntitiesByTypeName(ownerKey, EntityConstants.TYPE_DATA_SET)) {
    	    String identifier = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    	    if (includedDataSets.isEmpty() || includedDataSets.contains(identifier)) {
    	        dataSets.add(dataSetEntity);
    	    }
    	}
    	
    	int samplesAdded = 0;
    	for(Entity dataSetEntity : dataSets) {
    	    String identifier = dataSetEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
    	    logger.debug("Processing data set "+identifier);
    	    for(Entity sample : entityBean.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, identifier)) {
                logger.debug("  Processing sample "+sample.getName());
    	        if (sample.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE)) {
    	            String completionDateStr = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_COMPLETION_DATE);
    	            if (completionDateStr!=null) {
    	                String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
    	                if (EntityConstants.VALUE_BLOCKED.equals(status)) {
    	                    logger.trace("    Sample is blocked");
    	                }
    	                else if (EntityConstants.VALUE_RETIRED.equals(status)) {
    	                    logger.trace("    Sample is retired");
    	                }
    	                else {
    	                    String line = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_LINE);
    	                    if (line==null) {
    	                        logger.warn("    Cannot process sample without line: "+sample.getId());
    	                        continue;
    	                    }
        	                DateTime completionDate = new DateTime(ISO8601Utils.parse(completionDateStr));
        	                if (cutoffDate==null || cutoffDate.isAfter(completionDate)) {
        	                    samplesByLine.put(line, sample);
        	                    logger.debug("    Adding sample to line: "+line);
        	                    samplesAdded++;
        	                }
        	                else {
        	                    logger.debug("    Sample completed after cutoff date: "+completionDate);
        	                }
    	                }
    	            }
    	            else {
    	                logger.debug("    Sample has no completion date");
    	            }
    	        }
    	    }
    	}

        logger.info("Considering "+samplesByLine.keySet().size()+" fly lines, with "+samplesAdded+" samples");
    	
        int numAdded = 0;
    	List<String> lines = new ArrayList<>(samplesByLine.keySet());
    	Collections.sort(lines);
    	for(String line : lines) {

            logger.debug("Processing line "+line);
            List<Entity> samples = new ArrayList<>(samplesByLine.get(line));
            
            // Ensure there is at least one 63x polarity sample

            boolean has63xPolaritySample = false;
            for(Entity sample : samples) {
                if (has63xPolaritySample(sample)) {
                    has63xPolaritySample = true;
                    break;
                }
            }
    	    
            if (!has63xPolaritySample) {
                logger.debug("  Ignoring line which has no 63x polarity samples");
                continue;
            }
            
    	    Entity lineFolder = null;
    	    EntityData lineFolderEd = EntityUtils.findChildEntityDataWithName(releaseFolder, line);
    	    if (lineFolderEd==null) {
    	        lineFolder = verifyOrCreateChildFolder(releaseFolder, line);
    	    }
    	    else {
    	        lineFolder = lineFolderEd.getChildEntity();
    	    }
    	    
    	    // Sort samples
    	    Collections.sort(samples, new Comparator<Entity>() {
                @Override
                public int compare(Entity o1, Entity o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
    	    
    	    // Add missing samples
    	    for(Entity sample : samples) {
    	        logger.debug("  Processing sample "+sample.getName());
    	        EntityData ed = EntityUtils.findChildEntityDataWithChildId(lineFolder, sample.getId());
    	        if (ed==null) {
    	            logger.debug("    Adding to line folder: "+lineFolder.getName()+" (id="+lineFolder.getId()+")");   
    	            sampleHelper.addToParent(lineFolder, sample, lineFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
    	            numAdded++;
    	        }
    	    }
    	    
    	    // Re-sort line folder
            List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(lineFolder);
            sortByChildName(eds);
    	}

        // Re-sort release folder
        List<EntityData> eds = EntityUtils.getOrderedEntityDataWithChildren(releaseFolder);
        sortByChildName(eds);

        logger.info("Added "+numAdded+" samples to line folders");
        
        processData.putItem("RELEASE_FOLDER_ID", releaseFolder.getId().toString());
        contextLogger.info("Putting '"+releaseFolder.getId()+"' in RELEASE_FOLDER_ID");
    }
    
    private boolean has63xPolaritySample(Entity sample) throws Exception {

        String identifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        if (identifier==null || !identifier.contains("polarity")) {
            // If the parent sample is not a polarity sample then we don't need to check anything else
            return false;
        }
        
        String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
        if ("63x".equals(objective)) {
            return true;
        }
        else if (objective==null) {
            // Check for sub-samples
            entityLoader.populateChildren(sample);
            for(Entity subsample : sample.getChildren()) {
                objective = subsample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
                if ("63x".equals(objective)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private void sortByChildName(List<EntityData> eds) throws Exception {
        Collections.sort(eds, new Comparator<EntityData>() {
            @Override
            public int compare(EntityData o1, EntityData o2) {
                Entity s1 = o1.getChildEntity();
                Entity s2 = o2.getChildEntity();
                return s1.getName().compareTo(s2.getName());

            }
        });
        int index = 1;
        for(EntityData ed : eds) {
            if ((ed.getOrderIndex() == null) || (ed.getOrderIndex() != index)) {
                logger.info("Updating index: "+ed.getOrderIndex()+" -> "+index);
                entityBean.updateChildIndex(ed, index);
            }
            index++;
        }
    }

    public Entity verifyOrCreateChildFolder(Entity parentFolder, String childName) throws Exception {

        entityLoader.populateChildren(parentFolder);
        
        Entity folder = null;
        for (Entity child : EntityUtils.getChildrenOfType(parentFolder, EntityConstants.TYPE_FOLDER)) {
            if (child.getName().equals(childName)) {
                if (folder != null) {
                    logger.warn("Unexpectedly found multiple child folders with name=" + childName+" for parent folder id="+parentFolder.getId());
                }
                else {
                    folder = child;
                }
            }
        }
        
        if (folder == null) {
            // We need to create a new folder
            Date createDate = new Date();
            folder = new Entity();
            folder.setCreationDate(createDate);
            folder.setUpdatedDate(createDate);
            folder.setOwnerKey(ownerKey);
            folder.setName(childName);
            folder.setEntityTypeName(EntityConstants.TYPE_FOLDER);
            folder = entityBean.saveOrUpdateEntity(folder);
            entityHelper.addToParent(parentFolder, folder, parentFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
        }
        
        return folder;
    }
    
    private void loadTopLevelFolder() throws Exception {
        if (topLevelFolder!=null) return;
        logger.info("Getting releases folder...");
        this.topLevelFolder = sampleHelper.createOrVerifyRootEntity(EntityConstants.NAME_FLY_LINE_RELEASES, true, false);
        if (topLevelFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(topLevelFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            entityBean.saveOrUpdateEntity(topLevelFolder);
        }
    }
}
