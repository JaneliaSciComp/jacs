package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;

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

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (sampleEntityId == null || "".equals(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        Entity supportingFiles = EntityUtils.getSupportingData(sampleProcessingResult);
        
        Map<String,Entity> jsonEntityMap = new HashMap<String,Entity>();
        for(Entity resultItem : supportingFiles.getChildren()) {
            if (resultItem.getName().endsWith(".json")) {
                String stub = resultItem.getName().replaceFirst("\\.json", "");
                jsonEntityMap.put(stub, resultItem);
                logger.info("Found JSON metadata file: "+resultItem.getName());
            }
        }

        entityLoader.populateChildren(sampleEntity);
        Entity sampleSupportingFiles = EntityUtils.getSupportingData(sampleEntity);

        if (sampleSupportingFiles!=null) {
            sampleSupportingFiles = entityBean.getEntityTree(sampleSupportingFiles.getId());
            List<Entity> tileEntities = EntityUtils.getDescendantsOfType(sampleSupportingFiles, EntityConstants.TYPE_IMAGE_TILE, true);
            for(Entity tileEntity : tileEntities) {
                for(EntityData ed : tileEntity.getOrderedEntityData()) {
                    Entity lsmStack = ed.getChildEntity();
                    if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
                        
                        logger.info("Processing metadata for LSM: "+lsmStack.getName());
                        
                        Entity jsonEntity = jsonEntityMap.get(lsmStack.getName());
                        if (jsonEntity==null) {
                            logger.warn("  No JSON metadata file found for LSM: "+lsmStack.getName());
                            continue;
                        }
                        
                        File jsonFile = new File(jsonEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
                        LSMMetadata metadata = LSMMetadata.fromFile(jsonFile);

                        StringBuffer colors = new StringBuffer();
                        StringBuffer dyeNames = new StringBuffer();
                        for(Channel channel : metadata.getChannels()) {
                            if (colors.length()>0) colors.append(",");
                            if (dyeNames.length()>0) dyeNames.append(",");
                            colors.append(channel.getColor());
                            dyeNames.append(metadata.getDetectionChannel(channel).getDyeName());
                        }
                        
                        logger.info("  Setting colors: "+colors);
                        logger.info("  Setting dyes: "+dyeNames);
                        
                        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS, colors.toString());
                        lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES, dyeNames.toString());
                        entityBean.saveOrUpdateEntity(lsmStack);
                    }
                }
            }

        }
        
    }
}
