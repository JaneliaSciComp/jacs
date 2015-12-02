package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronMappingGridService;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A service which has several modes to be called from the CleanupRetiredData pipeline. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CleanupRetiredDataService extends AbstractEntityService {

	private static final String MODE_CREATE_SEPARATION_PAIRS = "CREATE_SEPARATION_PAIRS";
    private static final String MODE_CLEAN_UP = "CLEAN_UP";
    private static final String OWNER = "nerna";
    private static final String GROUP = "flylight";
    private static final String OWNER_KEY = "user:"+OWNER;
    private static final String GROUP_KEY = "group:"+GROUP;
    private static final String MAPPING_ROOT = "Annotation Migration";
    private static final String TARGETS_ROOT = "Migration Targets";
    
    private String mode = null;

	private Multimap<String, Entity> sampleMap = HashMultimap.<String,Entity>create();
	private Set<Long> retiredSampleSet = new HashSet<Long>();
	
    public void execute() throws Exception {
	
        mode = data.getRequiredItemAsString("MODE");

        if (mode.equals(MODE_CREATE_SEPARATION_PAIRS)) {
            doCreateSampleAndSeparationPairs();
        }
        else if (mode.equals(MODE_CLEAN_UP)) {
            doCleanup();
        }
        else {
            logger.error("Unrecognized mode: "+mode);
        }
    }
    
    private void doCreateSampleAndSeparationPairs() throws ComputeException {

    	List<LongPair> sampleIdPairs = new ArrayList<LongPair>();
    	List<LongPair> separationIdPairs = new ArrayList<LongPair>();
    	List<LongPair> separationIdPairsNeedingMapping = new ArrayList<LongPair>();
    	
    	contextLogger.info("Finding all samples...");
		addSamples(sampleMap, entityBean.getUserEntitiesByTypeName(OWNER_KEY, "Sample"));
		addSamples(sampleMap, entityBean.getUserEntitiesByTypeName(GROUP_KEY, "Sample"));
		
		contextLogger.info("Finding retired samples...");
		addRetiredSamples(retiredSampleSet, getRootEntity(OWNER_KEY, "Retired Data"));
		addRetiredSamples(retiredSampleSet, getRootEntity(GROUP_KEY, "Retired Data"));

		List<String> slideCodes = new ArrayList<String>(sampleMap.keySet());
		Collections.sort(slideCodes);
		
		for(String slideCode : slideCodes) {
		
		    Collection<Entity> samples = sampleMap.get(slideCode);
	        if (samples.isEmpty()) continue;
        	
		    boolean hasRetired = false;
		    for(Entity sample : samples) {
		        if (retiredSampleSet.contains(sample.getId())) {
		            hasRetired = true;
		            break;
		        }
		    }
		
		    if (!hasRetired) continue;
							
	        List<Entity> activeSamples = new ArrayList<Entity>();
	        List<Entity> retiredSamples = new ArrayList<Entity>();
	        for(Entity sample : samples) {
	            if (retiredSampleSet.contains(sample.getId())) {
	                retiredSamples.add(sample);
	            }
	            else {
	                activeSamples.add(sample);
	            }
	        }
	
	        Multimap<Entity, Entity> sampleMapping = HashMultimap.<Entity,Entity>create();
	        for(Entity retiredSample : retiredSamples) {
	            for(Entity activeSample : activeSamples) {
	                if (lsmSetsMatch(retiredSample, activeSample)) {
	                    sampleMapping.put(retiredSample, activeSample);
	                }
	            }
	        }
	        
        	contextLogger.info("Processing "+slideCode);
	        contextLogger.info("  Found "+activeSamples.size()+" active samples");
	        contextLogger.info("  Found "+retiredSamples.size()+" retired samples");
	        contextLogger.info("  Mapped "+sampleMapping.size()+" retired samples to active samples");
        	contextLogger.info("  Processing active samples...");
        	
	        for(Entity sample : samples) {

	            Collection<Entity> targetSamples = sampleMapping.get(sample);
	            if (targetSamples.isEmpty()) continue;

	            if (targetSamples.size()>1) {
	            	logger.warn("More than one sample matches LSM set for "+sample.getName());
	            }
	            
	            loadChildren(sample);
	
	            Entity separation = getLatestSeparation(sample);
	            
	            for(Entity targetSample : targetSamples) {
		            Entity targetSeparation = getLatestSeparation(targetSample);
		            if (targetSeparation!=null) {

		            	contextLogger.info(sample.getName()+" -> "+targetSample.getName()+" ("+separation.getId()+"->"+targetSeparation.getId()+")");
		            	
		            	LongPair samplePair = new LongPair(sample.getId(), targetSample.getId());
		            	LongPair separationPair = new LongPair(separation.getId(), targetSeparation.getId());

		            	sampleIdPairs.add(samplePair);
		            	separationIdPairs.add(separationPair);
		            	
		                String transferDir = targetSeparation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		            	File targetFile = new File(transferDir, NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX+"_"+separation.getId()+".txt");
		            	if (targetFile.exists()) {
		            		contextLogger.info("Mapping file already exists: "+targetFile.getAbsolutePath());
		            	}
		            	else {
		            		separationIdPairsNeedingMapping.add(separationPair);
		            	}
		            }
		            else {
		            	contextLogger.info("Retired sample has no neuron separation: "+sample.getName());
		            }
	            }
				
	            // free memory
	            sample.setEntityData(null);
	        }
		}    	

		data.putItem("SEPARATION_PAIR_NEEDING_MAPPING", separationIdPairsNeedingMapping);
		data.putItem("SEPARATION_PAIR", separationIdPairs);
		data.putItem("SAMPLE_PAIR", sampleIdPairs);
    }
    
    private Entity getLatestSeparation(Entity sample) {

    	try {
	    	entityLoader.populateChildren(sample);
	    	Entity pipelineRun = EntityUtils.getLatestChildOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
	    	if (pipelineRun!=null) {
	    		entityLoader.populateChildren(pipelineRun);
	        	Entity result = EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
	        	if (pipelineRun!=null) {
	        		entityLoader.populateChildren(result);
	            	Entity separation = EntityUtils.getLatestChildOfType(result, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
	            	return separation;
	        	}	
	    	}
    	}
    	catch (Exception e) {
    		logger.warn("Error getting latest separation",e);
    	}
    	
    	return null;
    }

	private void addSamples(Multimap<String, Entity> sampleMap, Collection<Entity> samples) {
	    for(Entity sample : samples) {
	        SampleReportSampleInfo info = new SampleReportSampleInfo(sample);
	        sampleMap.put(info.slide_code, sample);
	    }
	}
	
	private void addRetiredSamples(Set<Long> retiredSampleSet, Entity retiredSampleFolder) throws ComputeException {
	    loadChildren(retiredSampleFolder);
	    for(Entity child : retiredSampleFolder.getChildren()) {
	        retiredSampleSet.add(child.getId());
	    }
	}

    private boolean lsmSetsMatch(Entity sample1, Entity sample2) throws ComputeException {
        Set<String> set1 = getLsmSet(sample1);
        Set<String> set2 = getLsmSet(sample2);
        logger.trace("  Comparing LSM sets:");
        logger.trace("    Set1: "+set1);
        logger.trace("    Set2: "+set2);
        return set1.containsAll(set2) && set2.containsAll(set1);
    }

    private Set<String> getLsmSet(Entity sample) throws ComputeException {
        Set<String> lsmSet = new HashSet<String>();
        loadChildren(sample);
        Entity supportingData = EntityUtils.getSupportingData(sample);
        loadChildren(supportingData);
        if (supportingData != null) {
            for(Entity imageTile : EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)) {
                loadChildren(imageTile);
                for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                	lsmSet.add(ArchiveUtils.getDecompressedFilepath(lsm.getName()));
                }
            }
        }
        return lsmSet;
    }

	private Entity loadChildren(Entity entity) throws ComputeException {
		if (entity==null || entity.getId()==null) return entity;
		EntityUtils.replaceChildNodes(entity, entityBean.getChildEntities(null, entity.getId()));
		return entity;
	}

    private Entity getRootEntity(String ownerKey, String topLevelFolderName) throws ComputeException {
        for(Entity commonRoot : annotationBean.getCommonRootEntities(ownerKey)) {
            if (commonRoot.getName().equals(topLevelFolderName)) {
                return commonRoot;
            }
        }
        return null;
    }
    
	private class SampleReportSampleInfo {
	    String sampleName = "";
	    String line = "";
	    String slide_code = "";
	    String objective = "";
	
	    SampleReportSampleInfo(Entity sample) {
	
	        sampleName = sample.getName();
	        String[] parts = sampleName.split("~");
	
	        if (parts.length>1) {
	            sampleName = parts[0];
	            objective = parts[1];
	        }
	
	        sampleName = sampleName.replaceFirst("-Retired", "");
	        int startOfSlideCode = sampleName.lastIndexOf("-20");
	        if (startOfSlideCode<0) {
	            // Try to find typo'd slide codes
	            startOfSlideCode = sampleName.lastIndexOf("-10");
	        }
	
	        if (startOfSlideCode>0) {
	            line = sampleName.substring(0,startOfSlideCode);
	            slide_code = sampleName.substring(startOfSlideCode+1);
	            Pattern p = Pattern.compile("(\\d{8}_\\d+_\\w{2}).*");
	            Matcher m = p.matcher(slide_code);
	            if (m.matches()) {
	                slide_code = m.group(1);
	            }
	        }
	        else {
	            line = "";
	            slide_code = sampleName;
	        }
	    }
	}
    
    private void doCleanup() throws Exception {

    	Entity rootEntity = entityHelper.createOrVerifyRootEntity(MAPPING_ROOT, true, false);
    	
    	List<LongPair> sampleIdPairs = (ArrayList<LongPair>)data.getRequiredItem("SAMPLE_PAIR");

    	List<Long> targetIds = new ArrayList<Long>();
    	
    	for(LongPair sampleIdPair : sampleIdPairs) {
    		Long retiredSampleId = sampleIdPair.getLong1();
    		Long targetSampleId = sampleIdPair.getLong2();
    		
    		Entity targetSample = entityBean.getEntityById(targetSampleId);
    		Entity groupFolder = entityHelper.verifyOrCreateChildFolder(rootEntity, targetSample.getName());
    		
    		List<Long> childrenIds = new ArrayList<Long>();
    		childrenIds.add(retiredSampleId);
    		childrenIds.add(targetSampleId);
    		entityBean.addChildren(ownerKey, groupFolder.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
    		
    		targetIds.add(targetSampleId);
    	}
    	
    	Entity targetsEntity = entityHelper.createOrVerifyRootEntity(TARGETS_ROOT, true, false);
    	entityBean.addChildren(ownerKey, targetsEntity.getId(), targetIds, EntityConstants.ATTRIBUTE_ENTITY);
    }
	
    public class LongPair {
    	
    	private Long long1;
    	private Long long2;
    	
		public LongPair(Long long1, Long long2) {
			this.long1 = long1;
			this.long2 = long2;
		}
		
		public Long getLong1() {
			return long1;
		}
		public void setLong1(Long long1) {
			this.long1 = long1;
		}
		public Long getLong2() {
			return long2;
		}
		public void setLong2(Long long2) {
			this.long2 = long2;
		}
    }
}