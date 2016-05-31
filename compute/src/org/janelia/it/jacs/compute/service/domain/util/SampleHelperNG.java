package org.janelia.it.jacs.compute.service.domain.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.model.SlideImage;
import org.janelia.it.jacs.compute.service.domain.model.SlideImageGroup;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.ReverseReference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFilepath;
import org.janelia.it.jacs.model.domain.sample.*;
import org.janelia.it.jacs.model.domain.support.DomainObjectAttribute;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.reflections.ReflectionUtils;


/**
 * Helper methods for dealing with Samples.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleHelperNG extends DomainHelper {

    private static final String NO_CONSENSUS_VALUE = "NO_CONSENSUS";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-{Slide Code}";
    
    private List<DataSet> dataSets;
    private String dataSetNameFilter;
    private Map<String,DataSet> dataSetByIdentifier;

    // Lookup tables
    private Map<String,SageField> lsmSageFields;
    private Map<String,SageField> sampleSageFields;
    private Map<String,DomainObjectAttribute> sampleAttrs;
    
    // Processing state
    private Map<Long,LSMImage> lsmCache = new HashMap<>();
    private Set<Long> updatedLsmIds = new HashSet<>();
    private Set<String> sageAttrsNotFound = new HashSet<>();
    private int numSamplesCreated = 0;
    private int numSamplesUpdated = 0;
    private int numSamplesReprocessed = 0;
    
    public SampleHelperNG(ComputeBeanRemote computeBean, String ownerKey, Logger logger) {
        this(computeBean, ownerKey, logger, null);
    }

    public SampleHelperNG(ComputeBeanRemote computeBean, String ownerKey, Logger logger, ContextLogger contextLogger) {
        super(computeBean, ownerKey, logger, contextLogger);
    }
    
    public void setDataSetNameFilter(String dataSetNameFilter) {
        this.dataSetNameFilter = dataSetNameFilter;
    }

    public LSMImage createOrUpdateLSM(SlideImage slideImage) throws Exception {
        
    	logger.debug("createOrUpdateLSM("+slideImage.getName()+")");
        boolean dirty = false;
        
        LSMImage lsm = domainDao.getLsmBySageId(ownerKey, slideImage.getSageId());
        if (lsm==null) {
            lsm = new LSMImage();
            lsm.setFiles(new HashMap<FileType,String>());
            logger.info("Created new LSM for SAGE image#"+slideImage.getSageId());
            dirty = true;
        }
        
        if (updateLsmAttributes(lsm, slideImage)) {
            logger.info("Updated LSM properties for "+slideImage.getName());
            dirty = true;
        }
        
        if (dirty) {
            lsm = domainDao.save(ownerKey, lsm);
            updatedLsmIds.add(lsm.getId());
        }
        else if (!lsm.getSageSynced()) {
            domainDao.updateProperty(ownerKey, LSMImage.class, lsm.getId(), "sageSynced", true);
        }
        
        lsmCache.put(lsm.getId(), lsm);
        return lsm;
    }
    
    private boolean updateLsmAttributes(LSMImage lsm, SlideImage slideImage) throws Exception {

    	logger.debug("updateLsmAttribute(lsmId="+lsm.getId()+",sageId="+slideImage.getSageId()+")");
        boolean dirty = false;
        
        Map<String,SageField> lsmSageAttrs = getLsmSageFields();
        for(String key : slideImage.getProperties().keySet()) {
            try {
                SageField attr = lsmSageAttrs.get(key);
                if (attr==null) {
                	if (!sageAttrsNotFound.contains(key)) {
                		logger.warn("SAGE Attribute not found on LSMImage: "+key);
                		sageAttrsNotFound.add(key);
                	}
                	continue;
                }
                Object value = slideImage.getProperties().get(key);
                String strValue = value==null?null:value.toString();
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
                    	if (value instanceof Long) {
                    		trueValue = (Long)value;
                    	}
                    	else {
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Long(strValue);
                            }
                    	}
                    }
                    else if (fieldType.equals(Integer.class)) {
                    	if (value instanceof Integer) {
                    		trueValue = (Integer)value;
                    	}
                    	else {
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Integer(strValue);
                            }
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
                            if (!StringUtils.isEmpty(strValue)) {
                            	trueValue = new Boolean(strValue);
                            }
                        }
                    }
                    else {
                        // This might take care of future types we may not have anticipated
                        trueValue = value;
                    }
                }

                String fieldName = attr.field.getName();
                Object currValue = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(lsm, attr.field);
                if (!StringUtils.areEqual(currValue, trueValue)) {
                    org.janelia.it.jacs.shared.utils.ReflectionUtils.setFieldValue(lsm, attr.field, trueValue);
	                logger.info("  Setting "+fieldName+"="+trueValue);
	                dirty = true;
	            }
	            else {
	                logger.debug("  Already set "+fieldName+"="+trueValue);
	            }
            }
            catch (Exception e) {
                logger.error("Error setting SAGE attribute value "+key+" for LSM#"+lsm.getId(),e);
            }
        }

        // Other attributes which are not automatically populated using @SAGEAttribute
        
        if (!StringUtils.areEqual(lsm.getName(), slideImage.getName())) {
            lsm.setName(slideImage.getName());
            dirty = true;
        }
        
        String filepath = slideImage.getFilepath();
        if (!StringUtils.areEqual(lsm.getFilepath(), filepath)) {
            lsm.setFilepath(filepath);
            lsm.getFiles().put(FileType.LosslessStack,filepath);
            dirty = true;
        }

        String objective = slideImage.getObjective();
        if (!StringUtils.areEqual(lsm.getObjective(), objective)) {
            lsm.setObjective(objective);
            dirty = true;
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

        if (lsm.getAnatomicalArea()==null) {
            lsm.setAnatomicalArea("");
            dirty = true;
        }

        return dirty;
    }
        
    public Sample createOrUpdateSample(String slideCode, DataSet dataSet, Collection<LSMImage> lsms) throws Exception {

    	logger.info("Creating or updating sample: "+slideCode+" ("+(dataSet==null?"":"dataSet="+dataSet.getName())+")");

        Multimap<String,SlideImageGroup> objectiveGroups = HashMultimap.create();
    	boolean lsmDirty = false;
        int tileNum = 0;
        for(LSMImage lsm : lsms) {

        	// Have any of the LSMs been updated? If so, we need to mark the sample for reprocessing later.
        	if (updatedLsmIds.contains(lsm.getId())) {
        		lsmDirty = true;
        	}
        	
        	// Extract LSM metadata
        	String objective = lsm.getObjective();
            String area = lsm.getAnatomicalArea();
            String tag = lsm.getTile();
            if (tag==null) {
                tag = "Tile "+(tileNum+1);
            }

            // Group LSMs by objective and tile
            Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
            SlideImageGroup group = null;
            for(SlideImageGroup slideImageGroup : subTileGroupList) {
            	if (StringUtils.areEqual(slideImageGroup.getTag(), tag)) {
            		group = slideImageGroup;
            		if (!StringUtils.areEqual(slideImageGroup.getAnatomicalArea(), area)) {
                        logger.warn("  No consensus for area in tile group '"+group.getTag()+"' ("+slideImageGroup.getAnatomicalArea()+" != "+area+")");
                		group.setAnatomicalArea(NO_CONSENSUS_VALUE);
            		}
            		break;
            	}
            }
            if (group==null) {
            	group = new SlideImageGroup(area, tag);
            	objectiveGroups.put(objective, group);
            }
            group.addFile(lsm);
            
            tileNum++;
        }
        
        logger.debug("  Sample objectives: "+objectiveGroups.keySet());

        boolean sampleNew = false;
        boolean sampleDirty = false;
        boolean needsReprocessing = lsmDirty;
        
        Sample sample = getOrCreateSample(slideCode, dataSet);
        if (sample.getId()==null) {
            sampleDirty = true;
            needsReprocessing = true;
            sampleNew = true;
        }
                
        if (setSampleAttributes(dataSet, sample, objectiveGroups.values())) {
            sampleDirty = true;
            needsReprocessing = true;
        }

        // First, remove all tiles/LSMSs from objectives which are no longer found in SAGE
        for(ObjectiveSample objectiveSample : new ArrayList<>(sample.getObjectiveSamples())) {
        	if (!objectiveGroups.containsKey(objectiveSample.getObjective())) {
	        	if (objectiveSample.hasPipelineRuns()) {
	        		sample.removeObjectiveSample(objectiveSample);
	        	}
	        	else {
	        		objectiveSample.setTiles(new ArrayList<SampleTile>());
	        	}
	            sampleDirty = true;
        	}
        }
        
        List<String> objectives = new ArrayList<>(objectiveGroups.keySet());
        Collections.sort(objectives);
        for(String objective : objectives) {
            Collection<SlideImageGroup> subTileGroupList = objectiveGroups.get(objective);
                        
            // Figure out the number of channels that should be in the final merged/stitched sample
            int sampleNumSignals = getNumSignalChannels(subTileGroupList);
            int sampleNumChannels = sampleNumSignals+1;
            String channelSpec = ChanSpecUtils.createChanSpec(sampleNumChannels, sampleNumChannels);

            logger.info("  Processing objective "+objective+", signalChannels="+sampleNumSignals+", chanSpec="+channelSpec);
            
            // Find the sample, if it exists, or create a new one.
            if (createOrUpdateObjectiveSample(sample, slideCode, objective, channelSpec, subTileGroupList)) {
                sampleDirty = true;
                needsReprocessing = true;
            }
        }
        
        if (DomainConstants.VALUE_DESYNC.equals(sample.getStatus())) {
            // This sample is no longer desynchronized, so delete its desync status
            sample.setStatus(null);
            sampleDirty = true;
        }

        if (needsReprocessing && sample.getId()!=null) {
		    if (lsmDirty) {
		    	logger.info("  LSMs changed, will mark sample for reprocessing");
		    }
        	markForProcessing(sample);
        	sampleDirty = true;
        }
        
        if (sampleDirty) {
        	sample.setSageSynced(true);
            sample = domainDao.save(ownerKey, sample);
            logger.info("  Saving sample: "+sample.getName()+" (id="+sample.getId()+")");
            numSamplesUpdated++;
        }
        else if (!sample.getSageSynced()) {
            domainDao.updateProperty(ownerKey, Sample.class, sample.getId(), "sageSynced", true);
        }

        // Update all back-references from the sample's LSMs
        Reference sampleRef = Reference.createFor(sample);
        List<Reference> lsmRefs = sample.getLsmReferences();
        for(Reference lsmRef : lsmRefs) {
        	LSMImage lsm = lsmCache.get(lsmRef.getTargetId());
        	if (lsm==null) {
        		logger.warn("LSM (id="+lsmRef.getTargetId()+") not found in cache. This should never happen and indicates a bug.");
        		continue;
        	}
        	if (!StringUtils.areEqual(lsm.getSample(),sampleRef)) {
        		lsm.setSample(sampleRef);
        		saveLsm(lsm);
        		logger.info("Updated sample reference for LSM#"+lsm.getId());
        	}
        }

        if (sampleNew) { // We could disable this check to refresh all sample permissions
            domainDao.syncPermissions(dataSet.getOwnerKey(), Sample.class.getSimpleName(), sample.getId(), dataSet);
        }

        return sample;
    }
    
    private Sample getOrCreateSample(String slideCode, DataSet dataSet) {
        
        Sample sample = domainDao.getSampleBySlideCode(ownerKey, dataSet.getIdentifier(), slideCode);
        if (sample != null) {
        	logger.info("  Found existing sample "+sample.getId()+" with status "+sample.getStatus());
        	return sample;
        }
        
        // If no matching samples were found, create a new sample
        sample = new Sample();
        sample.setDataSet(dataSet.getIdentifier());
        sample.setSlideCode(slideCode);
    	logger.info("  Creating new sample for "+dataSet.getIdentifier()+"/"+slideCode);
        numSamplesCreated++;
        return sample;
    }
    
    private boolean setSampleAttributes(DataSet dataSet, Sample sample, Collection<SlideImageGroup> tileGroupList) {

        boolean dirty = false;
        Date maxTmogDate = null;

        Map<String,Object> consensusValues = new HashMap<>();
        Map<String,SageField> lsmSageAttrs = getLsmSageFields();
        
        for(SlideImageGroup tileGroup : tileGroupList) {
            for(LSMImage lsm : tileGroup.getImages()) {
                
                for(SageField lsmAttr : lsmSageAttrs.values()) {
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
                        else if (!StringUtils.areEqual(consensusValue,value)) {
                            consensusValues.put(fieldName, NO_CONSENSUS_VALUE);
                        }    
                    }
                }
            }
        }

        if (maxTmogDate!=null) {
            consensusValues.put("tmogDate", maxTmogDate);
        }
        
        if (logger.isTraceEnabled()) {
	        logger.trace("  Consensus values: ");
	        for(String key : consensusValues.keySet()) {
	            Object value = consensusValues.get(key);
	            logger.trace("    "+key+": "+value);
	        }
        }
        
        Map<String,SageField> sampleAttrs = getSampleSageFields();
        for(String fieldName : consensusValues.keySet()) {
        	SageField sampleAttr = sampleAttrs.get(fieldName);
            if (sampleAttr!=null) {
                try {
                	Object currValue = org.janelia.it.jacs.shared.utils.ReflectionUtils.getFieldValue(sample, sampleAttr.field);
                    Object consensusValue = consensusValues.get(fieldName);
                    if (!StringUtils.areEqual(currValue, consensusValue)) {
                    	org.janelia.it.jacs.shared.utils.ReflectionUtils.setFieldValue(sample, sampleAttr.field, consensusValue);
                        logger.info("  Setting "+fieldName+"="+consensusValue);
                        dirty = true;
                    }
                    else {
                        logger.debug("  Already set "+fieldName+"="+consensusValue);
                    }
                }
                catch (Exception e) {
                    logger.error("  Problem setting Sample."+fieldName,e);
                }
            }
            else {
                logger.debug("  Not a sample attribute: "+fieldName);
            }
        }
        
        String newName = getSampleName(dataSet, sample);
        if (!StringUtils.areEqual(sample.getName(),newName)) {
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
    public String getSampleName(DataSet dataSet, Sample sample) {
        
    	Map<String,DomainObjectAttribute> sampleAttrs = getSampleAttrs();
        Map<String,Object> valueMap = new HashMap<>();
        for(String key : sampleAttrs.keySet()) {
        	DomainObjectAttribute attr = sampleAttrs.get(key);
        	Object obj = null;
        	try {
        		obj = attr.getGetter().invoke(sample);
        	}
        	catch (Exception e) {
        		logger.error("Error getting sample attribute value for: "+key,e);
        	}
        	if (obj!=null) {
        		valueMap.put(key, obj.toString());
        	}
        }

        String sampleNamePattern = dataSet==null?DEFAULT_SAMPLE_NAME_PATTERN:dataSet.getSampleNamePattern();
        return StringUtils.replaceVariablePattern(sampleNamePattern, valueMap);
    }
    
    private boolean createOrUpdateObjectiveSample(Sample sample, String channelSpec, String objective, String chanSpec, Collection<SlideImageGroup> tileGroupList) throws Exception {

        boolean dirty = false;
        
        ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
        if (objectiveSample==null) {
            objectiveSample = new ObjectiveSample(objective);
            sample.addObjectiveSample(objectiveSample);
            synchronizeTiles(objectiveSample, tileGroupList);
            dirty = true;
        }
        else if (synchronizeTiles(objectiveSample, tileGroupList)) {
            dirty = true;
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
                tiles.add(sampleTile);
            }
            objectiveSample.setTiles(tiles);
            logger.info("  Updated tiles for objective "+objectiveSample.getObjective());
            return true;
        }

        for (SlideImageGroup tileGroup : tileGroupList) {
            SampleTile sampleTile = objectiveSample.getTileByName(tileGroup.getTag());
            if (sampleTile==null) {
            	throw new IllegalStateException("No such tile: "+tileGroup.getTag());
            }
            if (!StringUtils.areEqual(tileGroup.getAnatomicalArea(),sampleTile.getAnatomicalArea())) {
                sampleTile.setAnatomicalArea(tileGroup.getAnatomicalArea());
                logger.info("  Updated anatomical area for tile "+sampleTile.getName()+" to "+sampleTile.getAnatomicalArea());
                dirty = true;
            }
        }
        
        return dirty;
    }
    
    public boolean tilesMatch(ObjectiveSample objectiveSample, Collection<SlideImageGroup> tileGroupList) throws Exception {
        
        Set<SampleTile> seenTiles = new HashSet<>();
        
        logger.debug("  Checking if tiles match");
        
        for (SlideImageGroup tileGroup : tileGroupList) {
        	
        	logger.debug("  Checking for "+tileGroup.getTag());

            // Ensure each tile is in the sample
            SampleTile sampleTile = objectiveSample.getTileByName(tileGroup.getTag());
            if (sampleTile==null) {
            	logger.info("  Existing sample does not contain tile: "+tileGroup.getTag());
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

            // Ensure each tiles references the correct LSMs
            if (!lsmIds1.equals(lsmIds2)) {
            	logger.info("  LSM sets are not the same ("+lsmIds1+"!="+lsmIds2+").");
                return false;
            }
        }
        
        if (objectiveSample.getTiles().size() != seenTiles.size()) {
            // Ensure that the sample has no extra tiles it doesn't need
        	logger.info("  Tile set sizes are not the same ("+objectiveSample.getTiles().size()+"!="+seenTiles.size()+").");
            return false;
        }
        
        logger.debug("  Tiles match!");
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
        if (imageTile.getName()==null || !StringUtils.areEqual(imageTile.getName(),tileGroup.getTag())) {
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
        logger.info("  Marking for reprocessing: "+sample.getName());
        sample.setStatus(DomainConstants.VALUE_MARKED);
        numSamplesReprocessed++;
    }
    
    private Map<String,DomainObjectAttribute> getSampleAttrs() {
        if (sampleAttrs==null) {
            logger.info("Building sample attribute map");
            this.sampleAttrs = new HashMap<>();
            for (DomainObjectAttribute attr : DomainUtils.getSearchAttributes(Sample.class)) {
                logger.info("  "+attr.getLabel()+" -> Sample."+attr.getName());
                sampleAttrs.put(attr.getLabel(), attr);
            }
        }
        return sampleAttrs;
    }  
    
    private Map<String,SageField> getLsmSageFields() {
        if (lsmSageFields==null) {
            logger.info("Building LSM SAGE field map");
            this.lsmSageFields = new HashMap<>();
            for (Field field : ReflectionUtils.getAllFields(LSMImage.class)) {
                SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
                if (sageAttribute!=null) {
                    SageField attr = new SageField();
                    attr.cvName = sageAttribute.cvName();
                    attr.termName = sageAttribute.termName();
                    attr.field = field;
                    logger.info("  "+attr.getKey()+" -> LsmImage."+field.getName());
                    lsmSageFields.put(attr.getKey(), attr);
                }
            }
        }
        return lsmSageFields;
    }
    
    private Map<String,SageField> getSampleSageFields() {
        if (sampleSageFields==null) {
            logger.info("Building sample SAGE field map");
            this.sampleSageFields = new HashMap<>();
            for (Field field : ReflectionUtils.getAllFields(Sample.class)) {
                SAGEAttribute sageAttribute = field.getAnnotation(SAGEAttribute.class);
                if (sageAttribute!=null) {
                    SageField attr = new SageField();
                    attr.cvName = sageAttribute.cvName();
                    attr.termName = sageAttribute.termName();
                    attr.field = field;
                    logger.info("  "+field.getName()+" -> Sample."+field.getName());
                    sampleSageFields.put(field.getName(), attr);
                }
            }
        }
        return sampleSageFields;
    }    

    private class SageField {
        String cvName;
        String termName;
        Field field;
        public String getKey() {
            return cvName+"_"+termName;
        }
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

            logger.debug("  Calculating number of channels in tile "+tileGroup.getTag());
            
            int tileNumSignals = 0;
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
     * Go through a sample area's tiles and look for a concatenated LSM attribute with a given name. If a consensus can 
     * be reached across all the Tiles in the area, then return that consensus. Otherwise log a warning and return null.
     * @param sampleArea
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusTileAttributeValue(AnatomicalArea sampleArea, String attrName, String delimiter) throws Exception {
        List<AnatomicalArea> sampleAreas = new ArrayList<>();
        sampleAreas.add(sampleArea);
        return getConsensusTileAttributeValue(sampleAreas, attrName, delimiter);
    }

    /**
     * Go through a set of sample areas' tiles and look for an attribute with a given name. If a consensus
     * can be reached across all the LSM's in the area then return that consensus. Otherwise log a warning and return null.
     * @param attrName
     * @return
     * @throws Exception
     */
    public String getConsensusTileAttributeValue(List<AnatomicalArea> sampleAreas, String attrName, String delimiter) throws Exception {
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
        	for(SampleTile sampleTile : getTilesForArea(objectiveSample, sampleArea)) {
        	    logger.trace("    Determining consensus for "+attrName+" in "+sampleTile.getName()+" tile");
            	List<LSMImage> lsms = domainDao.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
	        	
            	StringBuilder sb = new StringBuilder();
                for(LSMImage image : lsms) {
                    Object value = DomainUtils.getAttributeValue(image, attrName);
                    if (sb.length()>0) sb.append(delimiter);
                    if (value!=null) sb.append(value);
                }
                
                String tileValue = sb.toString();
                if (consensus!=null && !StringUtils.areEqual(consensus,tileValue)) {
                    logger.warn("No consensus for attribute '"+attrName+"' can be reached for sample area "+sampleArea.getName());
                    return null;
                }
                else {
                    consensus = tileValue==null?null:tileValue.toString();
                }
        	}
        
        }
        return consensus;
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
                for(SampleTile sampleTile : getTilesForArea(objectiveSample, sampleArea)) {
        	    logger.trace("    Determining consensus for "+attrName+" in "+sampleTile.getName()+" tile");
            	List<LSMImage> lsms = domainDao.getDomainObjectsAs(sampleTile.getLsmReferences(), LSMImage.class);
            	
                for(LSMImage image : lsms) {
    	        	logger.trace("      Determining consensus for "+attrName+" in "+image.getName()+" LSM");
                    Object value = DomainUtils.getAttributeValue(image, attrName);
                    if (consensus!=null && !StringUtils.areEqual(consensus,value)) {
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

    public List<SampleTile> getTilesForArea(ObjectiveSample objectiveSample, AnatomicalArea area) {
        List<SampleTile> tiles = new ArrayList<>();
        for(SampleTile tile : objectiveSample.getTiles()) {
            if (area.getName().equals(tile.getAnatomicalArea()) && area.getTileNames().contains(tile.getName())) {
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
        separation.setName(resultName);
        separation.setFiles(new HashMap<FileType,String>());

        ReverseReference fragmentsReference = new ReverseReference();
        fragmentsReference.setReferringClassName(NeuronFragment.class.getSimpleName());
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
        error.setCreationDate(new Date());
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

    public List<FileGroup> createFileGroups(HasFilepath parent, List<String> filepaths) throws Exception {

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
            	key = FilenameUtils.getBaseName(name);
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
                group = new FileGroup(key);
                group.setFilepath(parent.getFilepath());
                group.setFiles(new HashMap<FileType,String>());
                groups.put(key, group);
            }
            
            DomainUtils.setFilepath(group, fileType, filepath);
        }

        return new ArrayList<>(groups.values());
    }

    public void sortMembersByName(TreeNode folder) throws Exception {
        if (folder ==null || !folder.hasChildren()) return;
        final Map<Long,DomainObject> map = DomainUtils.getMapById(domainDao.getChildren(ownerKey, folder));
        Collections.sort(folder.getChildren(), new Comparator<Reference>() {
            @Override
            public int compare(Reference o1, Reference o2) {
                DomainObject d1 = map.get(o1.getTargetId());
                DomainObject d2 = map.get(o2.getTargetId());
                return d1.getName().compareTo(d2.getName());
            }
        });
        domainDao.save(ownerKey, folder);
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
