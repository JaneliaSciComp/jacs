package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
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

    protected static final String PRIVATE_DATA_SET_FOLDER_NAME = "My Data Sets";
    protected static final String PUBLIC_DATA_SET_FOLDER_NAME = "Public Data Sets";
    
    protected AnnotationBeanLocal annotationBean;
    
    private Entity topLevelFolder;
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
     * 
     * @param parentSample
     * @param sampleIdentifier
     * @param dataSetIdentifier
     * @param tileGroupList
     * @return
     * @throws Exception
     */
    public Entity createOrUpdateSample(Entity parentSample, String sampleIdentifier, String dataSetIdentifier, 
            Collection<SlideImageGroup> tileGroupList) throws Exception {

        logger.info("Creating or updating sample: "+sampleIdentifier+" ("+(parentSample==null?"":("parentSample="+parentSample.getName()+", "))+"dataSet="+dataSetIdentifier+")");
        
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
        
        logger.info("  Sample objectives: "+objectiveGroups.keySet());
        Entity sample = null;
        
        if (objectiveGroups.isEmpty()) {
            throw new IllegalStateException("Sample "+sampleIdentifier+" has no objective groups");
        }
        else if (objectiveGroups.keySet().size()>1) {
            
            if (parentSample!=null) {
                throw new IllegalStateException("Sample "+sampleIdentifier+" has already been subdivided but still contains multiple objectives: "+parentSample.getId());
            }
            
            // There is more than one objective. Create a parent Sample, and then a SubSample for each objective.
            sample = getOrCreateSample(sampleIdentifier, null, dataSetIdentifier, null, tileGroupList);
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
                sample = getOrCreateSample(sampleIdentifier, null, dataSetIdentifier, null, tileGroupList);
            }
            
            List<String> objectives = new ArrayList<String>(objectiveGroups.keySet());
            Collections.sort(objectives);
            for(String objective : objectives) {
                Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                createOrUpdateSample(sample, sampleIdentifier+"~"+objective, null, subTileGroupList);
            }

            putInCorrectDataSetFolder(sample);
        }
        else {
            String objective = objectiveGroups.keySet().iterator().next();
            
            logger.info("  Sample has a single objective group: "+objective);
            
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(tileGroupList);
            String sampleChannelSpec = getDefaultChanSpec(sampleNumSignals);
            logger.debug("  Sample has "+sampleNumSignals+" signal channels, and thus specification '"+sampleChannelSpec+"'");
            
            // Find the sample, if it exists, or create a new one.
            sample = getOrCreateSample(sampleIdentifier, sampleChannelSpec, dataSetIdentifier, objective, tileGroupList);
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
    
    protected Entity getOrCreateSample(String sampleIdentifier, String channelSpec, String dataSetIdentifier, String objective, Collection<SlideImageGroup> tileGroupList) throws Exception {

        Entity sample = findExistingSample(sampleIdentifier);
        if (sample == null) {
            
            // Check if the same exists already with an old-style tagged name
            if (tileGroupList.size()==1) {
                String tag = tileGroupList.iterator().next().getTag();
                String tagSampleIdentifier = sampleIdentifier+"-"+tag.replace(" ", "_");
                sample = findExistingSample(tagSampleIdentifier);
            }
            
            // Still null?
            if (sample == null) {
                logger.info("  Creating new sample with identifier: "+sampleIdentifier);
                sample = createSample(sampleIdentifier, channelSpec, dataSetIdentifier, objective);
                numSamplesCreated++;   
                return sample;
            }
        }
        
        logger.info("  Found existing sample with identifier: "+sampleIdentifier);
        setSampleAttributes(sample, channelSpec, dataSetIdentifier, objective);
        entityLoader.populateChildren(sample);
        numSamplesUpdated++;
        return sample;
    }
    
    protected Entity findExistingSample(String sampleIdentifier) throws ComputeException {

        List<Entity> matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(ownerKey, 
                sampleIdentifier, EntityConstants.TYPE_SAMPLE);
        
        if (matchingSamples.isEmpty()) {

            if ("group:flylight".equals(ownerKey)) {
                // FlyLight cannot steal samples from others
                return null;
            }
            
            matchingSamples = entityBean.getUserEntitiesByNameAndTypeName(null, 
                    sampleIdentifier, EntityConstants.TYPE_SAMPLE);
            
            if (matchingSamples.isEmpty()) {
                // Could not find sample child entity
                return null;
            }

            Entity matchingSample = null;
            for(Entity sample : matchingSamples) {
                if ("group:flylight".equals(sample.getOwnerKey())) {
                    // Can only steal samples from the FlyLight user
                    matchingSample = sample;
                }
            }
            
            if (matchingSample != null) {
                return entityBean.annexEntityTree(ownerKey, matchingSample.getId());    
            }
        }
        
        if (matchingSamples.size()>1) {
            logger.warn("  Multiple samples (count="+matchingSamples.size()+") found with sample identifier: "+sampleIdentifier);
        }
        
        return matchingSamples.get(0);
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
    public Entity createSample(String name, String channelSpec, String dataSetIdentifier, String objective) throws Exception {
        logger.info("  Creating sample "+name);
        Date createDate = new Date();
        Entity sample = new Entity();
        sample.setOwnerKey(ownerKey);
        sample.setEntityType(entityBean.getEntityTypeByName(EntityConstants.TYPE_SAMPLE));
        sample.setCreationDate(createDate);
        sample.setUpdatedDate(createDate);
        sample.setName(name);
        setSampleAttributes(sample, channelSpec, dataSetIdentifier, objective);
        return sample;
    }

    /**
     * Set the sample attributes.
     * @param sample the Sample entity
     * @param channelSpec the channel specification for the secondary data (not the input stacks) in the sample (e.g. "sssr")
     * @param dataSetIdentifier the data set identifier (e.g. "flylight_whole_brain")
     * @param objective the objective, if the sample is specific to a single objective (e.g. "63x")
     * @return
     * @throws Exception
     */
    public Entity setSampleAttributes(Entity sample, String channelSpec, String dataSetIdentifier, String objective) throws Exception {
        logger.info("    Setting properties: dataSet="+dataSetIdentifier+", spec="+channelSpec+", objective="+objective);
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
                logger.info("  Adding LSM file to sample: "+image.getImagePath());
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
        logger.info("  Saved LSM stack as "+lsmStack.getId());
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
        logger.info("    Setting LSM stack properties: channelSpec=" + image.getChannelSpec() + 
                ", numChannels=" + image.getChannels()+ ", objective=" + image.getObjective() + 
                ", opticalRes=" + image.getOpticalRes() + ", gender=" + image.getGender() + ", area=" + image.getArea()
                + ", mountingProtocol=" + image.getMountingProtocol());
        
        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, image.getImagePath());
        if (image.getChannels()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS, image.getChannels());
        }
        if (image.getOpticalRes()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, image.getOpticalRes());   
        }
        if (image.getChannelSpec()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, image.getChannelSpec());
        }
        if (image.getObjective()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE, image.getObjective());
        }
        if (image.getMountingProtocol()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_MOUNTING_PROTOCOL, image.getMountingProtocol());
        }
        if (image.getGender()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_GENDER, image.getGender());
        }
        if (image.getArea()!=null) {
            lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA, image.getArea());
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
        for(int i=0; i<channelSpec.length(); i++) {
            if (channelSpec.charAt(i) == channelCode) {
                if (builder.length()>0) builder.append(" ");
                builder.append(""+i);
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

    public int getNumSamplesCreated() {
        return numSamplesCreated;
    }

    public int getNumSamplesUpdated() {
        return numSamplesUpdated;
    }

    public int getNumSamplesAdded() {
        return numSamplesAdded;
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
        if ("group:flylight".equals(ownerKey)) {
            this.topLevelFolder = createOrVerifyRootEntity(PUBLIC_DATA_SET_FOLDER_NAME, true, false);
        }
        else {
            this.topLevelFolder = createOrVerifyRootEntity(PRIVATE_DATA_SET_FOLDER_NAME, true, false);
        }
    }
}
