package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.neuronSeparator.NeuronMappingGridService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
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
public class MigrationNeuronAnnotationsService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(MigrationNeuronAnnotationsService.class);
    
    public void execute() throws Exception {

        Long sourceSeparationId = data.getRequiredItemAsLong("SOURCE_SEPARATION_ID");
        Long targetSeparationId = data.getRequiredItemAsLong("TARGET_SEPARATION_ID");
        boolean targetIsWarped = data.getItemAsBoolean("TARGET_IS_WARPED"); // default to false
        
        Entity sourceSeparation = entityBean.getEntityTree(sourceSeparationId);
        if (sourceSeparation == null) {
            throw new IllegalArgumentException("Source separation entity not found with id="+sourceSeparationId);
        }

        logger.info("Retrieved source separation: "+sourceSeparation.getName()+" (id="+sourceSeparationId+")");
        
        Entity targetSeparation = entityBean.getEntityTree(targetSeparationId);
        if (targetSeparation == null) {
            throw new IllegalArgumentException("Target separation entity not found with id="+targetSeparation);
        }

        logger.info("Retrieved target separation: "+targetSeparation.getName()+" (id="+targetSeparationId+")");

        migrateAnnotations(sourceSeparation, targetSeparation, targetIsWarped);
    }
    
    /**
     * Check for Samples with the old result structure.
     */
    private void migrateAnnotations(Entity sourceSeparation, Entity targetSeparation, boolean targetIsWarped) throws Exception {
          
        populateChildren(sourceSeparation);
        populateChildren(targetSeparation);
        
        Entity supportingFiles = EntityUtils.getSupportingData(targetSeparation);
        populateChildren(supportingFiles);
        
        boolean resultWasMapped = EntityUtils.findChildWithName(supportingFiles, "SeparationResult.nsp")!=null; 
        
        Entity sourceNeuronCollection = EntityUtils.findChildWithType(sourceSeparation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        Entity targetNeuronCollection = EntityUtils.findChildWithType(targetSeparation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        
        List<Entity> sourceAnnotations = getAnnotationsForChildren(sourceNeuronCollection);
        List<Entity> targetAnnotations = getAnnotationsForChildren(targetNeuronCollection);
        
        if (!targetAnnotations.isEmpty()) {
            logger.warn("Target separation ("+targetSeparation.getId()+") already has neuron annotations ("+targetAnnotations.size()+").");
            // TODO: is this an error condition, or should we just warn and proceed?
            logger.warn("Cannot proceed with annotation migration to annotated separation");
            return;
        }
        
        if (sourceAnnotations.isEmpty()) {
            logger.info("Source separation ("+sourceSeparation.getId()+") has no neuron annotations to migrate");
            return;
        }

        logger.info("Migrating annotations from: "+sourceSeparation.getId()+" to "+targetSeparation.getId());
        
        if (targetIsWarped) {
            int ns = sourceNeuronCollection.getChildren().size();
            int nt = targetNeuronCollection.getChildren().size();
            if (ns != nt) {
                throw new IllegalStateException("Target separation was warped, but does not contain the same number of neurons as the source ("+ns+"!="+nt+")");
            }
            Map<Integer,Integer> mapping = generateOneToOneMapping(ns);
            migrateAnnotations(sourceNeuronCollection, targetNeuronCollection, sourceAnnotations, targetAnnotations, mapping);
            
        }
        else {
            Map<Integer,Integer> mapping = getMapping(targetSeparation, sourceSeparation.getId(), resultWasMapped);
            if (mapping==null) {
                throw new IllegalStateException("Unwarped separation has no mapping file: "+targetSeparation.getId());
            }
            logger.info("Retrieved "+mapping.size()+" neuron mappings");
            migrateAnnotations(sourceNeuronCollection, targetNeuronCollection, sourceAnnotations, targetAnnotations, mapping);
        }    
    }
    
    /**
     * Return a mapping from the neuron entity ids to the neuron numbers, or vice-versa if invertMap is true.
     */
    private Map<Number,Number> getNeuronNumMap(Entity neuronCollection, boolean invertMap) {
        Map<Number,Number> neuronNumMap = new HashMap<Number,Number>();
        for(Entity neuron : EntityUtils.getChildrenOfType(neuronCollection, EntityConstants.TYPE_NEURON_FRAGMENT)) {
            String numberStr = neuron.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUMBER);
            if (numberStr!=null) {
                Integer number = new Integer(numberStr);
                if (invertMap) {
                    neuronNumMap.put(number, neuron.getId());
                }
                else {
                    neuronNumMap.put(neuron.getId(), number);
                }
            }
        }
        return neuronNumMap;
    }
    
    /**
     * Migrate the annotations between two collection of neurons, given a neuron number mapping.
     */
    private void migrateAnnotations(Entity sourceNeuronCollection, Entity targetNeuronCollection, 
            List<Entity> sourceAnnotations, List<Entity> targetAnnotations, Map<Integer,Integer> mapping) 
                    throws ComputeException {

        Map<Number,Number> sourceNeuronNumByEntityId = getNeuronNumMap(sourceNeuronCollection, false);
        Map<Number,Number> targetEntityIdByNeuronNum = getNeuronNumMap(targetNeuronCollection, true);
        
        Multimap<Long,Entity> sourceAnnotationMap = getAnnotationMap(sourceAnnotations);
        
        for(Long neuronEntityId : sourceAnnotationMap.keySet()) {
            Collection<Entity> annotations = sourceAnnotationMap.get(neuronEntityId);
            if (annotations==null) {
                logger.info("Neuron (id="+neuronEntityId+") has null annotation");
                continue;
            }
            logger.info("Neuron (id="+neuronEntityId+") has "+annotations.size()+" annotations");
            Number sourceNumber = sourceNeuronNumByEntityId.get(neuronEntityId);
            if (sourceNumber==null) {
                logger.info("Neuron (id="+neuronEntityId+") has null number");
                continue;
            }
            logger.info("Neuron (id="+neuronEntityId+") is number "+sourceNumber+" in the source separation");
            Integer targetNumber = mapping.get(sourceNumber);
            if (targetNumber==null) {
                logger.info("Neuron (number="+sourceNumber+") has no mapping in the target separation");
                continue;
            }
            logger.info("Neuron (id="+neuronEntityId+") is number "+targetNumber+" in the target separation");
            Long targetEntityId = (Long)targetEntityIdByNeuronNum.get(targetNumber);
            if (targetEntityId==null) {
                logger.info("Neuron (number="+sourceNumber+") has no entity in the target separation");
                continue;
            }
            logger.info("Neuron (id="+neuronEntityId+") is neuron (id="+targetEntityId+") in the target separation");
            migrateAnnotations(annotations, targetEntityId);
        }
    }
    
    /**
     * Copy the given annotations to the new target, and mark them computational.
     */
    private void migrateAnnotations(Collection<Entity> annotations, Long targetId) throws ComputeException {
        for(Entity annotation : annotations) {
            logger.info("Migrating annotation "+annotation.getName()+" to "+targetId);
            migrateAnnotation(annotation, targetId);    
        }
    }
    
    /**
     * Copy the given annotation to the given target, and mark it computational.
     * @param annotationEntity
     * @param targetId
     * @throws ComputeException
     */
    private void migrateAnnotation(Entity annotationEntity, Long targetId) throws ComputeException {
        OntologyAnnotation annotation = new OntologyAnnotation();
        annotation.init(annotationEntity);
        annotation.setEntity(null);
        annotation.setId(null);
        annotation.setIsComputational(true);
        annotation.setTargetEntityId(targetId);
        annotationBean.createOntologyAnnotation(ownerKey, annotation);
    }
    
    /**
     * Returns the annotations for the children of a given enity.
     */
    private List<Entity> getAnnotationsForChildren(Entity entity) throws ComputeException {
        return annotationBean.getAnnotationsForChildren(ownerKey, entity.getId());
    }
     
    /**
     * Returns a multimap from entity to list of annotations which target that entity.
     */
    private Multimap<Long,Entity> getAnnotationMap(List<Entity> neuronAnnotations) {

        Multimap<Long,Entity> neuronAnnotationMap = HashMultimap.<Long,Entity>create();
        
        for(Entity annotation : neuronAnnotations) {
            String targetIdStr = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID);
            if (targetIdStr!=null) {
                Long targetId = Long.parseLong(targetIdStr);
                neuronAnnotationMap.put(targetId, annotation);
            }
        }
        
        return neuronAnnotationMap;
    }

    /**
     * Generate a simple one-to-one mapping of any given size.
     */
    private Map<Integer,Integer> generateOneToOneMapping(int size) {
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
    private Map<Integer,Integer> getMapping(Entity separation, Long sourceSeparationId, boolean useMappedIndicies) {
        String dir = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

        File separationDir = new File(dir);
        File[] mappingFiles = FileUtil.getFilesWithPrefixes(separationDir, NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX);
                
    	File mappingFile = null;
    	for(File file : mappingFiles) {
    		if (file.getName().startsWith(NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX+"_"+sourceSeparationId)) {
    			mappingFile = file;
    		}
    	}
    	
    	for(File file : mappingFiles) {
    		if (file.getName().equals(NeuronMappingGridService.MAPPING_FILE_NAME_PREFIX+".txt")) {
    			mappingFile = file;
    		}
    	}
        
        Scanner scanner = null;
        
        try {
            scanner = new Scanner(mappingFile);
            scanner.useDelimiter("\\n");
            Map<Integer,Integer> mapping = new HashMap<Integer,Integer>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                Pattern p = Pattern.compile("^Previous index (.*?) : Unmapped index (.*?) : Mapped index (.*?)$");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String prevIndex = m.group(1);
                    String unmappedIndex = m.group(2);
                    String mappedIndex = m.group(3);
                    String newIndex = useMappedIndicies?mappedIndex:unmappedIndex;
                    try {
                        mapping.put(Integer.parseInt(prevIndex), Integer.parseInt(newIndex));
                    }
                    catch (NumberFormatException e) {
                        logger.warn("Could not format indexes: "+prevIndex+"->"+newIndex);
                    }
                }
            }
            return mapping;
        }
        catch (Exception e) {
            logger.warn("Could not read mapping file: "+mappingFile, e);
            return null;
        }
        finally {
            if (scanner!=null) scanner.close();            
        }
    }
}
