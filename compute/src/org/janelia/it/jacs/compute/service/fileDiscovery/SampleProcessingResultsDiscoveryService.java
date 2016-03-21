package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import javax.resource.spi.IllegalStateException;

import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.domain.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * File discovery service for sample processing results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleProcessingResultsDiscoveryService extends AbstractDomainService {

    private Sample sample;
    private ObjectiveSample objectiveSample;
    
    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
        String channelMappingStr = data.getRequiredItemAsString("LSM_CHANNEL_MAPPING");
        Collection<String> channelMapping = Task.listOfStringsFromCsvString(channelMappingStr);
        
        String channelSpec = data.getRequiredItemAsString("CHANNEL_SPEC");
        AnatomicalArea sampleArea = (AnatomicalArea)data.getRequiredItem("SAMPLE_AREA");
        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        
        SampleProcessingResult result = sampleHelper.addNewSampleProcessingResult(run, resultName);
        
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();

        logger.info("Discovering supporting files in "+rootPath);
        logger.info("Creating entity named '"+resultName+"' with type 'SampleProcessingResult'");

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);

        result.setAnatomicalArea(sampleArea.getName());
        
        String stitchedFilepath = sampleArea.getStitchedFilepath();
        
        if (channelSpec!=null) {
            logger.info("Setting result channel specification to "+channelSpec);
            result.setChannelSpec(channelSpec);
        }
        
        String pixelRes = null;
        File image3d = null;
        for(String filepath : filepaths) {
            File file = new File(filepath);
            if (file.getName().endsWith(".tc")) {
                pixelRes = getStitchedDimensions(filepath);
            }
            else if (filepath.equals(stitchedFilepath)) {
                if (image3d!=null) {
                    logger.warn("More than one 3d image result detected for sample processing "+result.getId());
                }   
                logger.info("Using as main 3d image: "+file.getName());
                image3d = file;
            }
            // TODO: MIPS
//            else if (file.getName().endsWith(suffix)) {
//              "ReferenceMip": "merge/tile-1947307609727959138_reference.png",
//              "SignalMip": "merge/tile-1947307609727959138_signal.png",
//            }
        }
        
        if (image3d==null) {
            throw new IllegalStateException("Sample image not found: "+stitchedFilepath);
        }
        
        if (pixelRes==null) {
            // The result image was not stitched (since no *.tc file was found), so we can get the pixel resolution from the LSMs
            pixelRes = sampleHelper.getConsensusLsmAttributeValue(sampleArea, "imageSize");
        }

        // TODO: should determine consensus pixel resolution for all tiles (not just the main image), if we didn't stitch them
        if (pixelRes!=null) {
            logger.info("Setting result pixel resolution to "+pixelRes);
            result.setImageSize(pixelRes);
        }
        else {
            throw new ServiceException("Could not determine pixel resolution for "+image3d.getName());
        }
        
        result.setFilepath(rootPath);
        DomainUtils.setFilepath(result, FileType.LosslessStack, image3d.getAbsolutePath());
        
        // Find consensus optical res    
        String opticalRes = sampleHelper.getConsensusLsmAttributeValue(sampleArea, "opticalResolution");
        if (opticalRes!=null) {
            logger.info("Setting result optic resolution to "+opticalRes);
            result.setOpticalResolution(opticalRes);
        }
     
        // Find consensus channel colors
        logger.debug("channelMapping="+channelMapping);
        String channelColors = sampleHelper.getConsensusLsmAttributeValue(sampleArea, "channelColors");
        
        if (channelColors!=null) {
            List<String> consensusLsmColors = Task.listOfStringsFromCsvString(channelColors);
            logger.debug("consensusLsmColors="+consensusLsmColors);
        
            List<String> resultColors = new ArrayList<String>();
            for(String indexStr : channelMapping) {
                int originalIndex = Integer.parseInt(indexStr);
                String originalColor = consensusLsmColors.get(originalIndex);
                if (originalColor!=null) {
                    resultColors.add(originalColor);   
                }
                else {
                    resultColors.add("");
                }
            }
            
            if (image3d!=null && !resultColors.isEmpty()) {
                String resultColorsStr = Task.csvStringFromCollection(resultColors);
                logger.info("Setting result image colors: "+resultColorsStr);
                result.setChannelColors(resultColorsStr);
            }
        }
        
        sampleHelper.saveSample(sample);
        
        contextLogger.info("Putting "+result.getId()+" in RESULT_ENTITY_ID");
        data.putItem("RESULT_ENTITY_ID", result.getId());
    }
    
    private String getStitchedDimensions(String filePath) throws Exception {
        
        boolean takeNext = false;
        String dimensions = null;
        Scanner scanner = new Scanner(new File(filePath));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (takeNext) {
                dimensions = line;
                break;
            }
            else if (line.contains("# dimensions")) {
                takeNext = true;
            }
        }
        
        scanner.close();
        
        String[] parts = dimensions.split(" ");
        if (parts.length<3) return null;
        return parts[0]+"x"+parts[1]+"x"+parts[2];
    }
}
