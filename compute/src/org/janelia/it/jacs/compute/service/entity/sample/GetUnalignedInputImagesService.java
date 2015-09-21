package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Gets all the unaligned images for the given sample as InputImages which can be used as parameters to other services. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetUnalignedInputImagesService extends AbstractEntityService {

    private String mode;
    
    public void execute() throws Exception {

        this.mode = data.getItemAsString("MODE");

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (StringUtils.isEmpty(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }

        Entity sampleEntity = entityBean.getEntityTree(new Long(sampleEntityId));
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        String pipelineRunId = (String)processData.getItem("PIPELINE_RUN_ENTITY_ID");
        if (StringUtils.isEmpty(pipelineRunId)) {
            throw new IllegalArgumentException("PIPELINE_RUN_ENTITY_ID may not be null");
        }
        
        Entity pipelineRun = entityBean.getEntityTree(new Long(pipelineRunId));
        if (pipelineRun == null) {
            throw new IllegalArgumentException("Pipeline run entity not found with id="+pipelineRunId);
        }
        
        String objective = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);

        List<InputImage> inputImages = new ArrayList<InputImage>();
        List<Entity> results = pipelineRun.getOrderedChildren();
        for(Entity result : results) {
            if (result.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                inputImages.add(getInputImage(result, objective));    
            }
        }
        
        Collections.sort(inputImages, new Comparator<InputImage>() {
            @Override
            public int compare(InputImage o1, InputImage o2) {
                return ComparisonChain.start()
                        .compare(o1.getArea(), o2.getArea(), Ordering.natural()) // Brain before VNC
                        .compare(o1.getFilepath(), o2.getFilepath(), Ordering.natural().nullsLast()).result();
            }
        });
        
        StringBuilder sb = new StringBuilder();
        for(InputImage inputImage : inputImages) {
            if (sb.length()>0) sb.append(",");
            sb.append(inputImage.getArea());
        }
    
        boolean normalizeToFirst = sb.toString().equals("Brain,VNC");
        boolean runBasic = "20x".equals(objective);
        
        logger.info("Putting "+normalizeToFirst+" images into NORMALIZE_TO_FIRST_IMAGE");
        processData.putItem("NORMALIZE_TO_FIRST_IMAGE", Boolean.valueOf(normalizeToFirst));
        logger.info("Putting "+runBasic+" images into RUN_BASIC");
        processData.putItem("RUN_BASIC", Boolean.valueOf(runBasic));
    	logger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(Entity resultEntity, String objective) throws ComputeException {
        
        String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        
        Entity image = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String chanSpec = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        String prefix = FileUtils.getFilePrefix(filepath);
        String colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
        
        
        // TODO: move color specs to process files
        
        if ("20x".equals(objective)) {
            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "M1R", "G");
        }
        else if ("63x".equals(objective)) {
            if ("polarity".equals(mode)) {
                if (chanSpec.length()==2) {
                    colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "G", "1");
                }
                else {
                    colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "MG", "1");
                }
            }
        }
                
        logger.info("Input file: "+filepath);
        logger.info("  Area: "+area);
        logger.info("  Channel specification: "+chanSpec);
        logger.info("  Color specification: "+colorspec);
        logger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setArea(area);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec("");
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
