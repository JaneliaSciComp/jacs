package org.janelia.it.jacs.compute.service.align;

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
        for(Entity objectiveSample : sample.getChildren()) {
            
            String objective = objectiveSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
            if (Objective.OBJECTIVE_20X.getName().equals(objective)) {
                logger.info("Found 20x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_ALIGNMENT_RESULT);
                if (result != null) {
                    Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (image!=null) {
                        input2 = new AlignmentInputFile();
                        input2.setPropertiesFromEntity(image);
                        if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                        logger.info("Found 20x aligned stack: "+input2.getInputFilename());
                        logInputFound("first input (20x aligned stack)", input2);
                    }
                }
            }
            else if (Objective.OBJECTIVE_63X.getName().equals(objective)) {
                logger.info("Found 63x sub-sample: "+objectiveSample.getName());
                Entity result = getLatestResultOfType(objectiveSample, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
                if (result!=null) {
                    Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                    if (image!=null) {
                        input1 = new AlignmentInputFile();
                        input1.setPropertiesFromEntity(image);
                        if (warpNeurons) input2.setInputSeparationFilename(getConsolidatedLabel(result));
                        logInputFound("second input (63x stack)", input1);
                    }
                }

                this.gender = sampleHelper.getConsensusLsmAttributeValue(objectiveSample, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
                if (gender!=null) {
                    logger.info("Found gender consensus: "+gender);
                }
            }
        }
    }
    
    private Entity getLatestResultOfType(Entity objectiveSample, String resultType) throws Exception {
        entityLoader.populateChildren(objectiveSample);
        for(Entity pipelineRun : objectiveSample.getOrderedChildren()) {
            entityLoader.populateChildren(pipelineRun);
            if (pipelineRun.getEntityType().getName().equals(EntityConstants.TYPE_PIPELINE_RUN)) {
                for(Entity a : pipelineRun.getChildren()) {
                    entityLoader.populateChildren(a);
                    if (a.getEntityType().getName().equals(resultType)) {
                        if (EntityUtils.findChildWithType(a, EntityConstants.TYPE_ERROR) == null) {
                            return a;
                        }
                    }
                }
            }
        }
        return null;
    }
}
