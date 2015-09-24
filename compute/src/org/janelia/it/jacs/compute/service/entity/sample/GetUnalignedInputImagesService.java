package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
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

    private static final String SERVICE_PACKAGE = "org.janelia.it.jacs.compute.service.image";
    
    private boolean sampleNaming;
    private String colorSpec;
    private String mode;
    
    public void execute() throws Exception {

        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getRequiredItem("SAMPLE_AREAS");
        
        this.colorSpec = data.getItemAsString("OUTPUT_COLOR_SPEC");
        this.mode = data.getItemAsString("MODE");
        this.sampleNaming = data.getItemAsBoolean("SAMPLE_NAMING");
        
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
        Map<String,InputImage> areaToImage = new HashMap<>();
        
        List<Entity> results = pipelineRun.getOrderedChildren();
        for(Entity resultEntity : results) {
            String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
            if (resultEntity.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
                InputImage inputImage = getInputImage(sampleEntity, resultEntity, objective);
                areaToImage.put(area, inputImage);    
                inputImages.add(inputImage);
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
        
        // Add tile images
        List<InputImage> toAdd = new ArrayList<InputImage>();
        List<InputImage> toDelete = new ArrayList<InputImage>();
        
        for(AnatomicalArea sampleArea : sampleAreas) {
            InputImage templateImage = areaToImage.get(sampleArea.getName());
            for(MergedLsmPair mergedPair : sampleArea.getMergedLsmPairs()) {
                InputImage inputImage = getInputImage(sampleEntity, templateImage, mergedPair);
                if (inputImage.getFilepath().equals(templateImage.getFilepath())) {
                    if (!"63x".equals(objective)) {
                        // For 63x samples, we want to use the tile name instead of the area name
                        logger.info("Will replace template image: "+templateImage.getFilepath());
                        toDelete.add(templateImage);
                    }
                }
                logger.info("Adding tile image: "+inputImage.getFilepath());
                toAdd.add(inputImage);
            }
        }
        
        inputImages.removeAll(toDelete);
        inputImages.addAll(toAdd);
        
        StringBuilder sb = new StringBuilder();
        for(InputImage inputImage : inputImages) {
            if (sb.length()>0) sb.append(",");
            sb.append(inputImage.getArea());
        }
    
        boolean normalizeToFirst = sb.toString().equals("Brain,VNC");
        String serviceClassName = "20x".equals(objective) ?  "BasicMIPandMovieGenerationService" : "EnchancedMIPandMovieGenerationService";
        String serviceClass = SERVICE_PACKAGE+"."+serviceClassName;
        
        contextLogger.info("Putting "+normalizeToFirst+" into NORMALIZE_TO_FIRST_IMAGE");
        processData.putItem("NORMALIZE_TO_FIRST_IMAGE", Boolean.valueOf(normalizeToFirst));
        contextLogger.info("Putting "+serviceClass+" into SERVICE_CLASS");
        processData.putItem("SERVICE_CLASS", serviceClass);
    	contextLogger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(Entity sampleEntity, Entity resultEntity, String objective) throws ComputeException {
        
        String area = resultEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        Entity image = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        String filepath = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        String chanSpec = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        
        String prefix = FileUtils.getFilePrefix(filepath);
        if (sampleNaming) {
            String sampleName = sampleEntity.getName();
            int tilde = sampleName.indexOf('~');
            if (tilde>0) {
                sampleName = sampleName.substring(0,tilde);
            }
            if (resultEntity.getName().startsWith("stitched")) {
                prefix = sampleName+"-stitched";
            }
            else {
                prefix = sampleName+"-"+area;    
            }
            
        }
        
        String colorspec = colorSpec;
        
        if (colorspec==null) {
            logger.warn("No OUTPUT_COLOR_SPEC specified, attempting to guess based on objective and MODE...");
            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
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
        }
             
        contextLogger.info("Input file: "+filepath);
        contextLogger.info("  Area: "+area);
        contextLogger.info("  Channel specification: "+chanSpec);
        contextLogger.info("  Color specification: "+colorspec);
        contextLogger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setArea(area);
        inputImage.setChanspec(chanSpec);
        inputImage.setColorspec(colorspec);
        inputImage.setDivspec("");
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }

    private InputImage getInputImage(Entity sampleEntity, InputImage templateImage, MergedLsmPair mergedPair) throws ComputeException {
        
        String filepath = mergedPair.getMergedFilepath();

        String prefix = FileUtils.getFilePrefix(filepath);
        if (sampleNaming) {
            String sampleName = sampleEntity.getName();
            int tilde = sampleName.indexOf('~');
            if (tilde>0) {
                sampleName = sampleName.substring(0,tilde);
            }
            prefix = sampleName+"-"+mergedPair.getTag();
        }
             
        contextLogger.info("Tile input file: "+filepath);
        contextLogger.info("  Area: "+templateImage.getArea());
        contextLogger.info("  Channel specification: "+templateImage.getChanspec());
        contextLogger.info("  Color specification: "+templateImage.getColorspec());
        contextLogger.info("  Output prefix: "+prefix);
        
        InputImage inputImage = new InputImage();
        inputImage.setFilepath(filepath);
        inputImage.setArea(templateImage.getArea());
        inputImage.setChanspec(templateImage.getChanspec());
        inputImage.setColorspec(templateImage.getColorspec());
        inputImage.setDivspec("");
        inputImage.setOutputPrefix(prefix);
        
        return inputImage;
    }
}
