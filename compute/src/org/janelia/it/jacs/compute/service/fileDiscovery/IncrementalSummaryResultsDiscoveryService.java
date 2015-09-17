package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;

/**
 * Incremental file discovery service for LSM summary results.
 * 
 * Input variables if adding files to an existing result:
 *   RESULT_ENTITY or RESULT_ENTITY_ID  
 * 
 * Input variables if discovering new result:
 *   ROOT_ENTITY_ID - the parent of the result
 *   ROOT_FILE_NODE - the file node containing the separation files to be discovered
 *   RESULT_ENTITY_NAME - the name of the new result entity
 *   SAMPLE_ENTITY_ID - the sample entity containing the ROOT_ENTITY_ID
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IncrementalSummaryResultsDiscoveryService extends IncrementalResultDiscoveryService {

    @Override
    protected Entity createNewResultEntity(String resultName) throws Exception {
        if (StringUtils.isEmpty(resultName)) {
            resultName = "LSM Summary Result";
        }
        Entity resultEntity = helper.createFileEntity(resultFileNode.getDirectoryPath(), resultName, EntityConstants.TYPE_LSM_SUMMARY_RESULT);
        logger.info("Created new summary result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
        return resultEntity;
    }
    
    @Override
    protected void discoverResultFiles(Entity summaryResult) throws Exception {

        if (!summaryResult.getEntityTypeName().equals(EntityConstants.TYPE_LSM_SUMMARY_RESULT)) {
            throw new IllegalStateException("Expected LSM Summary Result as input");
        }
        
        File dir = new File(summaryResult.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing "+summaryResult.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            logger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }

        Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(summaryResult);
        helper.addFilesInDirToFolder(supportingFiles, dir, true);

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (StringUtils.isEmpty(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
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
        
        entityLoader.populateChildren(sampleSupportingFiles);
        List<Entity> tileEntities = sampleSupportingFiles.getOrderedChildren();
        for(Entity tileEntity : tileEntities) {
            
            entityLoader.populateChildren(tileEntity);
            
            for(Entity lsmStack : EntityUtils.getChildrenOfType(tileEntity, EntityConstants.TYPE_LSM_STACK)) {
                                    
                String lsmFilename = ArchiveUtils.getDecompressedFilepath(lsmStack.getName());
                
                logger.info("Processing metadata for LSM: "+lsmFilename);
                
                Entity jsonEntity = jsonEntityMap.get(lsmFilename);
                if (jsonEntity==null) {
                    logger.warn("  No JSON metadata file found for LSM: "+lsmFilename);
                    continue;
                }

                List<String> colors = new ArrayList<>();
                List<String> dyeNames = new ArrayList<>();
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

                if (!colors.isEmpty() && !StringUtils.areAllEmpty(colors)) {
                    logger.info("  Setting LSM colors: "+colors);
                    lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_COLORS, Task.csvStringFromCollection(colors));
                }
                
                if (!dyeNames.isEmpty() && !StringUtils.areAllEmpty(dyeNames)) {
                    logger.info("  Setting LSM dyes: "+dyeNames);
                    lsmStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES, Task.csvStringFromCollection(dyeNames));
                }
                
                entityBean.saveOrUpdateEntity(lsmStack);
            }
        }   
    }
}
