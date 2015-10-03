package org.janelia.it.jacs.compute.service.align;

import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.cv.Objective;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * A configured aligner which takes additional parameters to align the 63x image to a whole brain 20x image.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConfiguredPairAlignmentService extends ConfiguredAlignmentService {
        
	private static final String BRAIN_AREA = "Brain";
	
    @Override
    protected void populateInputs(List<AnatomicalArea> sampleAreas) throws Exception {
        
        if (true) throw new UnsupportedOperationException("Pair alignment is currently unsupported. Check back soon!");
        
    	alignedAreas.addAll(sampleAreas);

    	// Ignore sample areas, and get the sample pair (20x/63x)
    	Entity sample = sampleEntity;
        if (Objective.OBJECTIVE_63X.getName().equals(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE))) {
            // Already within the 63x sample, we need the parent
            Entity parentSample = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
            if (parentSample==null) {
                throw new IllegalStateException("Parent sample is null for 63x sample: "+sampleEntity.getId());
            }
            sample = parentSample;
        }
        
        entityLoader.populateChildren(sample);
        
        final List<Entity> sampleList = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE);
        contextLogger.info("initParameters: found " + sampleList.size() + " objective samples for entity " + sample);

        for(Entity objectiveSample : sampleList) {
            
            String objective = objectiveSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                contextLogger.info("Found 20x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, BRAIN_AREA);
                input2 = buildInputFromResult("second input (20x stack)", result);
            }
            else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                contextLogger.info("Found 63x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, BRAIN_AREA);
                if (result==null) {
                    // In some cases there is no "Brain" area, and the 63x LSMs have been incorrectly annotated with the tile name as the area.
                    result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, null);
                }
                input1 = buildInputFromResult("first input (63x stack)", result);
            }
        }

        if (input1==null || input2==null) {
        	runAligner = false;
        }
    }

    @Override
    protected void setLegacyConsensusValues() throws Exception {
    	// Do nothing, since we build our consensus values while populating the inputs
    }
    
    private Entity getLatestResultOfType(Entity objectiveSample, String resultType, String anatomicalArea) throws Exception {
        entityLoader.populateChildren(objectiveSample);

        contextLogger.debug("Looking for latest result of type "+resultType+" with anatomicalArea="+anatomicalArea);
        
        List<Entity> pipelineRuns = EntityUtils.getChildrenOfType(objectiveSample, EntityConstants.TYPE_PIPELINE_RUN);
        Collections.reverse(pipelineRuns);
        for(Entity pipelineRun : pipelineRuns) {
            entityLoader.populateChildren(pipelineRun);

            contextLogger.debug("  Check pipeline run "+pipelineRun.getName()+" (id="+pipelineRun.getId()+")");

            if (EntityUtils.findChildWithType(pipelineRun, EntityConstants.TYPE_ERROR) != null) {
                continue;
            }
            
            List<Entity> results = EntityUtils.getChildrenForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT);
            Collections.reverse(results);
            for(Entity result : results) {

                contextLogger.debug("    Check result "+result.getName()+" (id="+result.getId()+")");
                
                if (result.getEntityTypeName().equals(resultType)) {
                    if (anatomicalArea==null || anatomicalArea.equalsIgnoreCase(result.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA))) {
                        entityLoader.populateChildren(result);
                        return result;
                    }
                }
            }   
        }
        return null;
    }

    private AlignmentInputFile buildInputFromResult(String inputType, Entity sampleProcessingResult) throws Exception {

        if (sampleProcessingResult==null) return null;
        AlignmentInputFile inputFile = null;

        final Entity image = sampleProcessingResult.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if (image != null) {
            inputFile = new AlignmentInputFile();
            inputFile.setPropertiesFromEntity(image);
            entityLoader.populateChildren(image);
        	String losslessPath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_LOSSLESS_IMAGE);
        	if (losslessPath!=null) {
        	    // Use lossless path, if available
        	    inputFile.setFilepath(losslessPath);
        	}
            
            if (warpNeurons) {
                inputFile.setInputSeparationFilename(getConsolidatedLabel(sampleProcessingResult));
            }
            //logInputFound(inputType, inputFile);
        } 
        else {
            contextLogger.error("Could not find default 3d image for result " + sampleProcessingResult +
                                " (id=" + sampleProcessingResult.getId() + ")");
        }
        
        return inputFile;
    }
}
