package org.janelia.it.jacs.compute.service.domain;

import java.io.File;
import java.lang.reflect.Field;
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
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineError;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.reflections.ReflectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelperNG extends EntityHelperNG {

    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";
    
    private List<DataSet> dataSets;
    private String dataSetNameFilter;
    private Map<String,DataSet> dataSetByIdentifier;
    private int numSamplesCreated = 0;
    private int numSamplesUpdated = 0;
    private int numSamplesAdded = 0;
    private int numSamplesMovedToBlockedFolder = 0;
    private int numSamplesReprocessed = 0;
    
    public SampleHelperNG(ComputeBeanRemote computeBean, String ownerKey, Logger logger) {
        this(computeBean, ownerKey, logger, null);
    }

    public SampleHelperNG(ComputeBeanRemote computeBean, String ownerKey, Logger logger, ContextLogger contextLogger) {
        super(computeBean, ownerKey, logger, contextLogger);
    }

    /**
     * Clear all the visited flags on all entities owned by the user.
     */
    public void clearVisited() throws Exception {
        logger.info("Clearing visited flag on all entities for user "+ownerKey);
        domainDao.deleteProperty(ownerKey, Sample.class, "visited");
    }
    
    public void setDataSetNameFilter(String dataSetNameFilter) {
        this.dataSetNameFilter = dataSetNameFilter;
    }

    public LSMImage createOrUpdateLSM(SlideImage slideImage) throws Exception {
        LSMImage lsm = domainDao.getLsmBySageId(ownerKey, slideImage.getSageId());
        boolean dirty = false;
        if (lsm==null) {
            lsm = new LSMImage();
            lsm.setFiles(new HashMap<FileType,String>());
            logger.info("Created new LSM for SAGE image#"+slideImage.getSageId()+" with "+lsm.getId());
            dirty = true;
        }
        if (updateLsmAttributes(lsm, slideImage)) {
            logger.info("  Updated LSM properties");
            dirty = true;
        }
        if (dirty) {
            domainDao.save(ownerKey, lsm);
        }
        return lsm;
    }

    private Map<String,LsmSageAttribute> lsmSageAttrs;
    
    private void buildLsmAttributeMap() {
        lsmSageAttrs = new HashMap<>();
        for (Field field : ReflectionUtils.getAllFields(LSMImage.class)) {
            SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
            if (sageAttribute!=null) {
                LsmSageAttribute attr = new LsmSageAttribute();
                attr.cvName = sageAttribute.cvName();
                attr.termName = sageAttribute.termName();
                attr.field = field;
                logger.info("  "+attr.getKey()+" -> LsmImage."+field.getName());
                lsmSageAttrs.put(attr.getKey(), attr);
            }
        }
    }
    
    private Map<String,LsmSageAttribute> getLsmSageAttrs() {
        if (lsmSageAttrs==null) {
            buildLsmAttributeMap();
        }
        return lsmSageAttrs;
    }

    private class LsmSageAttribute {
        String cvName;
        String termName;
        Field field;
        public String getKey() {
            return cvName+"_"+termName;
        }
    }

    private Map<String,DomainObjectAttribute> sampleAttrs;
    
    private void buildSampleAttributeMap() {
        sampleAttrs = new HashMap<>();
        for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(Sample.class)) {
            logger.info("  "+attr.getName()+" -> Sample."+attr.getName());
            sampleAttrs.put(attr.getName(), attr);
        }
    }
    
    private Map<String,DomainObjectAttribute> getSampleAttrs() {
        if (sampleAttrs==null) {
            buildSampleAttributeMap();
        }
        return sampleAttrs;
    }    
    
    private boolean updateLsmAttributes(LSMImage lsm, SlideImage slideImage) throws Exception {

        boolean dirty = false;
        
        if (!StringUtils.areEqual(lsm.getFilepath(), slideImage.getFilepath())) {
            File file = new File(slideImage.getFilepath());
            String name = ArchiveUtils.getDecompressedFilepath(file.getName());
            lsm.setName(name);
            lsm.setFilepath(slideImage.getFilepath());
            lsm.getFiles().put(FileType.LosslessStack,lsm.getFilepath());
            dirty = true;
        }

        if (!StringUtils.areEqual(lsm.getObjective(), slideImage.getObjective())) {
            lsm.setObjective(slideImage.getObjective());
            dirty = true;
        }
        
        Map<String,LsmSageAttribute> lsmSageAttrs = getLsmSageAttrs();
        
        for(String key : slideImage.getProperties().keySet()) {
            try {
                LsmSageAttribute attr = lsmSageAttrs.get(key);
                Object value = slideImage.getProperties().get(key);
                Object trueValue = null;
                if (value!=null) {
                    Class<?> fieldType = attr.field.getType();
                    // Convert the incoming value from SAGE to the correct type in our domain model
                    if (fieldType.equals(String.class)) {
                        trueValue = value.toString();
                    }
                    else if (fieldType.equals(Date.class)) {
                        // Dates are represented as java.sql.Timestamps, which is a subclass of Date, 
                        // so this should be safe to assign directly
                        trueValue = value;
                    }
                    else if (fieldType.equals(Long.class)) {
                        logger.info(key+" is a "+value.getClass().getName()+" with value "+value);
                        trueValue = Long.parseLong(value.toString());
                    }
                    else if (fieldType.equals(Integer.class)) {
                        if (!StringUtils.isEmpty(value.toString())) {
                            trueValue = Integer.parseInt(value.toString());
                        }
                    }
                    else if (fieldType.equals(Boolean.class)) {
                        if (value instanceof Boolean) {
                            trueValue = value;
                        }
                        else if (value instanceof Integer) {
                            trueValue = new Boolean(((Integer)value)!=0);
                        }
                        else {
                            throw new Exception("Cannot parse "+value+" into a Boolean");
                        }
                    }
                    else {
                        // This might take care of future types we may not have anticipated
                        trueValue = value;
                    }
                }

                Object currValue = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(lsm, attr.field);
                if (differ(currValue, trueValue)) {
                    org.janelia.it.jacs.shared.utils.ReflectionUtils.setFieldValue(lsm, attr.field, trueValue);
                    dirty = true;
                }
            }
            catch (Exception e) {
                logger.error("Error setting SAGE attribute value "+key+" for LSM#"+lsm.getId(),e);
            }
        }

        if (lsm.getVoxelSizeX()!=null && lsm.getVoxelSizeY()!=null && lsm.getVoxelSizeZ()!=null) {
            String opticalRes = lsm.getVoxelSizeX()+"x"+lsm.getVoxelSizeY()+"x"+lsm.getVoxelSizeZ();
            if (!StringUtils.areEqual(lsm.getOpticalResolution(), opticalRes)) {
                lsm.setOpticalResolution(opticalRes);
                dirty = true;
            }
        }

        if (lsm.getDimensionX()!=null && lsm.getDimensionY()!=null && lsm.getDimensionZ()!=null) {
            String imageSize = lsm.getDimensionX()+"x"+lsm.getDimensionY()+"x"+lsm.getDimensionZ();
            if (!StringUtils.areEqual(lsm.getImageSize(), imageSize)) {
                lsm.setImageSize(imageSize);
                dirty = true;
            }
        }
        
        if (lsm.getGender()!=null) {
            String gender = sanitizeGender(lsm.getGender());
            if (!StringUtils.areEqual(lsm.getGender(), gender)) {
                lsm.setGender(gender);
                dirty = true;
            }
        }
        
        return dirty;
    }

    /**
     * Convert non-standard gender values like "Female" into standardized codes like "f". The
     * four standardized codes are "m", "f", "x", and "NO_CONSENSUS" in the case of samples.
     */
    private String sanitizeGender(String gender) {
        if (gender==null) {
            return null;
        }
        if (gender.equals(NO_CONSENSUS_VALUE)) {
            return NO_CONSENSUS_VALUE;
        }
        String genderLc = gender.toLowerCase();
        if (genderLc.startsWith("f")) {
            return "f";
        }
        else if (genderLc.startsWith("m")) {
            return "m";
        }
        else if (genderLc.startsWith("x")) {
            return "x";
        } 
        else {
            logger.warn("Invalid value for gender: "+gender);
            return null;
        }
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
    public Sample createOrUpdateSample(String slideCode, DataSet dataSet, Collection<SlideImageGroup> tileGroupList) throws Exception {

        logger.info("Creating or updating sample: "+slideCode+" ("+(dataSet==null?"":"dataSet="+dataSet.getName())+")");
        
        Multimap<String,SlideImageGroup> objectiveGroups = HashMultimap.create();
        for(SlideImageGroup tileGroup : tileGroupList) {
            String groupObjective = null;
            for(LSMImage lsm : tileGroup.getImages()) {
                if (groupObjective==null) {
                    groupObjective = lsm.getObjective();
                }
                else if (!groupObjective.equals(lsm.getObjective())) {
                    logger.warn("  No consensus for objective in tile group '"+tileGroup.getTag()+"' ("+groupObjective+" != "+lsm.getObjective()+")");
                }
            }
            if (groupObjective==null) {
                groupObjective = "";
            }
            objectiveGroups.put(groupObjective, tileGroup);
        }    
        
        logger.debug("  Sample objectives: "+objectiveGroups.keySet());
                
        boolean dirty = false;
        
        Sample sample = getOrCreateSample(slideCode, dataSet);
        if (sample.getId()==null) {
            dirty = true;
        }
                
        if (setSampleAttributes(dataSet, sample, tileGroupList)) {
            dirty = true;
        }
        
        List<String> objectives = new ArrayList<>(objectiveGroups.keySet());
        Collections.sort(objectives);
        for(String objective : objectives) {
            Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                        
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(tileGroupList);
            int sampleNumChannels = sampleNumSignals+1;
            String channelSpec = ChanSpecUtils.createChanSpec(sampleNumChannels, sampleNumChannels);

            logger.info("  Sample attributes: objective="+objective+", signalChannels="+sampleNumSignals+", chanSpec="+channelSpec);
            
            // Find the sample, if it exists, or create a new one.
            if (createOrUpdateObjectiveSample(sample, slideCode, objective, channelSpec, subTileGroupList)) {
                dirty = true;
            }
        }
        
        if (DomainConstants.VALUE_DESYNC.equals(sample.getStatus())) {
            // This sample is no longer desynchronized, so delete its desync status
            sample.setStatus(null);
            dirty = true;
        }
        
        if (dirty) {
            sample.setVisited(true);
            domainDao.save(ownerKey, sample);
            markForProcessing(sample);
        }
        else {
            domainDao.updateProperty(ownerKey, Sample.class, sample.getId(), "visited", true);
            
        }
        // TODO: Auto-share sample if necessary 
        
        logger.debug("  Setting visited flag on sample: "+sample.getName()+" (id="+sample.getId()+")");        
        return sample;
    }
    
    private Sample getOrCreateSample(String slideCode, DataSet dataSet) {
        
        Sample sample = domainDao.getSampleBySlideCode(ownerKey, dataSet.getIdentifier(), slideCode);
        if (sample==null) {
            sample = new Sample();
            sample.setDataSet(dataSet.getIdentifier());
            sample.setSlideCode(slideCode);
        }
        return sample;
    }
    
    private boolean setSampleAttributes(DataSet dataSet, Sample sample, Collection<SlideImageGroup> tileGroupList) {

        boolean dirty = false;
        Date maxTmogDate = null;

        Map<String,Object> consensusValues = new HashMap<>();
        Map<String,LsmSageAttribute> lsmSageAttrs = getLsmSageAttrs();
        
        for(SlideImageGroup tileGroup : tileGroupList) {
            for(LSMImage lsm : tileGroup.getImages()) {
                
                for(LsmSageAttribute lsmAttr : lsmSageAttrs.values()) {
                    String fieldName = lsmAttr.field.getName();
                    Object value = null;
                    try {
                        value = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(lsm, lsmAttr.field);
                    }
                    catch (Exception e) {
                        logger.error("  Problem getting value for LSMImage."+fieldName,e);
                    }
                    // Special consideration is given to the TMOG Date, so that the latest LSM TMOG date is recorded as the Sample TMOG date. 
                    if ("tmogDate".equals(fieldName)) {
                        Date date = (Date)value;
                        if (maxTmogDate==null || date.after(maxTmogDate)) {
                            maxTmogDate = date;
                        }
                    }
                    else {
                        Object consensusValue = consensusValues.get(fieldName);
                        if (consensusValue==null) {
                            consensusValues.put(fieldName, value);
                        }
                        else if (!consensusValue.equals(value)) {
                            consensusValues.put(fieldName, NO_CONSENSUS_VALUE);
                        }    
                    }
                }
            }
        }

        if (maxTmogDate!=null) {
            consensusValues.put("tmogDate", maxTmogDate);
        }
        
        logger.info("  Consensus values: ");
        for(String key : consensusValues.keySet()) {
            Object value = consensusValues.get(key);
            logger.info("    "+key+": "+value);
        }
        
        Map<String,DomainObjectAttribute> sampleAttrs = getSampleAttrs();
        for(String fieldName : consensusValues.keySet()) {
            Object consensusValue = consensusValues.get(fieldName);
            DomainObjectAttribute sampleAttr = sampleAttrs.get(fieldName);
            if (sampleAttr!=null) {
                try {
                    Object currValue = sampleAttr.getGetter().invoke(sample);
                    if (differ(currValue, consensusValue)) {
                        sampleAttr.getSetter().invoke(sample, consensusValue);
                        logger.info("  Setting "+fieldName+"="+consensusValue);
                        dirty = true;
                    }
                }
                catch (Exception e) {
                    logger.error("  Problem setting Sample."+fieldName,e);
                }
            }
        }
        
        String newName = getSampleName(dataSet, sample, consensusValues);
        if (!sample.getName().equals(newName)) {
            logger.info("  Updating sample name to: "+newName);
            sample.setName(newName);
            dirty = true;
        }
        
        return dirty;
    }

    /**
     * Create a new name for a sample, given the sample's attributes.
     * {Line}-{Slide Code}-Right_Optic_Lobe
     * {Line}-{Slide Code}-Left_Optic_Lobe
     * {VT line|Line}-{Slide Code}-Left_Optic_Lobe
     * {Line}-{Effector}-{Age}
     */
    public String getSampleName(DataSet dataSet, Sample sample, Map<String,Object> sampleProperties) {
        
        String sampleNamePattern = dataSet==null?DEFAULT_SAMPLE_NAME_PATTERN:dataSet.getSampleNamePattern();

        Pattern pattern = Pattern.compile("\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(sampleNamePattern);
        StringBuffer buffer = new StringBuffer();
        logger.trace("    Building sample name:");
        while (matcher.find()) {
            String tmpGroup = matcher.group(1);
            String[] replacementPieces = tmpGroup.split("\\|");
            String replacement=null;
            for (String tmpPiece:replacementPieces) {
                Object obj = sampleProperties.get(tmpPiece.trim());
                if (obj!=null) {
                    replacement = obj.toString();
                    if (replacement != null) {
                        matcher.appendReplacement(buffer, "");
                        buffer.append(replacement);
                        break;
                    }
                    logger.trace("        " + replacement+" -> "+buffer);
                }
            }
            if (null==replacement) {
                logger.warn("Cannot find a property replacement for Sample Naming Pattern element " + tmpGroup);
            }
        }
        matcher.appendTail(buffer);
        logger.trace("        append tail -> "+buffer);
        return buffer.toString();
    }
    
    private boolean createOrUpdateObjectiveSample(Sample sample, String channelSpec, String objective, String chanSpec, Collection<SlideImageGroup> tileGroupList) throws Exception {

        boolean dirty = false;
        
        ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
        if (objectiveSample==null) {
            objectiveSample = new ObjectiveSample();
            synchronizeTiles(objectiveSample, tileGroupList);
            numSamplesCreated++;
            dirty = true;
        }
        else if (synchronizeTiles(objectiveSample, tileGroupList)) {
            dirty = true;
            numSamplesUpdated++;
            logger.debug("Updated objective "+objective+" for sample "+sample.getName());
        }
        
        return dirty;
    }

    public boolean synchronizeTiles(ObjectiveSample objectiveSample, Collection<SlideImageGroup> tileGroupList) throws Exception {
        
        boolean dirty = false;

        if (!tilesMatch(objectiveSample, tileGroupList)) {
            // Something has changed, so just recreate the tiles
            List<SampleTile> tiles = new ArrayList<>();
            for (SlideImageGroup tileGroup : tileGroupList) {
                SampleTile sampleTile = new SampleTile();
                sampleTile.setName(tileGroup.getTag());
                sampleTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
                List<Reference> lsmReferences = new ArrayList<>();
                for(LSMImage lsm : tileGroup.getImages()) {
                    lsmReferences.add(Reference.createFor(lsm));
                }
                sampleTile.setLsmReferences(lsmReferences);
            }
            objectiveSample.setTiles(tiles);
            logger.debug("  Updated tiles for objective "+objectiveSample.getObjective());
            dirty = true;
        }

        for (SlideImageGroup tileGroup : tileGroupList) {
            SampleTile sampleTile = objectiveSample.getTileByName(tileGroup.getTag());
            if (!tileGroup.getAnatomicalArea().equals(sampleTile.getAnatomicalArea())) {
                sampleTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
                logger.debug("  Updated anatomical area for tile "+sampleTile.getName());
                dirty = true;
            }
        }
        
        return dirty;
    }
    
    public boolean tilesMatch(ObjectiveSample objectiveSample, Collection<SlideImageGroup> tileGroupList) throws Exception {
        
        Set<SampleTile> seenTiles = new HashSet<>();
        
        for (SlideImageGroup tileGroup : tileGroupList) {

            SampleTile sampleTile = objectiveSample.getTileByName(tileGroup.getTag());
            if (sampleTile==null) {
                // Ensure each tile is in the sample
                return false;
            }
            seenTiles.add(sampleTile);
            
            Set<Long> lsmIds1 = new HashSet<>();
            for(LSMImage lsm : tileGroup.getImages()) {
                lsmIds1.add(lsm.getId());
            }

            Set<Long> lsmIds2 = new HashSet<>();
            for(Reference lsmReference : sampleTile.getLsmReferences()) {
                lsmIds2.add(lsmReference.getTargetId());
            }
            
            if (!lsmIds1.equals(lsmIds2)) {
                // Ensure each tiles references the correct LSMs
                return false;
            }
        }
        
        if (objectiveSample.getTiles().size() != seenTiles.size()) {
            // Ensure that the sample has no extra tiles it doesn't need
            return false;
        }
        
        return true;
    }

    /**
     * Create and return a new sample tile based on the given SAGE image data.
     * @return
     * @throws Exception
     */
    public SampleTile createTile(SlideImageGroup tileGroup) throws Exception {
        SampleTile imageTile = new SampleTile();
        setTileAttributes(imageTile, tileGroup);
        return imageTile;
    }
    
    /**
     * Set the tile attributes from the given SAGE image data.
     * @return true if something changed
     * @throws Exception
     */
    public boolean setTileAttributes(SampleTile imageTile, SlideImageGroup tileGroup) throws Exception {
        boolean tileDirty = false;
        logger.debug("    Setting tile properties: name="+tileGroup.getTag()+", anatomicalArea="+tileGroup.getAnatomicalArea());
        if (imageTile.getName()==null || !imageTile.getName().equals(tileGroup.getTag())) {
            imageTile.setName(tileGroup.getTag());
            tileDirty = true;
        }
        if (!StringUtils.areEqual(tileGroup.getAnatomicalArea(),imageTile.getAnatomicalArea())) {
            imageTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
            tileDirty = true;
        }
        return tileDirty;
    }
    
    
    private void markForProcessing(Sample sample) throws Exception {
        String sampleStatus = sample.getStatus();
        if (sampleStatus!=null) {
            // Certain statuses mean that a sample should not be reprocessed
            if (sampleStatus.equals(DomainConstants.VALUE_BLOCKED) || sampleStatus.equals(DomainConstants.VALUE_RETIRED)) {
                return;
            }
        }
        logger.info("  Sample tiles changed, marking for reprocessing: "+sample.getName());
        sample.setStatus(DomainConstants.VALUE_MARKED);
        domainDao.save(ownerKey, sample);
        numSamplesReprocessed++;
    }
    
    /**
     * Return the channel specification for the LSM (or create a default one using the number of channels).
     * @param lsm
     * @return
     */
    public String getLSMChannelSpec(LSMImage lsm, int refIndex) {
        
        String chanSpec = lsm.getChanSpec();
        if (!StringUtils.isEmpty(chanSpec)) {
            return chanSpec;
        }
        
        Integer numChannels = lsm.getNumChannels();
        if (numChannels!=null) {
            try {
            	return ChanSpecUtils.createChanSpec(numChannels, refIndex+1);    
            }
            catch (NumberFormatException e) {
                logger.warn("Could not parse Num Channels ('"+numChannels+"') on LSM with id="+lsm.getId());
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
            for(LSMImage lsm : tileGroup.getImages()) {
                String chanspec = lsm.getChanSpec();
                if (chanspec!=null) {
                    for(int j=0; j<chanspec.length(); j++) {
                        if (chanspec.charAt(j)=='s') {
                            tileNumSignals++;
                        }
                    }
                }
            }
            
            if (tileNumSignals<1) {
                logger.debug("  Falling back on channel number");
                // We didn't get the information from the channel spec, let's fall back on inference from numChannels
                for(LSMImage lsm : tileGroup.getImages()) {
                    Integer numChannels = lsm.getNumChannels();
                    if (numChannels!=null) {
                        tileNumSignals += numChannels - 1;
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
     * Go through a sample area's LSM supporting files and look for an attribute with a given name. If a consensus
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
     * Go through a set of sample areas' LSM supporting files and look for an attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusLsmAttributeValue(List<AnatomicalArea> sampleAreas, String attrName) throws Exception {
        Sample sample = null;
        String consensus = null;
        logger.trace("Determining consensus for "+attrName+" for sample areas: "+getSampleAreasCSV(sampleAreas));
        for(AnatomicalArea sampleArea : sampleAreas) {
        	logger.trace("  Determining consensus for "+attrName+" in "+sampleArea.getName()+" sample area");
		
        	if (sample==null) {
            	sample = domainDao.getDomainObject(null, Sample.class, sampleArea.getSampleId());
        	}
        	else if (!sample.getId().equals(sampleArea.getSampleId())) {
        	    throw new IllegalStateException("All sample areas must come from the same sample");
        	}
        	
        	ObjectiveSample objectiveSample = sample.getObjectiveSample(sampleArea.getObjective());
        	for(String tileName : sampleArea.getTileNames()) {
        	    logger.trace("    Determining consensus for "+attrName+" in "+tileName+" tile");
        	    SampleTile sampleTile = objectiveSample.getTileByName(tileName);
            	List<LSMImage> lsms = domainDao.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
            	
                for(LSMImage image : lsms) {
    	        	logger.trace("      Determining consensus for "+attrName+" in "+image.getName()+" LSM");
                    Object value = DomainUtils.getAttributeValue(image, attrName);
                    if (consensus!=null && !consensus.equals(value)) {
                        logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample area "+sampleArea.getName());
                        return null;
                    }
                    else {
                        consensus = value==null?null:value.toString();
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
     * Return the data sets for the configured owner.
     * @return
     * @throws Exception
     */
    public Collection<DataSet> getDataSets() throws Exception {
        if (dataSets==null) {
            loadDataSets();
        }
        return dataSets;
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
    
    public int getNumSamplesMovedToBlockedFolder() {
        return numSamplesMovedToBlockedFolder;
    }
    
    public int getNumSamplesReprocessed() {
        return numSamplesReprocessed;
    }

    public Map<String, DataSet> getDataSetByIdentifierMap() throws Exception {
        if (dataSetByIdentifier==null) {
            loadDataSets();
        }
        return dataSetByIdentifier;
    }

    private void loadDataSets() throws Exception {
        
        if (dataSets!=null) return;
        
        this.dataSetByIdentifier = new HashMap<>();
        this.dataSets = domainDao.getDomainObjects(ownerKey, DataSet.class);

        if (dataSetNameFilter != null) {
            List<DataSet> filteredDataSets = new ArrayList<>();
            for (DataSet dataSet : dataSets) {
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
        
        Collections.sort(dataSets, new Comparator<DataSet>() {
			@Override
			public int compare(DataSet o1, DataSet o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

        logger.info("Preloading data sets...");
        for(DataSet dataSet : dataSets) {
            // Cache the data set
            String dataSetIdentifier = dataSet.getIdentifier();
            dataSetByIdentifier.put(dataSetIdentifier, dataSet);
        }
    }
    
    // TODO: move to utility class
    private boolean differ(Object value1, Object value2) {
        return (value1==null&&value2!=null) || (value1!=null&&!value1.equals(value2));
    }

    public List<SampleTile> getTiles(ObjectiveSample objectiveSample, List<String> tileNames) {
        List<SampleTile> tiles = new ArrayList<>();
        for(SampleTile tile : objectiveSample.getTiles()) {
            if (tileNames.contains(tile.getName())) {
                tiles.add(tile);
            }
        }
        return tiles;
    }
    
    /* --------------------------- */

    public void saveLsm(LSMImage lsm) throws Exception {
        domainDao.save(ownerKey, lsm);
    }
    
    public void saveSample(Sample sample) throws Exception {
        domainDao.save(ownerKey, sample);
    }

    public void saveNeuron(NeuronFragment neuron) throws Exception {
        domainDao.save(ownerKey, neuron);
    }

    public SamplePipelineRun addNewPipelineRun(ObjectiveSample objectiveSample, String name, String pipelineProcess, int pipelineVersion) {
        SamplePipelineRun run = new SamplePipelineRun();
        run.setId(domainDao.getNewId());
        run.setCreationDate(new Date());
        run.setName(name);
        run.setPipelineProcess(pipelineProcess);
        run.setPipelineVersion(pipelineVersion);
        objectiveSample.addRun(run);
        return run;
    }

    public LSMSummaryResult addNewLSMSummaryResult(SamplePipelineRun run, String resultName) {
        LSMSummaryResult result = new LSMSummaryResult();
        result.setId(domainDao.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public SampleProcessingResult addNewSampleProcessingResult(SamplePipelineRun run, String resultName) {
        SampleProcessingResult result = new SampleProcessingResult();
        result.setId(domainDao.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }

    public SampleAlignmentResult addNewAlignmentResult(SamplePipelineRun run, String resultName) {
        SampleAlignmentResult result = new SampleAlignmentResult();
        result.setId(domainDao.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public SamplePostProcessingResult addNewSamplePostProcessingResult(SamplePipelineRun run, String resultName) {
        SamplePostProcessingResult result = new SamplePostProcessingResult();
        result.setId(domainDao.getNewId());
        result.setCreationDate(new Date());
        result.setName(resultName);
        result.setFiles(new HashMap<FileType,String>());
        run.addResult(result);
        return result;
    }
    
    public NeuronSeparation addNewNeuronSeparation(PipelineResult result, String resultName) {
        NeuronSeparation separation = new NeuronSeparation();
        separation.setId(domainDao.getNewId());
        separation.setCreationDate(new Date());
        separation.setFiles(new HashMap<FileType,String>());

        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setReferringClassName(NeuronFragment.class.getName());
        fragmentsReference.setReferenceAttr("separationId");
        fragmentsReference.setReferenceId(separation.getId());
        separation.setFragmentsReference(fragmentsReference);
        
        result.addResult(separation);
        return separation;
    }

    public NeuronFragment addNewNeuronFragment(NeuronSeparation separation, Integer index) {
        NeuronFragment neuron = new NeuronFragment();
        neuron.setCreationDate(new Date());
        neuron.setName("Neuron Fragment "+index);
        neuron.setNumber(index);
        neuron.setSample(Reference.createFor(separation.getParentRun().getParent().getParent()));
        neuron.setSeparationId(separation.getId());
        neuron.setFilepath(separation.getFilepath());
        neuron.setFiles(new HashMap<FileType,String>());
        return neuron;
    }
    
    public PipelineError setPipelineRunError(SamplePipelineRun run, String filepath, String description, String classification) {
        PipelineError error = new PipelineError();
        error.setFilepath(filepath);
        error.setDescription(description);
        error.setClassification(classification);
        run.setError(error);
        return error;
    }

    public PipelineResult addResult(SamplePipelineRun run, PipelineResult result) {
        run.addResult(result);
        return result;
    }

    public Map<String,FileGroup> createFileGroups(HasFilepath parent, List<String> filepaths) throws Exception {

        Map<String,FileGroup> groups = new HashMap<>();
    
        for(String filepath : filepaths) {
            
            File file = new File(filepath);
            String filename = file.getName();
            int d = filename.lastIndexOf('.');
            String name = filename.substring(0, d);
            String ext = filename.substring(d+1);
            
            FileType fileType = null;

            String key = null;
            if (filename.endsWith(".lsm.json")) {
                key = name;
                fileType = FileType.LsmMetadata;
            }
            else if (filename.endsWith(".lsm.metadata")) {
                // Ignore, to get rid of the old-style Perl metadata files
                continue;
            }
            else if ("properties".equals(ext)) {
                // Ignore properties files here, they should be specifically processed, not sucked into a file group
                continue;
            }
            else {
                int u = name.lastIndexOf('_');
                key = name.substring(0, u);
                String type = name.substring(u+1);
                if ("png".equals(ext)) {
                    if ("all".equals(type)) {
                        fileType = FileType.AllMip; 
                    }
                    else if ("reference".equals(type)) {
                        fileType = FileType.ReferenceMip;   
                    }
                    else if ("signal".equals(type)) {
                        fileType = FileType.SignalMip;  
                    }
                    else if ("signal1".equals(type)) {
                        fileType = FileType.Signal1Mip; 
                    }
                    else if ("signal2".equals(type)) {
                        fileType = FileType.Signal2Mip; 
                    }
                    else if ("signal3".equals(type)) {
                        fileType = FileType.Signal3Mip; 
                    }
                    else if ("refsignal1".equals(type)) {
                        fileType = FileType.RefSignal1Mip;  
                    }
                    else if ("refsignal2".equals(type)) {
                        fileType = FileType.RefSignal2Mip;  
                    }
                    else if ("refsignal3".equals(type)) {
                        fileType = FileType.RefSignal3Mip;  
                    }
                }
                else if ("mp4".equals(ext)) {
                    if ("all".equals(type) || "movie".equals(type)) {
                        fileType = FileType.AllMovie;   
                    }
                    else if ("reference".equals(type)) {
                        fileType = FileType.ReferenceMovie; 
                    }
                    else if ("signal".equals(type)) {
                        fileType = FileType.SignalMovie;    
                    }
                }
            }
            
            if (fileType==null) {
                logger.warn("  Could not determine file type for: "+filename);
                continue;
            }
            
            FileGroup group = groups.get(key);
            if (group==null) {
                group = new FileGroup();
                group.setFilepath(parent.getFilepath());
                group.setFiles(new HashMap<FileType,String>());
                groups.put(key, group);
            }
            
            DomainUtils.setFilepath(group, fileType, filepath);
        }
        
        return groups;
    }

    public void sortMembersByName(ObjectSet objectSet) throws Exception {
        if (objectSet==null || !objectSet.hasMembers()) return;
        final Map<Long,DomainObject> map = DomainUtils.getMapById(domainDao.getMembers(ownerKey, objectSet));
        Collections.sort(objectSet.getMembers(), new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                DomainObject d1 = map.get(o1);
                DomainObject d2 = map.get(o2);
                return d1.getName().compareTo(d2.getName());
            }
        });
        domainDao.save(ownerKey, objectSet);
    }
    
    public void sortChildrenByName(TreeNode treeNode) throws Exception {
        if (treeNode==null || !treeNode.hasChildren()) return;
        final Map<Long,DomainObject> map = DomainUtils.getMapById(domainDao.getChildren(ownerKey, treeNode));
        Collections.sort(treeNode.getChildren(), new Comparator<Reference>() {
            @Override
            public int compare(Reference o1, Reference o2) {
                DomainObject d1 = map.get(o1.getTargetId());
                DomainObject d2 = map.get(o2.getTargetId());
                return d1.getName().compareTo(d2.getName());
            }
        });
        domainDao.save(ownerKey, treeNode);
    }
}
