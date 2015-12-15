package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.FileUtils;
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
 *   SAMPLE_ENTITY_ID - the sample entity containing the ROOT_ENTITY_ID
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
        contextLogger.info("Created new summary result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
        return resultEntity;
    }
    
    @Override
    protected void discoverResultFiles(Entity summaryResult) throws Exception {

        if (!summaryResult.getEntityTypeName().equals(EntityConstants.TYPE_LSM_SUMMARY_RESULT)) {
            throw new IllegalStateException("Expected LSM Summary Result as input");
        }
        
        File dir = new File(summaryResult.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        contextLogger.info("Processing "+summaryResult.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            contextLogger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }

        Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(summaryResult);
        addFilesInDirToFolder(supportingFiles, dir, false);

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
                contextLogger.info("Found JSON metadata file: "+resultItem.getName());
            }
        }
        
        Map<String,Entity> propertiesEntityMap = new HashMap<String,Entity>();
        for(Entity resultItem : supportingFiles.getChildren()) {
            if (resultItem.getEntityTypeName().equals(EntityConstants.TYPE_TEXT_FILE)) {
                if (resultItem.getName().endsWith(".properties")) {
                    String stub = resultItem.getName().replaceFirst("\\.properties", ".lsm");
                    propertiesEntityMap.put(stub, resultItem);
                    contextLogger.info("Found properties file: "+resultItem.getName());
                }
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
                contextLogger.debug("Processing metadata for LSM: "+lsmFilename);

                boolean dirty = false;
                
                Entity jsonEntity = jsonEntityMap.get(lsmFilename);
                if (jsonEntity!=null) {
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
	                        else {
	                            dyeNames.add("Unknown");
	                        }
	                    }
	                }
	                catch (Exception e) {
	                    throw new Exception("Error parsing LSM metadata file: "+jsonFile,e);
	                }
	
	                if (!colors.isEmpty() && !StringUtils.areAllEmpty(colors)) {
	                    if (EntityUtils.setAttribute(lsmStack, EntityConstants.ATTRIBUTE_CHANNEL_COLORS, Task.csvStringFromCollection(colors))) {
	                        contextLogger.info("  Setting LSM colors: "+colors);
	                        dirty = true;
	                    }
	                }
	                
	                if (!dyeNames.isEmpty() && !StringUtils.areAllEmpty(dyeNames)) {
	                    if (EntityUtils.setAttribute(lsmStack, EntityConstants.ATTRIBUTE_CHANNEL_DYE_NAMES, Task.csvStringFromCollection(dyeNames))) {
	                        contextLogger.info("  Setting LSM dyes: "+dyeNames);
	                        dirty = true;
	                    }
	                }
                }
                
                Entity propertiesEntity = propertiesEntityMap.get(lsmFilename);
                if (propertiesEntity!=null) {
	                File propertiesFile = new File(propertiesEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
	                Properties properties = new Properties();
	                properties.load(new FileReader(propertiesFile));
	                
	                String brightnessCompensation = properties.getProperty("image.brightness.compensation");
	                if (!StringUtils.isEmpty(brightnessCompensation)) {
	                    if (EntityUtils.setAttribute(lsmStack, EntityConstants.ATTRIBUTE_BRIGHTNESS_COMPENSATION, brightnessCompensation)) {
	                        contextLogger.info("  Setting brightness compensation: "+brightnessCompensation);
	                        dirty = true;
	                    }
	                }
	            }
                
                
                if (dirty) {
                    entityBean.saveOrUpdateEntity(lsmStack);
                }
            }
        }   
    }

    private List<File> addFilesInDirToFolder(Entity folder, File dir, boolean recurse) throws Exception {
        List<File> files = helper.collectFiles(dir, recurse);
        contextLogger.info("Collected "+files.size()+" files for addition to "+folder.getName());
        if (!files.isEmpty()) {
            FileUtils.sortFilesByName(files);
            addFilesToFolder(folder, files);
        }
        return files;
    }

    private void addFilesToFolder(Entity filesFolder, List<File> files) throws Exception {
        for (File resultFile : files) {
            Entity resultEntity = getOrCreateResultItem(resultFile);
            addToParentIfNecessary(filesFolder, resultEntity, EntityConstants.ATTRIBUTE_ENTITY);
        }
    }
}
