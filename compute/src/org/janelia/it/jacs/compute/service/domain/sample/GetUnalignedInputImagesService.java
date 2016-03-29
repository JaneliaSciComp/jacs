package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.image.InputImage;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Gets all the unaligned images for the given sample as InputImages which can be used as parameters to other services. 
 * This generally includes all the (merged or unmerged) tile images, and the stitched image.  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetUnalignedInputImagesService extends AbstractDomainService {

    private static final String SERVICE_PACKAGE = "org.janelia.it.jacs.compute.service.image";
    
    private boolean sampleNaming;
    private String colorSpec;
    private String mode;

    private Sample sample;
    private ObjectiveSample objectiveSample;
    private Map<String,String> tileNames = new HashMap<>();
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getRequiredItem("SAMPLE_AREAS");
        this.colorSpec = data.getItemAsString("OUTPUT_COLOR_SPEC");
        this.mode = data.getItemAsString("MODE");
        this.sampleNaming = data.getItemAsBoolean("SAMPLE_NAMING");

        // For each merged file, find the tile name and save it in a map for later lookup
        for(AnatomicalArea sampleArea : sampleAreas) {
            for(MergedLsmPair mergedPair : sampleArea.getMergedLsmPairs()) {
                tileNames.put(mergedPair.getMergedFilepath(), mergedPair.getTileName());
            }
        }

        // Create an input image for each sample processing result
        List<InputImage> inputImages = new ArrayList<InputImage>();
        for(SampleProcessingResult resultEntity : run.getSampleProcessingResults()) {
            inputImages.add(getInputImage(resultEntity));
        }
        
        Collections.sort(inputImages, new Comparator<InputImage>() {
            @Override
            public int compare(InputImage o1, InputImage o2) {
                return ComparisonChain.start()
                        .compare(o1.getArea(), o2.getArea(), Ordering.natural()) // Brain before VNC
                        .compare(o1.getFilepath(), o2.getFilepath(), Ordering.natural().nullsLast()).result(); // Order by filename
            }
        });
        
        if (inputImages.isEmpty()) {
            throw new ServiceException("Could not find any unaligned images to process");
        }
        
        StringBuilder sb = new StringBuilder();
        for(InputImage inputImage : inputImages) {
            if (sb.length()>0) sb.append(",");
            sb.append(inputImage.getArea());
        }
        
        boolean normalizeToFirst = false;
        
        if ("20x".equals(objectiveSample.getObjective()) && inputImages.size()==2 && sb.toString().equals("Brain,VNC")) {
            // Special case of 20x Brain/VNC which need to be normalized
            // TODO: in the future, we should be able to normalize any number of images to a Brain
            normalizeToFirst = true;
        }
    
        String serviceClassName = ("20x".equals(objectiveSample.getObjective()) || mode==null) ? "BasicMIPandMovieGenerationService" : "EnhancedMIPandMovieGenerationService";
        String serviceClass = SERVICE_PACKAGE+"."+serviceClassName;
        
        contextLogger.info("Putting "+normalizeToFirst+" into NORMALIZE_TO_FIRST_IMAGE");
        processData.putItem("NORMALIZE_TO_FIRST_IMAGE", Boolean.valueOf(normalizeToFirst));
        contextLogger.info("Putting "+serviceClass+" into SERVICE_CLASS");
        processData.putItem("SERVICE_CLASS", serviceClass);
    	contextLogger.info("Putting "+inputImages.size()+" images into INPUT_IMAGES");
    	processData.putItem("INPUT_IMAGES", inputImages);
    }
    
    private InputImage getInputImage(SampleProcessingResult resultEntity) throws ComputeException {

        String area = resultEntity.getAnatomicalArea();
        String chanSpec = resultEntity.getChannelSpec();
        String effector = sample.getEffector();
        String filepath = DomainUtils.getDefault3dImageFilePath(resultEntity);
        String tileName = tileNames.get(filepath);
        
        String prefix = FileUtils.getFilePrefix(filepath);
        if (sampleNaming) {
            String sampleName = sanitize(sample.getName());
            if (filepath.contains("stitched-")) {
                if (StringUtils.isEmpty(area)) {
                    prefix = sampleName+"-stitched";
                }
                else {
                    prefix = sampleName+"-"+area;
                }
            }
            else {
                prefix = sampleName+"-"+sanitize(tileName);    
            }
            // Append effector if available and not already in the prefix by way of the sample name
            if (!StringUtils.isEmpty(effector) && !prefix.contains(effector)) {
                prefix += "-"+sanitize(effector);
            }
        }
        
        String colorspec = colorSpec;
        
        String objective = objectiveSample.getObjective();
        if (colorspec==null) {
            contextLogger.warn("No OUTPUT_COLOR_SPEC specified, attempting to guess based on objective="+objective+" and MODE="+mode+"...");
            
            // Default to RGB
            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
            
            if ("mcfo".equals(mode)) {
                // MCFO is always RGB
            }
            else {
                if (!"polarity".equals(mode) & chanSpec.length()==4) {
                    // 4 channel image with unknown mode, let's assume its MCFO
                    colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "RGB", "1");
                }
                else {
                    // Polarity and other image types (e.g. screen?) get the Yoshi treatment, 
                    // with green signal on top of magenta reference for 20x and green signal on top of grey reference for 63x.
                    if ("20x".equals(objective)) {
                        colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "GYC", "M");
                    }
                    else if ("63x".equals(objective)) {
                        if (chanSpec.length()==2) {
                            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "G", "1");
                        }
                        else {
                            colorspec = ChanSpecUtils.getDefaultColorSpec(chanSpec, "MGR", "1");
                        }
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
    
    private String sanitize(String s) {
        if (s==null) return null;
        return s.replaceAll("\\s+", "_").replaceAll("-", "_");
    }
}
