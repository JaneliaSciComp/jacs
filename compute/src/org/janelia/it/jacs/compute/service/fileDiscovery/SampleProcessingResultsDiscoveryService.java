package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
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

        if (!sampleProcessingResult.getEntityType().getName().equals(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)) {
            throw new IllegalStateException("Expected Sample Processing Result as input");
        }

        super.processFolderForData(sampleProcessingResult);

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

        // Find consensus optical res
        entityLoader.populateChildren(sampleEntity);
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, null, ownerKey, logger);        
        String opticalRes = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, sampleArea.getName());

        Entity supportingFiles = EntityUtils.getSupportingData(sampleProcessingResult);
        
        Map<String,Entity> jsonEntityMap = new HashMap<String,Entity>();
        for(Entity resultItem : supportingFiles.getChildren()) {
            if (resultItem.getName().endsWith(".json")) {
                String stub = resultItem.getName().replaceFirst("\\.json", "");
                jsonEntityMap.put(stub, resultItem);
                logger.info("Found JSON metadata file: "+resultItem.getName());
            }
            else if (resultItem.getEntityType().getName().equals(EntityConstants.TYPE_IMAGE_3D)) {
                if (channelSpec!=null) {
                    logger.info("Setting channel specification for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+channelSpec);
                    helper.setChannelSpec(resultItem, channelSpec);
                }
                if (opticalRes!=null) {
                    logger.info("Setting optic resolution for "+resultItem.getName()+" (id="+resultItem.getId()+") to "+opticalRes);
                    helper.setOpticalResolution(resultItem, opticalRes);
                }
            }
        }

        for(Entity tileEntity : sampleArea.getTiles()) {
            for(EntityData ed : tileEntity.getOrderedEntityData()) {
                Entity lsmStack = ed.getChildEntity();
                if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                    
                    // Don't trust entities in ProcessData, fetch a fresh copy
                    lsmStack = entityBean.getEntityById(lsmStack.getId());
                    
                    logger.info("Processing metadata for LSM: "+lsmStack.getName());
                    
                    Entity jsonEntity = jsonEntityMap.get(lsmStack.getName());
                    if (jsonEntity==null) {
                        logger.warn("  No JSON metadata file found for LSM: "+lsmStack.getName());
                        continue;
                    }

                    StringBuffer colors = new StringBuffer();
                    StringBuffer dyeNames = new StringBuffer();
                    File jsonFile = new File(jsonEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                    
                    try {
                        LSMMetadata metadata = LSMMetadata.fromFile(jsonFile);
                        for(Channel channel : metadata.getChannels()) {
                            if (colors.length()>0) colors.append(",");
                            if (dyeNames.length()>0) dyeNames.append(",");
                            colors.append(channel.getColor());
                            DetectionChannel detection = metadata.getDetectionChannel(channel);
                            if (detection!=null) {
                                dyeNames.append(detection.getDyeName());
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new Exception("Error parsing LSM metadata file: "+jsonFile,e);
                    }
                    
                    logger.info("  Setting colors: "+colors);
                    lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS, colors.toString());
                    if (dyeNames.length()>0) {
                        logger.info("  Setting dyes: "+dyeNames);
                        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES, dyeNames.toString());
                    }
                    entityBean.saveOrUpdateEntity(lsmStack);
                }
            }
        }
        
    }
}
