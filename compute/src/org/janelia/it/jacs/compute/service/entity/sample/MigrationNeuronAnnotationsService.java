package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronSeparationPipelineGridService;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Migrate annotations on neuron fragments between two neuron separation results. This service attempts to 
 * migrate the annotations using different methods, but it does not determine if annotations SHOULD be migrated.
 * That determination is left to the invoking service. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MigrationNeuronAnnotationsService extends AbstractDomainService {

    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;
	private String resultComment;
	
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);

        Long sourceSeparationId = data.getRequiredItemAsLong("SOURCE_SEPARATION_ID");
        Long targetSeparationId = data.getRequiredItemAsLong("TARGET_SEPARATION_ID");
        boolean targetIsWarped = data.getItemAsBoolean("TARGET_IS_WARPED"); // default to false
        
        List<NeuronSeparation> sourceSeparations = objectiveSample.getResultsById(NeuronSeparation.class, sourceSeparationId);
        if (sourceSeparations.isEmpty()) {
            throw new IllegalArgumentException("Source separation entity not found with id="+sourceSeparationId);
        }

        NeuronSeparation sourceSeparation = sourceSeparations.get(sourceSeparations.size()-1);
        contextLogger.info("Retrieved source separation: "+sourceSeparation.getName()+" (id="+sourceSeparationId+")");
        
        List<NeuronSeparation> targetSeparations = objectiveSample.getResultsById(NeuronSeparation.class, sourceSeparationId);
        if (targetSeparations.isEmpty()) {
            throw new IllegalArgumentException("Target separation entity not found with id="+targetSeparations);
        }

        NeuronSeparation targetSeparation = targetSeparations.get(targetSeparations.size()-1);
        contextLogger.info("Retrieved target separation: "+targetSeparation.getName()+" (id="+targetSeparationId+")");

        migrateAnnotations(sourceSeparation, targetSeparation, targetIsWarped);
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void migrateAnnotations(NeuronSeparation sourceSeparation, NeuronSeparation targetSeparation, boolean targetIsWarped) throws Exception {
        
        boolean resultWasMapped = DomainUtils.getFilepath(targetSeparation, FileType.NeuronSeparatorResult)!=null;
        
        List<NeuronFragment> sourceFragments = domainDao.getNeuronFragmentsBySeparationId(ownerKey, sourceSeparation.getId());
        List<NeuronFragment> targetFragments = domainDao.getNeuronFragmentsBySeparationId(ownerKey, targetSeparation.getId());
        
        Multimap<Integer,Annotation> sourceAnnotations = getNeuronAnnotations(sourceFragments);
        Multimap<Integer,Annotation> targetAnnotations = getNeuronAnnotations(targetFragments);
        
        if (sourceAnnotations.isEmpty()) {
            contextLogger.info("Source separation ("+sourceSeparation.getId()+") has no neuron annotations to migrate");
            return;
        }

        contextLogger.info("Migrating annotations from: "+sourceSeparation.getId()+" to "+targetSeparation.getId());
        
        if (targetIsWarped) {
            Long ns = sourceSeparation.getFragmentsReference().getCount();
            Long nt = targetSeparation.getFragmentsReference().getCount();
            if (ns != nt) {
                throw new IllegalStateException("Target separation was warped, but does not contain the same number of neurons as the source ("+ns+"!="+nt+")");
            }
            Map<Integer,Integer> mapping = generateOneToOneMapping(ns);
            migrateAnnotations(sourceFragments, targetFragments, sourceAnnotations, targetAnnotations, mapping);
            
        }
        else {
            Map<Integer,Integer> mapping = getMapping(targetSeparation, sourceSeparation.getId(), resultWasMapped);
            if (mapping!=null) {
                contextLogger.info("Retrieved "+mapping.size()+" neuron mappings");
	            if (!mapping.isEmpty()) {
	                
	            	migrateAnnotations(sourceFragments, targetFragments, sourceAnnotations, targetAnnotations, mapping);
	            }
	            else if (resultComment == null){
	            	resultComment = "No neuron mappings";
	            }
            }
            else if (resultComment == null){
            	resultComment = "No neuron mapping";
            }
        }    
    }

    public Multimap<Integer,Annotation> getNeuronAnnotations(List<NeuronFragment> fragments) {
        Map<Number,Number> sourceNeuronNumByEntityId = getNeuronNumMap(fragments, false);
        Multimap<Integer,Annotation> map = HashMultimap.<Integer,Annotation>create();
        for(Annotation annotation : domainDao.getAnnotations(null, DomainUtils.getReferences(fragments))) {
            Number fragmentIndex = sourceNeuronNumByEntityId.get(annotation.getTarget().getTargetId());
            map.put(fragmentIndex.intValue(), annotation);
        }
        return map;
    }
    
    public Map<Number, Number> getNeuronNumMap(List<NeuronFragment> fragments, boolean invert) {
        Map<Number, Number> fragmentIndexMap = new HashMap<>();
        for(NeuronFragment fragment : fragments) {
            if (invert) {
                fragmentIndexMap.put(fragment.getNumber(), fragment.getId());  
            }
            else {  
                fragmentIndexMap.put(fragment.getId(), fragment.getNumber());
            }
        }
        return fragmentIndexMap;
    }
    
    /**
     * Migrate the annotations between two collection of neurons, given a neuron number mapping.
     */
    private void migrateAnnotations(List<NeuronFragment> sourceNeuronCollection, List<NeuronFragment> targetNeuronCollection, 
            Multimap<Integer,Annotation> sourceAnnotations, Multimap<Integer,Annotation> targetAnnotations, Map<Integer,Integer> mapping) 
                    throws Exception {

        Map<Number,Number> targetEntityIdByNeuronNum = getNeuronNumMap(targetNeuronCollection, true);
                
        for(Integer sourceNumber : sourceAnnotations.keySet()) {
            Collection<Annotation> annotations = sourceAnnotations.get(sourceNumber);
            if (annotations==null||annotations.isEmpty()) {
                contextLogger.warn("Neuron "+sourceNumber+" has null annotation");
                continue;
            }
            Integer targetNumber = mapping.get(sourceNumber);
            if (targetNumber==null) {
                contextLogger.warn("Neuron (number="+sourceNumber+") has no mapping in the target separation");
                continue;
            }
            Long targetEntityId = (Long)targetEntityIdByNeuronNum.get(targetNumber);
            if (targetEntityId==null) {
                contextLogger.warn("Neuron (number="+sourceNumber+") has no entity in the target separation");
                continue;
            }
            contextLogger.info("Neuron "+sourceNumber+"->"+targetNumber+" has "+annotations.size()+" annotations");
        	migrateAnnotations(annotations, targetEntityId);
        	resultComment = "Migrated "+annotations.size()+" annotations";
        }
    }
    
    /**
     * Copy the given annotations to the new target, and mark them computational.
     */
    private void migrateAnnotations(Collection<Annotation> annotations, Long targetId) throws Exception {
        for(Annotation annotation : annotations) {
            contextLogger.info("Migrating annotation "+annotation.getName()+" to "+targetId);
            migrateAnnotation(annotation, targetId);    
        }
    }
    
    /**
     * Copy the given annotation to the given target, and mark it computational.
     * @param annotationEntity
     * @param targetId
     * @throws ComputeException
     */
    private void migrateAnnotation(Annotation annotationEntity, Long targetId) throws Exception {
        Annotation annotation = new Annotation(annotationEntity);
        annotation.setTarget(new Reference(annotationEntity.getTarget().getTargetClassName(), targetId));
        domainDao.save(annotation);
    }
    
    /**
     * Generate a simple one-to-one mapping of any given size.
     */
    private Map<Integer,Integer> generateOneToOneMapping(Long size) {
        Map<Integer,Integer> mapping = new HashMap<Integer,Integer>();
        for(int i=1; i<=size; i++) {
            mapping.put(i, i);
        }
        return mapping;
    }
    
    /**
     * Returns the mapping for a given separation, as determined by its mapping_issues.txt file.
     * 
     * The mapping_issues.txt file is formatted like this:
     * Previous index # : Unmapped index # : Mapped index #
     * 
     * If useMappedIndicies is true, then this method returns this mapping:
     *     Previous index # -> Mapped index #
     *     
     * If useMappedIndicies is false, then this method returns this mapping:
     *     Previous index # -> Unmapped index #
     */
    private Map<Integer,Integer> getMapping(NeuronSeparation separation, Long sourceSeparationId, boolean useMappedIndicies) {
        String dir = separation.getFilepath();

        File separationDir = new File(dir);
        File[] mappingFiles = FileUtil.getFilesWithPrefixes(separationDir, NeuronSeparationPipelineGridService.MAPPING_FILE_NAME_PREFIX);
                
    	File mappingFile = null;
    	for(File file : mappingFiles) {
    		if (file.getName().startsWith(NeuronSeparationPipelineGridService.MAPPING_FILE_NAME_PREFIX+"_"+sourceSeparationId)) {
    			mappingFile = file;
    		}
    	}

    	if (mappingFile==null) {
    		// Accept old-style mapping files in cases where the new one is not available (this should never happen, but you never know.)
	    	for(File file : mappingFiles) {
	    		if (file.getName().equals(NeuronSeparationPipelineGridService.MAPPING_FILE_NAME_PREFIX+".txt")) {
	    			mappingFile = file;
	    		}
	    	}
    	}
    	
    	if (mappingFile==null) {
        	resultComment = "No neuron mapping file";
        	contextLogger.error("No mapping file found from source separation "+sourceSeparationId+" to target "+separation.getId());
    		return null;
    	}
        
        Scanner scanner = null;
        
        try {
            scanner = new Scanner(mappingFile);
            scanner.useDelimiter("\\n");
            Map<Integer,Integer> mapping = new HashMap<Integer,Integer>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if ("ERROR: Input volumes are not the same size".equals(line)) {
                	resultComment = "Cannot map different sized volumes";
                	contextLogger.error("Mapping failed because volumes were not the same size");
                	return null;
                }
                
                Pattern p = Pattern.compile("^Previous index (.*?) : Unmapped index (.*?) : Mapped index (.*?)$");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String prevIndex = m.group(1);
                    String unmappedIndex = m.group(2);
                    String mappedIndex = m.group(3);
                    String newIndex = useMappedIndicies?mappedIndex:unmappedIndex;
                    if ("(none)".equals(prevIndex) || "(none)".equals(newIndex)) continue;
                    try {
                        mapping.put(Integer.parseInt(prevIndex), Integer.parseInt(newIndex));
                    }
                    catch (NumberFormatException e) {
                        contextLogger.warn("Could not format indexes: "+prevIndex+"->"+newIndex);
                    }
                }
                else {
                    contextLogger.warn("Unexpected format: "+line);
                }
            }
            return mapping;
        }
        catch (Exception e) {
        	resultComment = "Could not read neuron mapping file";
            logger.warn("Could not read neuron mapping file: "+mappingFile, e);
            return null;
        }
        finally {
            if (scanner!=null) scanner.close();            
        }
    }
}
