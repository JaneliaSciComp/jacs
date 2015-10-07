package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Incremental file discovery service for post-processing results.
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
public class IncrementalPostProcessingResultsDiscoveryService extends IncrementalResultDiscoveryService {

    @Override
    protected Entity createNewResultEntity(String resultName) throws Exception {
        if (StringUtils.isEmpty(resultName)) {
            resultName = "Sample Post-Processing Result";
        }
        Entity resultEntity = helper.createFileEntity(resultFileNode.getDirectoryPath(), resultName, EntityConstants.TYPE_POST_PROCESSING_RESULT);
        contextLogger.info("Created new post-processing result: "+resultEntity.getName()+" (id="+resultEntity.getId()+")");
        return resultEntity;
    }
    
    @Override
    protected void discoverResultFiles(Entity result) throws Exception {

        if (!result.getEntityTypeName().equals(EntityConstants.TYPE_POST_PROCESSING_RESULT)) {
            throw new IllegalStateException("Expected Post Processing Result as input");
        }
        
        File dir = new File(result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        contextLogger.info("Processing "+result.getName()+" results in "+dir.getAbsolutePath());
        
        if (!dir.canRead()) {
            contextLogger.info("Cannot read from folder "+dir.getAbsolutePath());
            return;
        }

        Entity supportingFiles = helper.getOrCreateSupportingFilesFolder(result);
        addFilesInDirToFolder(supportingFiles, dir, false);

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (StringUtils.isEmpty(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }
        
        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
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
