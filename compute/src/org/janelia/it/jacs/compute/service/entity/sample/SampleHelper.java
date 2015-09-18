package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelper extends EntityHelper {
    
    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";
    private static final DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
    private static final Set<String> explicitSampleAttrs = new HashSet<>();
    
    static {
        explicitSampleAttrs.add(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        explicitSampleAttrs.add(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        explicitSampleAttrs.add(EntityConstants.ATTRIBUTE_OBJECTIVE);
    }
    
    protected AnnotationBeanLocal annotationBean;
    
    private Entity topLevelFolder;
    private Entity retiredDataFolder;
    private Entity blockedDataFolder;
    private List<Entity> dataSets;
    private String dataSetNameFilter;
    private Map<String,Entity> dataSetFolderByIdentifier;
    private Map<String,Entity> dataSetEntityByIdentifier;
    private Set<Long> samplesToAnnex = new HashSet<>();
    private int numSamplesCreated = 0;
    private int numSamplesUpdated = 0;
    private int numSamplesAdded = 0;
    private int numSamplesAnnexed = 0;
    private int numSamplesMovedToBlockedFolder = 0;
    private int numSamplesReprocessed = 0;
    
    public SampleHelper(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, String ownerKey, Logger logger) {
        this(entityBean, computeBean, annotationBean, ownerKey, logger, null);
    }

    public SampleHelper(EntityBeanLocal entityBean,
                        ComputeBeanLocal computeBean,
                        AnnotationBeanLocal annotationBean,
                        String ownerKey,
                        Logger logger,
                        ContextLogger contextLogger) {
        super(entityBean, computeBean, ownerKey, logger, contextLogger);
        this.annotationBean = annotationBean;
    }
    
    public Date parse(String dateTimeStr) {
        try {
            return parser.parseDateTime(dateTimeStr).toDate();
        }
        catch (Exception e) {
            logger.error("Cannot parse ISO8601 date: "+dateTimeStr,e);
            return null;
        }
    }

    public String format(Date date) {
        DateTime dt = date==null?null:new DateTime(date);
        return parser.print(dt);
    }
    
    /**
     * Clear all the visited flags on all entities owned by the user.
     */
    public void clearVisited() throws Exception {
        logger.info("Clearing visited flag on all entities for user "+ownerKey);
        annotationBean.deleteAttribute(ownerKey, EntityConstants.ATTRIBUTE_VISITED);
    }
    
    /**
     * Set the visited flag on a given sample entity and clear the desync status if it is set.
     * @param sample sample to mark as visited
     * @return the updated sample entity
     * @throws Exception
     */
    public Entity setVisited(Entity sample) throws Exception {
        if (!EntityUtils.addAttributeAsTag(sample, EntityConstants.ATTRIBUTE_VISITED)) {
            throw new IllegalStateException("Could not set visited flag for "+sample.getName());
        }
        sample = entityBean.saveOrUpdateEntity(sample);
        
        EntityData statusEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
        if (statusEd!=null && EntityConstants.VALUE_DESYNC.equals(statusEd.getValue())) {
            // This sample is no longer desynchronized, so delete its desync status
            sample.getEntityData().remove(statusEd);
            entityBean.deleteEntityData(statusEd);
        }
        
        return sample;
    }

    public void setDataSetNameFilter(String dataSetNameFilter) {
        this.dataSetNameFilter = dataSetNameFilter;
    }

    /**
     * Create a sample or update it if it already exists.
     * @param parentSample target sample to create or update
     * @param slideCode string that denotes the target slide biomaterial
     * @param dataSet researcher set the sample belongs to
     * @param tileGroupList listing of the tiles expected
     * @return returns the new or old target sample
     * @throws Exception
     */
    public Entity createOrUpdateSample(Entity parentSample, String slideCode, Entity dataSet, 
            Collection<SlideImageGroup> tileGroupList) throws Exception {

        logger.info("Creating or updating sample: "+slideCode+" ("+(parentSample==null?"":("parentSample="+parentSample.getName()+", "))+(dataSet==null?"":"dataSet="+dataSet.getName())+")");
        
        Multimap<String,SlideImageGroup> objectiveGroups = HashMultimap.create();
        for(SlideImageGroup tileGroup : tileGroupList) {
            String groupObjective = null;
            for(SlideImage slideImage : tileGroup.getImages()) {
                if (groupObjective==null) {
                    groupObjective = slideImage.getObjective();
                }
                else if (!groupObjective.equals(slideImage.getObjective())) {
                    logger.warn("  No consensus for objective in tile group '"+tileGroup.getTag()+"' ("+groupObjective+" != "+slideImage.getObjective()+")");
                }
            }
            if (groupObjective==null) {
                groupObjective = "";
            }
            objectiveGroups.put(groupObjective, tileGroup);
        }    
        
        logger.debug("  Sample objectives: "+objectiveGroups.keySet());
        Entity sample;
        
        if (objectiveGroups.keySet().size()>1) {
            
            if (parentSample!=null) {
                throw new IllegalStateException("Sample "+slideCode+" has already been subdivided but still contains multiple objectives: "+parentSample.getId());
            }
            
            // There is more than one objective. Create a parent Sample, and then a SubSample for each objective.
            sample = getOrCreateSample(slideCode, dataSet, null, null, tileGroupList, null, false);
            
            List<String> objectives = new ArrayList<>(objectiveGroups.keySet());
            Collections.sort(objectives);
            for(String objective : objectives) {
                Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                createOrUpdateSample(sample, slideCode, null, subTileGroupList);
            }

            putInCorrectDataSetFolder(sample);
        }
        else {
            String objective = objectiveGroups.keySet().iterator().next();
            
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(tileGroupList);
            int sampleNumChannels = sampleNumSignals+1;
            String sampleChannelSpec = ChanSpecUtils.createChanSpec(sampleNumChannels, sampleNumChannels);

            logger.info("  Sample attributes: objective="+objective+", signalChannels="+sampleNumSignals+", chanSpec="+sampleChannelSpec);
            
            // Find the sample, if it exists, or create a new one.
            int prevNumSamplesUpdated = numSamplesUpdated;
            sample = getOrCreateSample(slideCode, dataSet, sampleChannelSpec, objective, tileGroupList, parentSample, true);
            
            if (parentSample!=null && prevNumSamplesUpdated != numSamplesUpdated) {
                // Updated an existing sub-sample, make sure it's converted fully 
                EntityData dsiEd = sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                if (dsiEd!=null) {
                    logger.info("  Converting existing sample into a sub-sample: "+sample.getId());
                    entityBean.deleteEntityData(dsiEd);
                    // Now remove the sub-sample from the data set folder
                    putInCorrectDataSetFolder(sample);
                }
            }
            
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

        logger.debug("  Setting visited flag on sample: "+sample.getName()+" (id="+sample.getId()+")");
        setVisited(sample);
        return sample;
    }
    
    protected Entity getOrCreateSample(String slideCode, Entity dataSet, String channelSpec, String objective, 
            Collection<SlideImageGroup> tileGroupList, Entity parentSample, boolean setLsmAttributes) throws Exception {

        boolean sampleDirty = false;
        boolean tilesDirty = false;
        
        Date maxTmogDate = null;
        
        // Find consensus values in the images which could be represented in the sample.
        Map<String,String> sampleProperties = new HashMap<>();
        for(SlideImageGroup slideImageGroup : tileGroupList) {
            for(SlideImage slideImage : slideImageGroup.getImages()) {
                Map<String,String> imageProps = slideImage.getProperties();
                for(String key : imageProps.keySet()) {
                    String value = imageProps.get(key);
                    // Special consideration is given to the TMOG Date, so that the latest LSM TMOG date is recorded as the Sample TMOG date. 
                    if (key.equals(EntityConstants.ATTRIBUTE_TMOG_DATE)) {
                        Date date = parse(value);
                        if (maxTmogDate==null || date.after(maxTmogDate)) {
                            maxTmogDate = date;
                        }
                    }
                    else {
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
        }
        
        if (maxTmogDate!=null) {
        	sampleProperties.put(EntityConstants.ATTRIBUTE_TMOG_DATE, format(maxTmogDate));
        }
        
        if (!slideCode.equals(sampleProperties.get(EntityConstants.ATTRIBUTE_SLIDE_CODE))) {
            throw new IllegalStateException("Sample consensus attribute Slide Code does not match slide code");
        }
        
        Entity sample = findOrAnnexExistingSample(tileGroupList, dataSet, objective, parentSample, sampleProperties);
        
        if (sample == null) {
            sample = createSample(dataSet, channelSpec, objective, sampleProperties, parentSample);
            synchronizeTiles(sample, tileGroupList, setLsmAttributes);
            numSamplesCreated++;
        }
        else {
            String newName = getSampleName(dataSet, objective, parentSample, sampleProperties);
            if (!sample.getName().equals(newName)) {
                logger.info("  Updating sample name to: "+newName);
                sample.setName(newName);
                sampleDirty = true;
            }
            
            if (setSampleAttributes(sample, dataSet, channelSpec, objective, sampleProperties)) {
                sampleDirty = true;
            }
            
            if (synchronizeTiles(sample, tileGroupList, setLsmAttributes)) {
                tilesDirty = true;
            }
            
            if (tilesDirty || sampleDirty) {
                if (tilesDirty) {
                    // Only reprocess the sample if the LSMs have changed
                    markForReprocessing(sample);
                }
                logger.debug("Updating sample "+sample.getName());
                sample = entityBean.saveOrUpdateEntity(sample);
                numSamplesUpdated++;
            }
            
        }

        return sample;
    }
    
    /**
     * Find the sample with the given LSMs. If it doesn't exist to the owner, then annex it. If it doesn't exist, 
     * try to annex a legacy sample from FlyLight, based on the putative sample identifier.
     */
    public Entity findOrAnnexExistingSample(Collection<SlideImageGroup> tileGroupList, Entity dataSet, String objective, 
            Entity parentSample, Map<String,String> sampleProperties) throws Exception {

        Set<String> tileNames = new HashSet<>();
        Set<String> lsmNames = new HashSet<>();
        for(SlideImageGroup slideImageGroup : tileGroupList) {
            for(SlideImage slideImage : slideImageGroup.getImages()) {
                String lsmName = slideImage.getFile().getName();
                lsmNames.add(lsmName);
                tileNames.add(slideImage.getTileType());
            }
        }
        
        logger.info("  Looking for existing sample with LSM set: "+lsmNames);
        logger.debug("    (With tile names: "+tileNames+")");
        
        Entity matchedUnownedSample = null;
        Entity matchedSample = null;
        Set<Long> visitedSamples = new HashSet<>();
        
        // Check if the any sample in the database has an LSM set that matches the current LSM set.
        // We can start at any of the LSMs, because they all need to be present.
        for(Entity lsm : entityBean.getEntitiesByName(lsmNames.iterator().next())) {
            Entity sample = entityBean.getAncestorWithType(null, lsm.getId(), EntityConstants.TYPE_SAMPLE);
            if (sample!=null) {
                if (visitedSamples.contains(sample.getId())) {
                    continue;
                }
                visitedSamples.add(sample.getId());
                
                Set<String> matchedLsmNames = new HashSet<>();
                Set<String> matchedTileNames = new HashSet<>();
                
                entityLoader.populateChildren(sample);
                Entity supportingData = EntityUtils.getSupportingData(sample);
                if (supportingData != null) {
                    entityLoader.populateChildren(supportingData);
                    for(Entity imageTile : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_TILE)) {
                        entityLoader.populateChildren(imageTile);
                        for(Entity siblingLsm : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                            matchedTileNames.add(imageTile.getName());
                            matchedLsmNames.add(siblingLsm.getName());
                        }
                    }
                }
                
                if (matchedLsmNames.equals(lsmNames)) {
                    if (sample.getOwnerKey().equals(ownerKey)) {
                        logger.info("  Found sample with matching LSM set: "+sample.getName());
                        matchedSample = sample;    
                        break;
                    }
                    else {
                        logger.info("  Found sample with matching LSM set, but it is not owned by us, so we'll keep looking: "+sample.getName());
                        matchedUnownedSample = sample;
                    }
                }
                else {
                    logger.debug("  Sample "+sample.getName()+" does not match: "+matchedLsmNames);
                    logger.debug("    (With tile names: "+matchedTileNames+")");
                }
            }
        }

        
        // Only use the unowned sample if we didn't find any owned samples
        if (matchedSample==null) {
            matchedSample = matchedUnownedSample;
        }
    
        // Found matching sample
        if (matchedSample != null) {
            final String matchedOwnerKey = matchedSample.getOwnerKey();
            if ((matchedOwnerKey == null) || (! matchedOwnerKey.equals(ownerKey))) {
                // Need to annex the sample if possible
                if ("group:flylight".equals(ownerKey)) {
                    // FlyLight cannot steal samples from others
                    logger.warn("  Found matching sample, but FlyLight cannot steal data from others.");
                    matchedSample = null;
                } 
                else {
                    // Annex it later, so we don't hold the connection open for too long
                    logger.warn("  Found matching sample owned by "+matchedSample.getOwnerKey()+". We will annex it later.");
                    samplesToAnnex.add(matchedSample.getId());
                }
            }
        }

        return matchedSample;

    }

    /**
     * Annex all the samples that were tracked as needing annexing, during processing with findOrAnnexExistingSample().
     * @throws Exception
     */
    public void annexSamples() throws Exception {
        if (samplesToAnnex.isEmpty()) return;
        logger.info("Will annexing "+samplesToAnnex.size()+" samples");
        for(Long entityId : new ArrayList<>(samplesToAnnex)) {
            try {
                samplesToAnnex.remove(entityId);
                entityBean.annexEntityTree(ownerKey, entityId);
                numSamplesAnnexed++;
            }
            catch (Exception e) {
                logger.error("Error annexing sample: "+entityId, e);
            }
        }
    }
    
    /**
     * Create a sample with the given name and set the sample attributes. 
     * @param dataSet data set sample will belong to
     * @param channelSpec channel specification for this sample's images
     * @param objective objective used for the scans
     * @return returns a new sample entity
     * @throws Exception
     */
    public Entity createSample(Entity dataSet, String channelSpec, String objective, Map<String,String> sampleProperties, Entity parentSample) throws Exception {

        String name = getSampleName(dataSet, objective, parentSample, sampleProperties);
        logger.info("  Creating new sample: "+name);
        Date createDate = new Date();
        Entity sample = new Entity();
        sample.setOwnerKey(ownerKey);
        sample.setEntityTypeName(EntityConstants.TYPE_SAMPLE);
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        
        setSampleAttributes(sample, dataSet, channelSpec, objective, sampleProperties);
        sample = entityBean.saveOrUpdateEntity(sample);
        
        return sample;
    }

    /**
     * Create a new name for a sample, given the sample's attributes.
     * {Line}-{Slide Code}-Right_Optic_Lobe
     * {Line}-{Slide Code}-Left_Optic_Lobe
     * {VT line|Line}-{Slide Code}-Left_Optic_Lobe
     * {Line}-{Effector}-{Age}
     */
    public String getSampleName(Entity dataSet, String objective, Entity parentSample, Map<String,String> sampleProperties) {

        if (dataSet==null) {
            if (parentSample==null) {
                logger.warn("    Data set and parent sample are both null!");
            }
            else {
                String dataSetIdentifier = parentSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
                dataSet = dataSetEntityByIdentifier.get(dataSetIdentifier);
            }
        }
        
        String sampleNamePattern = dataSet==null?DEFAULT_SAMPLE_NAME_PATTERN:dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_SAMPLE_NAME_PATTERN);

        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(sampleNamePattern);
        StringBuffer buffer = new StringBuffer();
        logger.trace("    Building sample name:");
        while (matcher.find()) {
            String tmpGroup = matcher.group(1);
            String[] replacementPieces = tmpGroup.split("\\|");
            String replacement=null;
            for (String tmpPiece:replacementPieces) {
                replacement = sampleProperties.get(tmpPiece.trim());
                if (replacement != null) {
                    matcher.appendReplacement(buffer, "");
                    buffer.append(replacement);
                    break;
                }
                logger.trace("        " + replacement+" -> "+buffer);
            }
            if (null==replacement) {
                logger.warn("Cannot find a property replacement for Sample Naming Pattern element " + tmpGroup);
            }
        }
        matcher.appendTail(buffer);
        logger.trace("        append tail -> "+buffer);
        if (parentSample!=null && !StringUtils.isEmpty(objective)) {
            buffer.append("~").append(objective);
            logger.trace("        append subsample -> "+buffer);
        }
        return buffer.toString();
    }

    /**
     * Set the sample attributes.
     * @param sample the Sample entity
     * @param dataSet the data set (e.g. "flylight_whole_brain")
     * @param channelSpec the channel specification for the secondary data (not the input stacks) in the sample (e.g. "sssr")
     * @param objective the objective, if the sample is specific to a single objective (e.g. "63x")
     * @return returns sample entity with the attributes set
     * @throws Exception
     */
    public boolean setSampleAttributes(Entity sample, Entity dataSet, String channelSpec, String objective, Map<String,String> sampleProperties) throws Exception {
        
        boolean dirty = false;
        
        String dataSetIdentifier = dataSet==null?null:dataSet.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        logger.debug("    Setting sample properties: "+sampleProperties);
        
        // Find out which attributes a Sample can support. We only want to set those. 
        Set<String> attrs = new HashSet<>();
        for(EntityAttribute attr : entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE).getAttributes()) {
            attrs.add(attr.getName());
        }
        
        // Set all the properties that we can. 
        for(String key : sampleProperties.keySet()) {
            if (explicitSampleAttrs.contains(key)) {
                continue;
            }
            String value = sampleProperties.get(key);
            if (attrs.contains(key) && value!=null && !NO_CONSENSUS_VALUE.equals(value)) {
                if (EntityUtils.setAttribute(sample, key, value)) {
                    dirty = true;
                }
            }
        }

        // Some attributes are known explicitly. These should all be listed in explicitSampleAttrs, so they're not set above.
        if (EntityUtils.setAttribute(sample, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
            dirty = true;
        }   
        if (EntityUtils.setAttribute(sample, EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, channelSpec)) {
            dirty = true;
        }  
        if (EntityUtils.setAttribute(sample, EntityConstants.ATTRIBUTE_OBJECTIVE, objective)) {
            dirty = true;
        }
        
        return dirty;
    }    

    /**
     * Given a collection of SlideImage groups, this method walks through each one and adds it to the sample, or
     * updates the existing structure to match. Any image tiles in the sample which are not in the given list are 
     * deleted.
     * @param sample sample to synchronize image tiles to
     * @param tileGroupList tiles to sync with to the sample
     * @param setLsmAttributes boolean to decide whether to set the LSM attributes as well
     * @throws Exception
     */
    public boolean synchronizeTiles(Entity sample, Collection<SlideImageGroup> tileGroupList, boolean setLsmAttributes) throws Exception {
        
        boolean dirty = false;
        
        Set<String> tileNameSet = new HashSet<>();
        for (SlideImageGroup tileGroup : tileGroupList) {
            if (addTileToSample(sample, tileGroup, setLsmAttributes)) {
                dirty = true;
            }
            tileNameSet.add(tileGroup.getTag());
        }

        entityLoader.populateChildren(sample);
        Entity supportingFolder = EntityUtils.getSupportingData(sample);
        for(Entity imageTile : EntityUtils.getChildrenOfType(supportingFolder, EntityConstants.TYPE_IMAGE_TILE)) {
            boolean matchFound = false;
            for(SlideImageGroup tileGroup : tileGroupList) {
                if (existingTileMatches(imageTile, tileGroup)) {
                    matchFound = true;
                }
            }
            if (!matchFound) {
                logger.info("  Removing superfluous image tile: "+imageTile.getName());
                entityBean.deleteEntityTreeById(imageTile.getOwnerKey(), imageTile.getId());
                dirty = true;
            }
        }

        logger.debug("  Synchronized tiles for '"+sample.getName()+"' (setLsmAttributes="+setLsmAttributes+")");
        return dirty;
    }
    
    /**
     * Add the given group of SlideImages to the specified sample. Any necessary supporting structures will be created
     * or updated, as long as the Sample entity itself exists. if the tile already exists in the sample, it is checked
     * to make sure the slide images are the same. If they are the same then the LSM Stack attributes are just updated.
     * If they differ then the existing tile is deleted and recreated with the correct slide images.
     *
     * @param sample sample to synchronize image tiles to
     * @param tileGroup specific tile group to sync with to the sample
     * @param setLsmAttributes boolean to decide whether to set the LSM attributes as well
     * @return true if the tile has changed
     * @throws Exception
     */
    public boolean addTileToSample(Entity sample, SlideImageGroup tileGroup, boolean setLsmAttributes) throws Exception {
        
        boolean dirty = false;
        
        // Get the existing Supporting Files, or create a new one
        Entity supportingFiles = EntityUtils.getSupportingData(sample);
        
        if (supportingFiles == null) {
            supportingFiles = createSupportingFilesFolder();
            addToParent(sample, supportingFiles, 0, EntityConstants.ATTRIBUTE_SUPPORTING_FILES);
        }
        else {
            // Reload the whole thing in case we've already modified it
            supportingFiles = entityBean.getEntityTree(supportingFiles.getId());
            // Set it back to the sample
            EntityData ed = EntityUtils.findChildEntityDataWithType(sample, EntityConstants.TYPE_SUPPORTING_DATA);
            ed.setChildEntity(supportingFiles);
        }
        
        // Find the tile
        EntityData imageTileEd = null;
        for (EntityData ed : supportingFiles.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child!=null && child.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_TILE)) {
                if (child.getName().equals(tileGroup.getTag())) {
                    String area = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                    if (area==null || area.equals(tileGroup.getAnatomicalArea())) {
                        imageTileEd = ed;
                        break;
                    }
                }
            }
        }
        
        Entity imageTile = null;
        if (imageTileEd != null) {
            imageTile = imageTileEd.getChildEntity();
            if (!existingTileMatches(imageTile, tileGroup)) {
                logger.info("  Tile '"+imageTile.getName()+"' (id="+imageTileEd.getId()+") has changed, will delete and recreate it.");
                entityBean.deleteEntityTreeById(imageTile.getOwnerKey(), imageTile.getId());
                imageTile = null;
                dirty = true;
            }
            else {
                logger.debug("  Tile '"+imageTile.getName()+"' exists (id="+imageTileEd.getId()+")");
                if (setLsmAttributes) {
                    for(Entity lsmStack : EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK)) {
                        for(SlideImage image : tileGroup.getImages()) {
                            if (image.getFile().getName().equals(lsmStack.getName())) {
                                if (setLsmStackAttributes(lsmStack, image)) {
                                    dirty = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (imageTile == null) {
            imageTile = createTile(tileGroup, true);
            addToParent(supportingFiles, imageTile, null, EntityConstants.ATTRIBUTE_ENTITY);
            for(SlideImage image : tileGroup.getImages()) {
                logger.info("    Adding LSM file to sample: "+image.getFile().getName());
                Entity lsmEntity = createLsmStackFromFile(image, setLsmAttributes);
                addToParent(imageTile, lsmEntity, imageTile.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
            }
            dirty = true;
        }
        else {
            if (setTileAttributes(imageTile, tileGroup)) {
                imageTile = entityBean.saveOrUpdateEntity(imageTile);
                dirty = true;
            }
        }
        
        return dirty;
    }

    /**
     * 
     * @param imageTile
     * @param tileGroup
     * @return
     */
    protected boolean existingTileMatches(Entity imageTile, SlideImageGroup tileGroup) {

        List<SlideImage> newImages = tileGroup.getImages();
        List<Entity> currImages = EntityUtils.getChildrenOfType(imageTile, EntityConstants.TYPE_LSM_STACK);
        
        Set<String> newFilenames = new HashSet<>();
        for(SlideImage image : newImages) {
            newFilenames.add(image.getFile().getName());
        }
        
        if (newImages.size() != currImages.size()) {
            return false;
        }
        
        Set<String> currFilenames = new HashSet<>();
        for(Entity lsmStack : currImages) {
            currFilenames.add(lsmStack.getName());
            if (!newFilenames.contains(lsmStack.getName())) {
                return false;
            }
        }
        
        for(SlideImage image : newImages) {
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
        filesFolder.setEntityTypeName(EntityConstants.TYPE_SUPPORTING_DATA);
        filesFolder.setCreationDate(createDate);
        filesFolder.setUpdatedDate(createDate);
        filesFolder.setName("Supporting Files");
        filesFolder = entityBean.saveOrUpdateEntity(filesFolder);
        logger.info("  Saved supporting files folder as "+filesFolder.getId());
        return filesFolder;
    }

    /**
     * Create and return an LSM Stack entity based on the given SAGE image data.
     * @return
     * @throws Exception
     */
    public Entity createTile(SlideImageGroup tileGroup, boolean setTileAttributes) throws Exception {
        Date createDate = new Date();
        Entity imageTile = new Entity();
        imageTile.setOwnerKey(ownerKey);
        imageTile.setEntityTypeName(EntityConstants.TYPE_IMAGE_TILE);
        imageTile.setCreationDate(createDate);
        imageTile.setUpdatedDate(createDate);
        if (setTileAttributes) {
            setTileAttributes(imageTile, tileGroup);
        }
        imageTile = entityBean.saveOrUpdateEntity(imageTile);
        logger.info("  Saved image tile '"+imageTile.getName()+"' as "+imageTile.getId());
        return imageTile;
    }
    
    /**
     * Set the tile attributes from the given SAGE image data.
     * @return true if something changed
     * @throws Exception
     */
    public boolean setTileAttributes(Entity imageTile, SlideImageGroup tileGroup) throws Exception {
        boolean tileDirty = false;
        logger.debug("    Setting tile properties: name="+tileGroup.getTag()+", anatomicalArea="+tileGroup.getAnatomicalArea());
        if (imageTile.getName()==null || !imageTile.getName().equals(tileGroup.getTag())) {
            logger.debug("    Updating tile name for Image Tile#"+imageTile.getId());
            imageTile.setName(tileGroup.getTag());
            tileDirty = true;
        }

        if (EntityUtils.setAttribute(imageTile, EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, tileGroup.getAnatomicalArea())) {
            tileDirty = true;
        }
        
        return tileDirty;
    }
    
    /**
     * Create and return an LSM Stack entity based on the given SAGE image data.
     * @param image
     * @return
     * @throws Exception
     */
    public Entity createLsmStackFromFile(SlideImage image, boolean setLsmAttributes) throws Exception {
        Date createDate = new Date();
        Entity lsmStack = new Entity();
        lsmStack.setOwnerKey(ownerKey);
        lsmStack.setEntityTypeName(EntityConstants.TYPE_LSM_STACK);
        lsmStack.setCreationDate(createDate);
        lsmStack.setUpdatedDate(createDate);
        lsmStack.setName(image.getFile().getName());
        if (setLsmAttributes) {
            if (!setLsmStackAttributes(lsmStack, image)) {
                // If it wasn't saved by setLsmStackAttributes, we have to save it here
                lsmStack = entityBean.saveOrUpdateEntity(lsmStack);    
            }
        }
        else {
            lsmStack = entityBean.saveOrUpdateEntity(lsmStack);    
        }
        
        logger.info("      Saved LSM stack as "+lsmStack.getId());
        return lsmStack;
    }
    
    /**
     * Set the LSM stack attribuets from the given SAGE image data.
     * @param lsmStack
     * @param image
     * @return true if something changed
     * @throws Exception
     */
    public boolean setLsmStackAttributes(Entity lsmStack, SlideImage image) throws Exception {
        
        boolean lsmDirty = false;
        boolean dirty = false;
        Map<String,String> imageProperties = image.getProperties();
        logger.debug("    Setting LSM stack properties:"+imageProperties);
        
        // Find out which attributes a Sample can support. We only want to set those. 
        Set<String> attrs = new HashSet<>();
        for(EntityAttribute attr : entityBean.getEntityTypeByName(EntityConstants.TYPE_LSM_STACK).getAttributes()) {
            attrs.add(attr.getName());
        }
        
        // Set all the properties that we can. 
        for(String key : imageProperties.keySet()) {
            String value = imageProperties.get(key);
            if (value!=null) {
                if (!attrs.contains(key)) {
                    logger.warn("LSM stack does not support property: "+key);
                }
                else {
                    if (EntityUtils.setAttribute(lsmStack, key, value)) {
                        if (key.equals(EntityConstants.ATTRIBUTE_TMOG_DATE)) {
                            // TODO: we can remove this once all LSMs have a TMOG date on them. 
                            // In the meantime, we don't want to trigger reprocessing on all samples so we use a separate dirty flag to save the LSM silently. 
                            lsmDirty = true;
                        }
                        else {
                            lsmDirty = true;
                            dirty = true;
                        }
                    }
                }
            }
        }

        if (lsmDirty) {
            lsmStack = entityBean.saveOrUpdateEntity(lsmStack);
        }
        
        return dirty;
    }
    
    
    private void markForReprocessing(Entity sample) {
        String sampleStatus = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
        if (sampleStatus!=null) {
            // Certain statuses mean that a sample should not be reprocessed
            if (sampleStatus.equals(EntityConstants.VALUE_BLOCKED) || sampleStatus.equals(EntityConstants.VALUE_RETIRED)) {
                return;
            }
        }
        logger.info("  Sample tiles changed, marking for reprocessing: "+sample.getName());
        EntityUtils.setAttribute(sample, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED);
        numSamplesReprocessed++;
    }
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsmEntity
     * @return
     */
    public String getLSMChannelSpec(Entity lsmEntity, int refIndex) {
        
        String chanSpec = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (!StringUtils.isEmpty(chanSpec)) {
            return chanSpec;
        }
        
        String numChannelsStr = lsmEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        if (!StringUtils.isEmpty(numChannelsStr)) {
            try {
            	return ChanSpecUtils.createChanSpec(Integer.parseInt(numChannelsStr), refIndex+1);    
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
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(AnatomicalArea sampleArea, String attrName) throws Exception {
        List<AnatomicalArea> sampleAreas = new ArrayList<>();
        sampleAreas.add(sampleArea);
        return getConsensusLsmAttributeValue(sampleAreas, attrName);
    }

    /**
     * Go through a set of sample areas' LSM supporting files and look for an entity attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(List<AnatomicalArea> sampleAreas, String attrName) throws Exception {
        String consensus = null;
        logger.trace("Determining consensus for "+attrName+" for sample areas: "+getSampleAreasCSV(sampleAreas));
        for(AnatomicalArea sampleArea : sampleAreas) {
        	logger.trace("  Determining consensus for "+attrName+" in "+sampleArea.getName()+" sample area");
			List<Entity> tileEntities = entityBean.getEntitiesById(sampleArea.getTileIds());
	        for(Entity tile : tileEntities) {
	        	logger.trace("    Determining consensus for "+attrName+" in "+tile.getName()+" tile");
	        	entityLoader.populateChildren(tile);
	            for(Entity image : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
		        	logger.trace("      Determining consensus for "+attrName+" in "+image.getName()+" LSM");
	                String value = image.getValueByAttributeName(attrName);
	                if (consensus!=null && !consensus.equals(value)) {
	                    logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample processing result "+sampleArea.getSampleProcessingResultId());
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
    
    private String getSampleAreasCSV(List<AnatomicalArea> sampleAreas) {
    	StringBuilder sb = new StringBuilder();
    	for(AnatomicalArea sampleArea : sampleAreas) {
    		if (sb.length()>0) sb.append(",");
    		sb.append(sampleArea.getName());
    	}
    	return sb.toString();
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
        String sampleStatus = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
        
        boolean blocked = EntityConstants.VALUE_BLOCKED.equals(sampleStatus);
        if (blocked) {
            logger.debug("  Ensuring blocked sample "+sample.getName()+" is in Blocked Data folder");   
        }
        else {
            logger.debug("  Ensuring sample "+sample.getName()+" is in "+sampleDataSetIdentifier+" folder");       
        }
        
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
                if (dataSetIdentifier.equals(sampleDataSetIdentifier) && !blocked) {
                    if (ed==null) {
                        logger.info("    Adding to data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");   
                        addToParent(dataSetFolder, sample, dataSetFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
                        numSamplesAdded++;
                    }
                    else {
                        logger.trace("    Already in data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");
                    }
                }
                else {
                    if (ed!=null) {
                        logger.info("    Removing from data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");   
                        entityBean.deleteEntityData(ed);
                    }
                    else {
                        logger.trace("    Already missing from data set folder: "+dataSetFolder.getName()+" (id="+dataSetFolder.getId()+")");
                    }
                }
            }
        }
        // Check the blocked data folder
        Entity blockedFolder = getBlockedDataFolder();
        EntityData blockedEd = EntityUtils.findChildEntityDataWithChildId(blockedFolder, sample.getId());
        if (blocked) {
            if (blockedEd==null) {
                logger.info("    Adding to blocked data folder");  
                addToParent(blockedFolder, sample, blockedFolder.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
                numSamplesMovedToBlockedFolder++;
            }
            else {
                logger.trace("    Already in blocked data folder");
            }
        }
        else {
            if (blockedEd!=null) {
                logger.info("    Removing from blocked data folder");
                entityBean.deleteEntityData(blockedEd);
            }
            else {
                logger.trace("    Already missing from blocked data folder");
            }
        }
        // Check the retired data folder, and remove this sample if it's in there
        Entity retiredFolder = getRetiredDataFolder();
        EntityData retiredEd = EntityUtils.findChildEntityDataWithChildId(retiredFolder, sample.getId());
        if (retiredEd!=null) {
            logger.info("    Removing from retired data folder");
            entityBean.deleteEntityData(retiredEd);
            if (sample.getName().contains("-Retired")) {
                sample.setName(sample.getName().replaceAll("-Retired", ""));
                entityBean.saveOrUpdateEntity(sample);
            }
        }
        else {
            logger.trace("    Already missing from retired data folder");
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

    public Entity getRetiredDataFolder() throws Exception {
        if (retiredDataFolder==null) {
            loadRetiredDataFolder();
        }
        return retiredDataFolder;
    }
    
    public Entity getBlockedDataFolder() throws Exception {
        if (blockedDataFolder==null) {
            loadBlockedDataFolder();
        }
        return blockedDataFolder;
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

    public int getNumSamplesAnnexed() {
        return numSamplesAnnexed;
    }
    
    public int getNumSamplesMovedToBlockedFolder() {
        return numSamplesMovedToBlockedFolder;
    }
    
    public int getNumSamplesReprocessed() {
        return numSamplesReprocessed;
    }

    public Map<String, Entity> getDataSetFolderByIdentifierMap() throws Exception {
        if (dataSetFolderByIdentifier==null) {
            loadDataSets();
        }
        return dataSetFolderByIdentifier;
    }

    public Map<String, Entity> getDataSetEntityByIdentifierMap() throws Exception {
        if (dataSetEntityByIdentifier==null) {
            loadDataSets();
        }
        return dataSetEntityByIdentifier;
    }

    private void loadDataSets() throws Exception {
        
        if (dataSets!=null) return;
        loadTopLevelFolder();
        
        this.dataSetFolderByIdentifier = new HashMap<>();
        this.dataSetEntityByIdentifier = new HashMap<>();
        this.dataSets = new ArrayList<>(entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_DATA_SET));

        if (dataSetNameFilter != null) {
            List<Entity> filteredDataSets = new ArrayList<>();
            for (Entity dataSet : dataSets) {
                if (dataSetNameFilter.equals(dataSet.getName())) {
                    filteredDataSets.add(dataSet);
                    break;
                }
            }
            dataSets = filteredDataSets;
        }

        if (dataSets.isEmpty()) {
            logger.info("No data sets found for user: "+ownerKey);
            return;
        }
        
        Collections.sort(dataSets, new Comparator<Entity>() {
			@Override
			public int compare(Entity o1, Entity o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

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
        if (topLevelFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(topLevelFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            entityBean.saveOrUpdateEntity(topLevelFolder);
        }
    }
    
    private void loadRetiredDataFolder() throws Exception {
        if (retiredDataFolder!=null) return;
        logger.info("Getting retired data folder...");
        this.retiredDataFolder = createOrVerifyRootEntity(EntityConstants.NAME_RETIRED_DATA, true, false);
        if (retiredDataFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(retiredDataFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            entityBean.saveOrUpdateEntity(retiredDataFolder);
        }
    }

    private void loadBlockedDataFolder() throws Exception {
        if (blockedDataFolder!=null) return;
        logger.info("Getting blocked data folder...");
        this.blockedDataFolder = createOrVerifyRootEntity(EntityConstants.NAME_BLOCKED_DATA, true, false);
        if (blockedDataFolder.getValueByAttributeName(EntityConstants.ATTRIBUTE_IS_PROTECTED)==null) {
            EntityUtils.addAttributeAsTag(blockedDataFolder, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            entityBean.saveOrUpdateEntity(blockedDataFolder);
        }
    }
}
