package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelper extends EntityHelper {

    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    
    protected AnnotationBeanLocal annotationBean;
    protected boolean resetSampleNames = true;
    
    private Entity topLevelFolder;
    private Entity trashFolder;
    private List<Entity> dataSets;
    private Map<String,Entity> dataSetFolderByIdentifier = new HashMap<String,Entity>();
    private Map<String,Entity> dataSetEntityByIdentifier = new HashMap<String,Entity>();
    private int numSamplesCreated = 0;
    private int numSamplesUpdated = 0;
    private int numSamplesAdded = 0;
    
    public SampleHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, String ownerKey, Logger logger) {
        super(entityBean, computeBean, ownerKey, logger);
        this.annotationBean = annotationBean;
    }

    /**
     * Clear all the visited flags on all entities owned by the user.
     */
    public void clearVisited() throws Exception {
        annotationBean.deleteAttribute(ownerKey, EntityConstants.ATTRIBUTE_VISITED);
    }
    
    /**
     * Set the visited flag on a given entity.
     * @param entity
     * @return
     * @throws Exception
     */
    public Entity setVisited(Entity entity) throws Exception {
        if (!EntityUtils.addAttributeAsTag(entity, EntityConstants.ATTRIBUTE_VISITED)) {
            throw new IllegalStateException("Could not set visited flag for "+entity.getName());
        }
        return entityBean.saveOrUpdateEntity(entity);
    }

    /**
     * Create a sample or update it if it already exists.
     * @param parentSample
     * @param slideCode
     * @param dataSet
     * @param tileGroupList
     * @return
     * @throws Exception
     */
    public Entity createOrUpdateSample(Entity parentSample, String slideCode, Entity dataSet, 
            Collection<SlideImageGroup> tileGroupList) throws Exception {

        logger.info("Creating or updating sample: "+slideCode+" ("+(parentSample==null?"":("parentSample="+parentSample.getName()+", "))+"dataSet="+dataSet.getName()+")");
        
        Multimap<String,SlideImageGroup> objectiveGroups = HashMultimap.<String,SlideImageGroup>create();
        for(SlideImageGroup tileGroup : tileGroupList) {
            String groupObjective = null;
            for(SlideImage slideImage : tileGroup.getImages()) {
                if (groupObjective==null) {
                    groupObjective = slideImage.getObjective();
                }
                else if (groupObjective != slideImage.getObjective()) {
                    logger.warn("  No consensus for objective in tile group '"+tileGroup.getTag()+"' ("+groupObjective+" != "+slideImage.getObjective()+")");
                }
            }
            if (groupObjective!=null) {
                objectiveGroups.put(groupObjective, tileGroup);
            }
        }    
        
        logger.debug("  Sample objectives: "+objectiveGroups.keySet());
        Entity sample = null;
        
        if (objectiveGroups.isEmpty()) {
            throw new IllegalStateException("Sample "+slideCode+" has no objective groups");
        }
        else if (objectiveGroups.keySet().size()>1) {
            
            if (parentSample!=null) {
                throw new IllegalStateException("Sample "+slideCode+" has already been subdivided but still contains multiple objectives: "+parentSample.getId());
            }
            
            // There is more than one objective. Create a parent Sample, and then a SubSample for each objective.
            sample = getOrCreateSample(slideCode, dataSet, null, null, tileGroupList, false);
            String childObjective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            
            if (childObjective!=null) {
                logger.info("  Converting existing "+childObjective+" sample into a child sample: "+sample.getId());
                
                Entity childSample = sample;
                // This is an objective-specific sample which was defined in the past. 
                // Convert it into a child sample, and create a new parent. 
                childSample.setName(childSample.getName()+"~"+childObjective);
                entityBean.saveOrUpdateEntity(sample);
                
                // temporarily clear the data set so that the sample is removed from data set folders
                childSample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, null);
                putInCorrectDataSetFolder(childSample);
                
                // Create a new parent
                sample = getOrCreateSample(slideCode, dataSet, null, null, tileGroupList, false);
            }
            
            List<String> objectives = new ArrayList<String>(objectiveGroups.keySet());
            Collections.sort(objectives);
            for(String objective : objectives) {
                Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                createOrUpdateSample(sample, slideCode, null, subTileGroupList);
            }

            putInCorrectDataSetFolder(sample);
        }
        else {
            String objective = objectiveGroups.keySet().iterator().next();
            
            logger.info("  Sample has a single objective group: "+objective);
            
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(tileGroupList);
            String sampleChannelSpec = getDefaultChanSpec(sampleNumSignals+1);
            logger.info("  Sample has "+sampleNumSignals+" signal channels, and thus specification '"+sampleChannelSpec+"'");
            
            // Find the sample, if it exists, or create a new one.
            sample = getOrCreateSample(slideCode, dataSet, sampleChannelSpec, objective, tileGroupList, parentSample!=null);
            synchronizeTiles(sample, tileGroupList);
            
            // Ensure the sample is a child of something
            if (parentSample==null) {
                putInCorrectDataSetFolder(sample);
            }
            else {
                if (EntityUtils.findChildWithEntityId(parentSample, sample.getId())==null) {
                    addToParent(parentSample, sample, parentSample.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);    
                }
            }
        }

        setVisited(sample);
        return sample;
    }
    
    protected Entity getOrCreateSample(String slideCode, Entity dataSet, String channelSpec, String objective, 
            Collection<SlideImageGroup> tileGroupList, boolean isSubSample) throws Exception {

        // Find consensus values in the images which could be represented in the sample
        Map<String,String> sampleProperties = new HashMap<String,String>();
        for(SlideImageGroup slideImageGroup : tileGroupList) {
            for(SlideImage slideImage : slideImageGroup.getImages()) {
                Map<String,String> imageProps = slideImage.getProperties();
                for(String key : imageProps.keySet()) {
                    String value = imageProps.get(key);
                    String consensusValue = sampleProperties.get(key);
                    if (consensusValue==null) {
                        sampleProperties.put(key, value);
                    }
                    else if (!consensusValue.equals(value)) {
                        sampleProperties.put(key, NO_CONSENSUS_VALUE);
                    }
                }
            }
        }
        
        if (!slideCode.equals(sampleProperties.get(EntityConstants.ATTRIBUTE_SLIDE_CODE))) {
            throw new IllegalStateException("Sample consensus attribute Slide Code does not match slide code");
        }
        
        Entity sample = findOrAnnexExistingSample(tileGroupList, dataSet, sampleProperties);
        
        if (sample == null) {
            sample = createSample(dataSet, channelSpec, objective, sampleProperties, isSubSample);
            numSamplesCreated++;
        }
        else {
            String newName = getSampleName(dataSet, objective, isSubSample, sampleProperties);
            if (resetSampleNames && !sample.getName().equals(newName)) {
                logger.info("  Updating sample name to: "+newName);
                sample.setName(newName);
                // No need to save the new name, since we'll save the sample in setSampleAttributes below
            }
            
            setSampleAttributes(sample, dataSet, channelSpec, objective, sampleProperties);
            entityLoader.populateChildren(sample);
            numSamplesUpdated++;
        }
        
        return sample;
    }
    
    /**
     * Find the sample with the given LSMs. If it doesn't exist to the owner, then annex it. If it doesn't exist, 
     * try to annex a legacy sample from FlyLight, based on the putative sample identifier.
     * @param tileGroupList
     * @param dataSet
     * @param sampleProperties
     * @return
     * @throws ComputeException
     */
    public Entity findOrAnnexExistingSample(Collection<SlideImageGroup> tileGroupList, Entity dataSet, Map<String,String> sampleProperties) throws Exception {

        Set<String> lsmNames = new HashSet<String>();
        for(SlideImageGroup slideImageGroup : tileGroupList) {
            for(SlideImage slideImage : slideImageGroup.getImages()) {
                String lsmName = slideImage.getFile().getName();
                lsmNames.add(lsmName);
            }
        }
        
        logger.info("  Looking for existing sample with LSM set: "+lsmNames);
        
        Entity matchedSample = null;
        Set<Long> visitedSamples = new HashSet<Long>();
        
        for(String lsmName : lsmNames) {
            for(Entity lsm : entityBean.getEntitiesByName(lsmName)) {
                Entity sample = entityBean.getAncestorWithType(null, lsm.getId(), EntityConstants.TYPE_SAMPLE);
                if (sample!=null) {
                    if (visitedSamples.contains(sample.getId())) {
                        continue;
                    }
                    visitedSamples.add(sample.getId());
                    
                    Set<String> matchedLsmNames = new HashSet<String>();
                    entityLoader.populateChildren(sample);
                    Entity supportingData = EntityUtils.getSupportingData(sample);
                    if (supportingData != null) {
                        entityLoader.populateChildren(supportingData);
                        for(Entity imageTile : supportingData.getChildren()) {
                            entityLoader.populateChildren(imageTile);    
                            for(Entity siblingLsm : imageTile.getChildren()) {
                                matchedLsmNames.add(siblingLsm.getName());
                            }
                        }
                    }
                    
                    if (matchedLsmNames.equals(lsmNames)) {
                        logger.info("  Found sample with matching LSM set: "+sample.getName());
                        matchedSample = sample;
                        break;
                    }
                }
            }
            if (matchedSample!=null) break;
        }
        
        if (matchedSample==null) {
            // Did not find any matching samples, try legacy name-based search
            
            String sampleIdentifier = getSampleName(dataSet, null, false, sampleProperties);
            List<Entity> matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(null, 
                    sampleIdentifier, EntityConstants.TYPE_SAMPLE);
            
            for(Entity entity : matchingSamples) {
                if (entity.getOwnerKey().equals("group:flylight")) {
                    logger.info("  Found legacy FlyLight sample with matching putative name: "+sampleIdentifier);
                    return entity;
                }
            }            
            
            return null;
        }
        else {
            // Found matching sample
            if (matchedSample.getOwnerKey().equals(ownerKey)) {
                // Reuse existing example
                return matchedSample;
            }
            else {
                // Need to annex the sample if possible
                if ("group:flylight".equals(ownerKey)) {
                    // FlyLight cannot steal samples from others
                    return null;
                }
                else {
                    return entityBean.annexEntityTree(ownerKey, matchedSample.getId());        
                }
                
            }
        }
    }
    
    /**
     * Create a sample with the given name and set the sample attributes. 
     * @param name
     * @param channelSpec
     * @param dataSetIdentifier
     * @param objective
     * @return
     * @throws Exception
     */
    public Entity createSample(Entity dataSet, String channelSpec, String objective, Map<String,String> sampleProperties, boolean isSubSample) throws Exception {

        String name = getSampleName(dataSet, objective, isSubSample, sampleProperties);
        logger.info("  Creating sample "+name);
        Date createDate = new Date();
        Entity sample = new Entity();
        sample.setOwnerKey(ownerKey);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        
        setSampleAttributes(sample, dataSet, channelSpec, objective, sampleProperties);
        
        return sample;
    }
    
    /** 
     * Create a new name for a sample, given the sample's attributes.
     * {Line}-{Slide Code}-Right_Optic_Lobe
     * {Line}-{Slide Code}-Left_Optic_Lobe
     * {Line}-{Effector}-{Age}
     */
    public String getSampleName(Entity dataSet, String objective, boolean isSubSample, Map<String,String> sampleProperties) {

        String sampleNameSuffix = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);

        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(sampleNameSuffix);
        StringBuffer buffer = new StringBuffer();
        logger.trace("    Building sample name:");
        while (matcher.find()) {
            String replacement = sampleProperties.get(matcher.group(1));
            if (replacement != null) {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
            logger.trace("        "+replacement+" -> "+buffer);
        }
        matcher.appendTail(buffer);
        logger.trace("        append tail -> "+buffer);
        if (isSubSample && !StringUtils.isEmpty(objective)) {
            buffer.append("~"+objective);
            logger.trace("        append subsample -> "+buffer);
        }
        return buffer.toString();
    }

    /**
     * Set the sample attributes.
     * @param sample the Sample entity
     * @param slideCode the slide code
     * @param dataSet the data set (e.g. "flylight_whole_brain")
     * @param channelSpec the channel specification for the secondary data (not the input stacks) in the sample (e.g. "sssr")
     * @param objective the objective, if the sample is specific to a single objective (e.g. "63x")
     * @return
     * @throws Exception
     */
    public Entity setSampleAttributes(Entity sample, Entity dataSet, String channelSpec, String objective, Map<String,String> sampleProperties) throws Exception {
        String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        logger.debug("    Setting sample properties: "+sampleProperties);
        
        // Find out which attributes a Sample can support. We only want to set those. 
        Set<String> attrs = new HashSet<String>();
        for(EntityAttribute attr : sample.getEntityType().getAttributes()) {
            attrs.add(attr.getName());
        }
        
        // Set all the properties that we can. 
        for(String key : sampleProperties.keySet()) {
            String value = sampleProperties.get(key);
            if (attrs.contains(key) && value!=null && !NO_CONSENSUS_VALUE.equals(value)) {
                sample.setValueByAttributeName(key, value);   
            }
        }

        // Some attributes are known explicitly, so let's overwrite whatever the image properties gave us above. 
        if (dataSetIdentifier!=null) {
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier);   
        }
        if (channelSpec!=null) {
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec);   
        }
        if (objective!=null) {
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE, objective); 
        }
        
        sample = entityBean.saveOrUpdateEntity(sample);
        return sample;
    }    

    /**
     * Given a collection of SlideImage groups, this method walks through each one and adds it to the sample, or
     * updates the existing structure to match. Any image tiles in the sample which are not in the given list are 
     * deleted.
     * @param sample
     * @param tileGroupList
     * @throws Exception
     */
    public void synchronizeTiles(Entity sample, Collection<SlideImageGroup> tileGroupList) throws Exception {
        
        Set<String> tileNameSet = new HashSet<String>();
        for (SlideImageGroup tileGroup : tileGroupList) {
            addTileToSample(sample, tileGroup);
            tileNameSet.add(tileGroup.getTag());
        }
        
        Entity supportingFolder = EntityUtils.getSupportingData(sample);
        for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFolder, EntityConstants.TYPE_IMAGE_TILE)) {
            if (!tileNameSet.contains(imageTile.getName())) {
                logger.info("  Removing superfluous image tile: "+imageTile.getName());
                entityBean.deleteEntityTree(imageTile.getOwnerKey(), imageTile.getId());
            }
        }
    }
    
    /**
     * Add the given group of SlideImages to the specified sample. Any necessary supporting structures will be created
     * or updated, as long as the Sample entity itself exists. if the tile already exists in the sample, it is checked
     * to make sure the slide images are the same. If they are the same then the LSM Stack attributes are just updated.
     * If they differ then the existing tile is deleted and recreated with the correct slide images.
     * 
     * @param sample
     * @param tileGroup
     * @throws Exception
     */
    public void addTileToSample(Entity sample, SlideImageGroup tileGroup) throws Exception {
        
        // Get the existing Supporting Files, or create a new one
        Entity supportingFiles = EntityUtils.getSupportingData(sample);
        
        if (supportingFiles == null) {
            supportingFiles = createSupportingFilesFolder();
            addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
        }
        else {
            if (!EntityUtils.areLoaded(supportingFiles.getEntityData())) {
                entityBean.loadLazyEntity(supportingFiles, true);
            }
        }
        
        EntityData imageTileEd = EntityUtils.findChildEntityDataWithNameAndType(supportingFiles, tileGroup.getTag(), EntityConstants.TYPE_IMAGE_TILE);
        Entity imageTile = null;
        
        if (imageTileEd != null) {
            imageTile = imageTileEd.getChildEntity();
            if (!existingTileMatches(imageTile, tileGroup)) {
                logger.info("  Tile '"+imageTile.getName()+"' (id="+imageTileEd.getId()+") has changed, will delete and recreate it.");
                entityBean.deleteSmallEntityTree(imageTile.getOwnerKey(), imageTile.getId());
                imageTile = null;
            }
            else {
                logger.info("  Tile '"+imageTile.getName()+"' exists (id="+imageTileEd.getId()+")");
                for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                    for(SlideImage image : tileGroup.getImages()) {
                        if (image.getFile().getName().equals(lsmStack.getName())) {
                            setLsmStackAttributes(lsmStack, image);
                        }
                    }
                }
            }
        }
        
        if (imageTile == null) {
            Date createDate = new Date();
            imageTile = new Entity();
            imageTile.setOwnerKey(ownerKey);
            imageTile.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_IMAGE_TILE));
            imageTile.setCreationDate(createDate);
            imageTile.setUpdatedDate(createDate);
            imageTile.setName(tileGroup.getTag());
            imageTile = entityBean.saveOrUpdateEntity(imageTile);
            logger.info("  Saved image tile '"+imageTile.getName()+"' as "+imageTile.getId());
            addToParent(supportingFiles, imageTile, null, EntityConstants.ATTRIBUTE_ENTITY);
            
            for(SlideImage image : tileGroup.getImages()) {
                logger.info("    Adding LSM file to sample: "+image.getFile().getName());
                Entity lsmEntity = createLsmStackFromFile(image);
                addToParent(imageTile, lsmEntity, imageTile.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
        }
    }

    /**
     * 
     * @param imageTile
     * @param tileGroup
     * @return
     */
    protected boolean existingTileMatches(Entity imageTile, SlideImageGroup tileGroup) {

        List<SlideImage> images = tileGroup.getImages();
        List<Entity> currTiles = EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK);
        
        Set<String> newFilenames = new HashSet<String>();
        for(SlideImage image : images) {
            newFilenames.add(image.getFile().getName());
        }
        
        if (images.size() != currTiles.size()) {
            return false;
        }
        
        Set<String> currFilenames = new HashSet<String>();      
        for(Entity lsmStack : currTiles) {
            currFilenames.add(lsmStack.getName());
            if (!newFilenames.contains(lsmStack.getName())) {
                return false;
            }
        }
        
        for(SlideImage image : tileGroup.getImages()) {
            if (!currFilenames.contains(image.getFile().getName())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create and return a supporting files folder. 
     * @return
     * @throws Exception
     */
    public Entity createSupportingFilesFolder() throws Exception {
        Date createDate = new Date();
        Entity filesFolder = new Entity();
        filesFolder.setOwnerKey(ownerKey);
        filesFolder.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SUPPORTING_DATA));
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("  Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    /**
     * Create and return an LSM Stack entity based on the given SAGE image data.
     * @param image
     * @return
     * @throws Exception
     */
    public Entity createLsmStackFromFile(SlideImage image) throws Exception {
        Date createDate = new Date();
        Entity lsmStack = new Entity();
        lsmStack.setOwnerKey(ownerKey);
        lsmStack.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK));
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(image.getFile().getName());
        lsmStack = setLsmStackAttributes(lsmStack, image);
        logger.info("      Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
    
    /**
     * Set the LSM stack attribuets from the given SAGE image data.
     * @param lsmStack
     * @param image
     * @return
     * @throws Exception
     */
    public Entity setLsmStackAttributes(Entity lsmStack, SlideImage image) throws Exception {
        Map<String,String> imageProperties = image.getProperties();
        logger.debug("    Setting LSM stack properties:"+imageProperties);
        
        // Find out which attributes a Sample can support. We only want to set those. 
        Set<String> attrs = new HashSet<String>();
        for(EntityAttribute attr : lsmStack.getEntityType().getAttributes()) {
            attrs.add(attr.getName());
        }
        
        // Set all the properties that we can. 
        for(String key : imageProperties.keySet()) {
            String value = imageProperties.get(key);
            if (attrs.contains(key) && value!=null && !NO_CONSENSUS_VALUE.equals(value)) {
                lsmStack.setValueByAttributeName(key, value);   
            }
        }
        lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        return lsmStack;
    }
    
    /**
     * Returns a default channel specification (reference channel last) for the given number of channels.
     * @param numSignals
     * @return
     */
    public String getDefaultChanSpec(int numChannels) {
        int numSignals = numChannels-1;
        StringBuilder buf = new StringBuilder();
        for(int j=0; j<numSignals; j++) {
            buf.append("s");
        }
        buf.append("r");
        return buf.toString();
    }
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsmEntity
     * @return
     */
    public String getLSMChannelSpec(Entity lsmEntity) {
        String chanSpec = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (!StringUtils.isEmpty(chanSpec)) return chanSpec;
        String numChannelsStr = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (!StringUtils.isEmpty(numChannelsStr)) {
            try {
                return getDefaultChanSpec(Integer.parseInt(numChannelsStr));    
            }
            catch (NumberFormatException e) {
                logger.warn("Could not parse Num Channels ('"+numChannelsStr+"') on LSM entity with id="+lsmEntity.getId());
            }
        }
        throw new IllegalStateException("LSM has no Channel Specification and no Num Channels");
    }

    /**
     * Calculate and return the number of signal channels across all the SlideImages in a tile group (and thus, the 
     * number of signal channels in the eventual merged tile that results from this group).
     * @param tileGroupList
     * @return
     */
    public int getNumSignalChannels(Collection<SlideImageGroup> tileGroupList) {
        int sampleNumSignals = -1;
        for(SlideImageGroup tileGroup : tileGroupList) {

            int tileNumSignals = 0;
            logger.debug("  Calculating number of channels in tile "+tileGroup.getTag());
            for(SlideImage slideImage : tileGroup.getImages()) {
                if (slideImage.getChannelSpec()!=null) {
                    for(int j=0; j<slideImage.getChannelSpec().length(); j++) {
                        if (slideImage.getChannelSpec().charAt(j)=='s') {
                            tileNumSignals++;
                        }
                    }
                }
            }
            
            if (tileNumSignals<1) {
                logger.debug("  Falling back on channel number");
                // We didn't get the information from the channel spec, let's fall back on inference from numChannels
                for(SlideImage slideImage : tileGroup.getImages()) {
                    if (slideImage.getChannels()!=null) {
                        tileNumSignals += Integer.parseInt(slideImage.getChannels()) - 1;
                    }
                }
            }
            
            logger.debug("  Tile "+tileGroup.getTag()+" has "+tileNumSignals+" signal channels");
            
            if (sampleNumSignals<0) {
                sampleNumSignals = tileNumSignals;
            }
            else if (sampleNumSignals != tileNumSignals) {
                logger.warn("  No consensus for number of signal channels per tile ("+sampleNumSignals+" != "+tileNumSignals+")");
            }
        }
        return sampleNumSignals;
    }
    
    /**
     * Go through a sample area's LSM supporting files and look for an entity attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param sampleEntity
     * @param attrName
     * @param areaName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(Entity sampleEntity, String attrName, String areaName) throws Exception {
        String consensus = null;
        entityLoader.populateChildren(sampleEntity);
        Entity supportingData = EntityUtils.getSupportingData(sampleEntity);
        if (supportingData==null) {
            logger.error("Sample has no supporting data: "+sampleEntity.getId());
            return null;
        }
        entityLoader.populateChildren(supportingData);
        for(Entity tile : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
            entityLoader.populateChildren(tile);
            for(Entity image : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {    
                String lsmArea = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if (areaName==null || areaName.equals(lsmArea)) {
                    String value = image.getValueByAttributeName(attrName);
                    if (consensus!=null && !consensus.equals(value)) {
                        logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample "+sampleEntity.getId());
                        return null;
                    }
                    else {
                        consensus = value;
                    }
                }
            }
        }
        return consensus;
    }

    /**
     * Returns a space-delimited list of channel indexes containing signal channels.
     * @param channelSpec channel specification (e.g. "rsss")
     * @return zero-indexed signal channels (e.g. "1 2 3")
     */
    public String getSignalChannelIndexes(String channelSpec) {
        return getChannelIndexes(channelSpec, 's');
    }

    /**
     * Returns a space-delimited list of channel indexes containing reference channels.
     * @param channelSpec channel specification (e.g. rsss)
     * @return zero-indexed reference channels (e.g. "0")
     */
    public String getReferenceChannelIndexes(String channelSpec) {
        return getChannelIndexes(channelSpec, 'r');
    }
    
    private String getChannelIndexes(String channelSpec, char channelCode) {
        StringBuilder builder = new StringBuilder();
        if (channelSpec!=null) {
            for(int i=0; i<channelSpec.length(); i++) {
                if (channelSpec.charAt(i) == channelCode) {
                    if (builder.length()>0) builder.append(" ");
                    builder.append(""+i);
                }
            }
        }
        return builder.toString();
    }

    /**
     * Remove the given sample from any incorrect data set folders and add it to the correct data set folder, based on
     * its data set identifier attribute.
     * @param sample
     * @throws Exception
     */
    public void putInCorrectDataSetFolder(Entity sample) throws Exception {
        loadDataSets();
        String sampleDataSetIdentifier = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        logger.info("  Putting sample "+sample.getName()+" in data set folder for "+sampleDataSetIdentifier);   
        for(String dataSetIdentifier : dataSetEntityByIdentifier.keySet()) {
            Entity dataSetFolder = dataSetFolderByIdentifier.get(dataSetIdentifier);
            // Either this is the folder we want, or the folder at least exists so that we can remove from it if necessary
            if (dataSetIdentifier.equals(sampleDataSetIdentifier) || dataSetFolder!=null) {
                if (dataSetFolder==null) {
                    Entity dataSet = dataSetEntityByIdentifier.get(dataSetIdentifier);
                    dataSetFolder = verifyOrCreateChildFolder(getTopLevelDataSetFolder(), dataSet.getName());
                    dataSetFolderByIdentifier.put(dataSetIdentifier, dataSetFolder);
                }
                EntityData ed = EntityUtils.findChildEntityDataWithChildId(dataSetFolder, sample.getId());
                if (dataSetIdentifier.equals(sampleDataSetIdentifier)) {
                    if (ed==null) {
                        logger.info("    Adding to data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");   
                        addToParent(dataSetFolder, sample, dataSetFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
                        numSamplesAdded++;
                    }
                }
                else {
                    if (ed!=null) {
                        logger.info("    Removing from data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");   
                        dataSetFolder.getEntityData().remove(ed);
                        entityBean.deleteEntityData(ed);
                    }
                }
            }
        }
    }

    /**
     * Return the data sets for the configured owner.
     * @return
     * @throws Exception
     */
    public Collection<Entity> getDataSets() throws Exception {
        if (dataSets==null) {
            loadDataSets();
        }
        return dataSets;
    }

    /**
     * Return the top level data set folder for the configured owner.
     * @return
     * @throws Exception
     */
    public Entity getTopLevelDataSetFolder() throws Exception {
        if (topLevelFolder==null) {
            loadTopLevelFolder();
        }
        return topLevelFolder;
    }

    /**
     * Return the top level trash folder for the configured owner.
     * @return
     * @throws Exception
     */
    public Entity getTrashFolder() throws Exception {
        if (trashFolder==null) {
            loadTrashFolder();
        }
        return trashFolder;
    }

    public int getNumSamplesCreated() {
        return numSamplesCreated;
    }

    public int getNumSamplesUpdated() {
        return numSamplesUpdated;
    }

    public int getNumSamplesAdded() {
        return numSamplesAdded;
    }
    
    public Map<String, Entity> getDataSetFolderByIdentifierMap() {
        return dataSetFolderByIdentifier;
    }

    public Map<String, Entity> getDataSetEntityByIdentifierMap() {
        return dataSetEntityByIdentifier;
    }

    private void loadDataSets() throws Exception {
        
        if (dataSets!=null) return;
        loadTopLevelFolder();
        
        this.dataSetFolderByIdentifier = new HashMap<String,Entity>();
        this.dataSetEntityByIdentifier = new HashMap<String,Entity>();
        this.dataSets = entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_DATA_SET);
        if (dataSets.isEmpty()) {
            logger.info("No data sets found for user: "+ownerKey);
            return;
        }        

        logger.info("Preloading data sets...");
        entityLoader.populateChildren(getTopLevelDataSetFolder());
        for(Entity dataSet : dataSets) {
            
            // Cache the data set
            String dataSetIdentifier = dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
            dataSetEntityByIdentifier.put(dataSetIdentifier, dataSet);
            
            // Cache the folder
            Entity dataSetFolder = EntityUtils.findChildWithName(getTopLevelDataSetFolder(), dataSet.getName());
            if (dataSetFolder!=null) {
                logger.info("Preloading data set: "+dataSet.getName());
                entityLoader.populateChildren(dataSetFolder);
                dataSetFolderByIdentifier.put(dataSetIdentifier, dataSetFolder);
            }
        }
    }
    
    private void loadTopLevelFolder() throws Exception {
        if (topLevelFolder!=null) return;
        logger.info("Getting data set folder...");
        this.topLevelFolder = createOrVerifyRootEntity(EntityConstants.NAME_DATA_SETS, true, false);
    }
    
    private void loadTrashFolder() throws Exception {
        if (trashFolder!=null) return;
        logger.info("Getting trash folder...");
        this.trashFolder = createOrVerifyRootEntity(EntityConstants.NAME_TRASH, true, false);
    }

    public boolean isResetSampleNames() {
        return resetSampleNames;
    }

    public void setResetSampleNames(boolean resetSampleNames) {
        this.resetSampleNames = resetSampleNames;
    }
}
