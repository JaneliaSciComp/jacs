package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.service.entity.EntityHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Helper methods for dealing with Samples.
 * 
 * @deprecated Use SampleHelperNG instead 
 */
public class SampleHelper extends EntityHelper {
    
    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Clear all the visited flags on all entities owned by the user.
     */
    public void clearVisited() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Set the visited flag on a given sample entity and clear the desync status if it is set.
     * @param sample sample to mark as visited
     * @return the updated sample entity
     * @throws Exception
     */
    public Entity setVisited(Entity sample) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public void setDataSetNameFilter(String dataSetNameFilter) {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Find the sample with the given LSMs. If it doesn't exist to the owner, then annex it. If it doesn't exist, 
     * try to annex a legacy sample from FlyLight, based on the putative sample identifier.
     */
    public Entity findOrAnnexExistingSample(Collection<SlideImageGroup> tileGroupList, Entity dataSet, String objective, 
            Entity parentSample, Map<String,String> sampleProperties) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");

    }

    /**
     * Annex all the samples that were tracked as needing annexing, during processing with findOrAnnexExistingSample().
     * @throws Exception
     */
    public void annexSamples() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Create a new name for a sample, given the sample's attributes.
     * {Line}-{Slide Code}-Right_Optic_Lobe
     * {Line}-{Slide Code}-Left_Optic_Lobe
     * {VT line|Line}-{Slide Code}-Left_Optic_Lobe
     * {Line}-{Effector}-{Age}
     */
    public String getSampleName(Entity dataSet, String objective, Entity parentSample, Map<String,String> sampleProperties) {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
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
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Create and return a supporting files folder. 
     * @return
     * @throws Exception
     */
    public Entity createSupportingFilesFolder() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Create and return an LSM Stack entity based on the given SAGE image data.
     * @return
     * @throws Exception
     */
    public Entity createTile(SlideImageGroup tileGroup, boolean setTileAttributes) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Set the tile attributes from the given SAGE image data.
     * @return true if something changed
     * @throws Exception
     */
    public boolean setTileAttributes(Entity imageTile, SlideImageGroup tileGroup) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Create and return an LSM Stack entity based on the given SAGE image data.
     * @param image
     * @return
     * @throws Exception
     */
    public Entity createLsmStackFromFile(SlideImage image, boolean setLsmAttributes) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Set the LSM stack attribuets from the given SAGE image data.
     * @param lsmStack
     * @param image
     * @return true if something changed
     * @throws Exception
     */
    public boolean setLsmStackAttributes(Entity lsmStack, SlideImage image) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsmEntity
     * @return
     */
    public String getLSMChannelSpec(Entity lsmEntity, int refIndex) {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Calculate and return the number of signal channels across all the SlideImages in a tile group (and thus, the 
     * number of signal channels in the eventual merged tile that results from this group).
     * @param tileGroupList
     * @return
     */
    public int getNumSignalChannels(Collection<SlideImageGroup> tileGroupList) {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Go through a sample area's LSM supporting files and look for an entity attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(AnatomicalArea sampleArea, String attrName) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Go through a set of sample areas' LSM supporting files and look for an entity attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(List<AnatomicalArea> sampleAreas, String attrName) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    /**
     * Remove the given sample from any incorrect data set folders and add it to the correct data set folder, based on
     * its data set identifier attribute.
     * @param sample
     * @throws Exception
     */
    public void putInCorrectDataSetFolder(Entity sample) throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Return the data sets for the configured owner.
     * @return
     * @throws Exception
     */
    public Collection<Entity> getDataSets() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    /**
     * Return the top level data set folder for the configured owner.
     * @return
     * @throws Exception
     */
    public Entity getTopLevelDataSetFolder() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public Entity getRetiredDataFolder() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    public Entity getBlockedDataFolder() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    public int getNumSamplesCreated() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public int getNumSamplesUpdated() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public int getNumSamplesAdded() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public int getNumSamplesAnnexed() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    public int getNumSamplesMovedToBlockedFolder() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
    
    public int getNumSamplesReprocessed() {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public Map<String, Entity> getDataSetFolderByIdentifierMap() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }

    public Map<String, Entity> getDataSetEntityByIdentifierMap() throws Exception {
        throw new UnsupportedOperationException("This class is no longer supported. Use SampleHelperNG instead.");
    }
}
