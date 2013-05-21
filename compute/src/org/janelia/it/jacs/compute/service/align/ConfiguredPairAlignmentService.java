package org.janelia.it.jacs.compute.service.align;

import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
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
    protected void init(IProcessData processData) throws Exception {
        super.init(processData);
        try {
            if (Objective.OBJECTIVE_63X.getName().equals(sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE))) {
                // Already within the 63x sample, we need the parent
                Entity parentSample = entityBean.getAncestorWithType(sampleEntity, EntityConstants.TYPE_SAMPLE);
                if (parentSample==null) {
                    throw new IllegalStateException("Parent sample is null for 63x sample: "+sampleEntity.getId());
                }
                initParameters(parentSample);
            }
            else {
                initParameters(sampleEntity);    
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void initParameters(Entity sample) throws Exception {

        entityLoader.populateChildren(sample);
        for(Entity objectiveSample : EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_SAMPLE)) {
            
            String objective = objectiveSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                logger.info("Found 20x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, BRAIN_AREA);
                if (result != null) {
                    Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (image!=null) {
                        input2 = new AlignmentInputFile();
                        input2.setPropertiesFromEntity(image);
                        if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                        logInputFound("second input (20x stack)", input2);
                    }
                    else {
                        logger.error("Could not find default 3d image for result "+result.getName()+" (id="+result.getId()+")");
                    }
                }
            }
            else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                logger.info("Found 63x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, null);
                if (result!=null) {
                    Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (image!=null) {
                        input1 = new AlignmentInputFile();
                        input1.setPropertiesFromEntity(image);
                        if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                        logInputFound("first input (63x stack)", input1);
                    }
                    else {
                        logger.error("Could not find default 3d image for result "+result.getName()+" (id="+result.getId()+")");
                    }
                }

                this.gender = sampleHelper.getConsensusLsmAttributeValue(objectiveSample, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
                if (gender!=null) {
                    logger.info("Found gender consensus: "+gender);
                }
            }
        }

        if (input1!=null) {
            putOutputVars(input1.getChannelSpec(), input1.getChannelColors());
        }
    }
    
    private Entity getLatestResultOfType(Entity objectiveSample, String resultType, String anatomicalArea) throws Exception {
        entityLoader.populateChildren(objectiveSample);

        logger.info("Looking for latest result of type "+resultType+" with anatomicalArea="+anatomicalArea);
        
        List<Entity> pipelineRuns = EntityUtils.getChildrenOfType(objectiveSample, EntityConstants.TYPE_PIPELINE_RUN);
        Collections.reverse(pipelineRuns);
        for(Entity pipelineRun : pipelineRuns) {
            entityLoader.populateChildren(pipelineRun);
            
            logger.info("  Check pipeline run "+pipelineRun.getName()+" (id="+pipelineRun.getId()+")");
            
            List<Entity> results = EntityUtils.getChildrenForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT);
            Collections.reverse(results);
            for(Entity result : results) {

                logger.info("    Check result "+result.getName()+" (id="+result.getId()+")");
                
                if (result.getEntityType().getName().equals(resultType)) {
                    if (anatomicalArea==null || anatomicalArea.equalsIgnoreCase(result.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA))) {
                        entityLoader.populateChildren(result);
                        if (EntityUtils.findChildWithType(result, EntityConstants.TYPE_ERROR) == null) {
                            return result;
                        }
                    }
                }
            }   
        }
        return null;
    }
}
