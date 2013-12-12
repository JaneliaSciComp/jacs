package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;

/**
 * File discovery service for sample processing results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleProcessingResultsDiscoveryService extends SupportingFilesDiscoveryService {

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        processData.putItem("RESULT_ENTITY_TYPE", EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT);
        super.execute(processData);
    }

    @Override
    protected void processFolderForData(Entity sampleProcessingResult) throws Exception {

        if (!sampleProcessingResult.getEntityTypeName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
            throw new IllegalStateException("Expected Sample Processing Result as input");
        }

        super.processFolderForData(sampleProcessingResult);

        String channelMappingStr = (String)processData.getItem("LSM_CHANNEL_MAPPING");
        Collection<String> channelMapping = Task.listOfStringsFromCsvString(channelMappingStr);
        
        String channelSpec = (String)processData.getItem("CHANNEL_SPEC");
        if (StringUtils.isEmpty(channelSpec)) {
            throw new IllegalArgumentException("CHANNEL_SPEC may not be null");
        }

        AnatomicalArea sampleArea = (AnatomicalArea)processData.getItem("SAMPLE_AREA");
        if (sampleArea==null) {
            throw new IllegalArgumentException("SAMPLE_AREA may not be null");
        }
        
        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (StringUtils.isEmpty(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        String stitchedFilename = (String)processData.getItem("STITCHED_FILENAME");

        // Find consensus optical res
        entityLoader.populateChildren(sampleEntity);
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, null, ownerKey, logger);        
        String opticalRes = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, sampleArea.getName());
        
        Entity supportingFiles = EntityUtils.getSupportingData(sampleProcessingResult);
        String pixelRes = null;
        
        Entity image3d = null;
        
        Map<String,Entity> jsonEntityMap = new HashMap<String,Entity>();
        for(Entity resultItem : supportingFiles.getChildren()) {
            if (resultItem.getName().endsWith(".json")) {
                String stub = resultItem.getName().replaceFirst("\\.json", "");
                jsonEntityMap.put(stub, resultItem);
                logger.info("Found JSON metadata file: "+resultItem.getName());
            }
            else if (resultItem.getName().endsWith(".tc")) {
                pixelRes = getStitchedDimensions(resultItem.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
            }
            else if (resultItem.getEntityTypeName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                if (stitchedFilename.equals(resultItem.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH))) {
                    if (image3d!=null) {
                        logger.warn("More than one 3d image result detected for sample processing "+sampleProcessingResult.getId());
                    }   
                    logger.info("Using as main 3d image: "+resultItem.getName());
                    image3d = resultItem; 
                    if (channelSpec!=null) {
                        logger.info("Setting channel specification for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelSpec);
                        helper.setChannelSpec(resultItem, channelSpec);
                    }
                    if (opticalRes!=null) {
                        logger.info("Setting optic resolution for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+opticalRes);
                        helper.setOpticalResolution(resultItem, opticalRes);
                    }
                }
                else {
                    logger.info("Ignoring 3d image which is not the stitched file: "+resultItem.getName());
                }
            }
        }

        if (pixelRes==null) {
            // The result image was not stitched (since no *.tc file was found), so we can get the pixel resolution from the LSMs
            pixelRes = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, sampleArea.getName());
        }

        if (pixelRes!=null) {
            logger.info("Setting pixel resolution for "+image3d.getName()+" (id="+image3d.getId()+") to "+pixelRes);
            helper.setPixelResolution(image3d, pixelRes);
        }

        List<String> consensusLsmColors = null;
        boolean consensusColors = true;
        
        for(Entity tileEntity : sampleArea.getTiles()) {
            
            List<String> allLsmColors = new ArrayList<String>();
            
            for(EntityData ed : tileEntity.getOrderedEntityData()) {
                Entity lsmStack = ed.getChildEntity();
                if (lsmStack != null && lsmStack.getEntityTypeName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    
                    // Don't trust entities in ProcessData, fetch a fresh copy
                    lsmStack = entityBean.getEntityById(lsmStack.getId());
                    
                    logger.info("Processing metadata for LSM: "+lsmStack.getName());
                    
                    Entity jsonEntity = jsonEntityMap.get(lsmStack.getName());
                    if (jsonEntity==null) {
                        logger.warn("  No JSON metadata file found for LSM: "+lsmStack.getName());
                        continue;
                    }

                    List<String> colors = new ArrayList<String>();
                    List<String> dyeNames = new ArrayList<String>();
                    File jsonFile = new File(jsonEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    
                    try {
                        LSMMetadata metadata = LSMMetadata.fromFile(jsonFile);
                        for(Channel channel : metadata.getChannels()) {
                            colors.add(channel.getColor());
                            DetectionChannel detection = metadata.getDetectionChannel(channel);
                            if (detection!=null) {
                                dyeNames.add(detection.getDyeName());
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new Exception("Error parsing LSM metadata file: "+jsonFile,e);
                    }
                    
                    allLsmColors.addAll(colors);
                    
                    logger.info("  Setting LSM colors: "+colors);
                    lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS, Task.csvStringFromCollection(colors));
                    if (!dyeNames.isEmpty()) {
                        logger.info("  Setting LSM dyes: "+dyeNames);
                        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES, Task.csvStringFromCollection(dyeNames));
                    }
                    entityBean.saveOrUpdateEntity(lsmStack);
                }
            }
            
            if (consensusLsmColors==null) {
                consensusLsmColors = allLsmColors;
            }
            else if (!consensusLsmColors.equals(allLsmColors)) {
                logger.warn("No color consensus among tiles ("+consensusLsmColors+"!="+allLsmColors+")");
                consensusColors = false;
            }
        }

        logger.debug("channelMapping="+channelMapping);
        
        if (consensusColors) {
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
                helper.setChannelColors(image3d, resultColorsStr);
            }
        }
    }

    public static String getStitchedDimensions(String filePath) throws Exception {
        
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
