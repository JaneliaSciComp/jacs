package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Purge large files owned by the given sample and block further processing on it.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PurgeAndBlockSampleService extends AbstractEntityService {

    public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    public transient static final String CENTRAL_ARCHIVE_DIR_PROP = "FileStore.CentralDir.Archived";

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    public void execute() throws Exception {
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        Entity sampleEntity = entityHelper.getRequiredSampleEntity(data);
        sampleEntity = sampleHelper.blockSampleProcessing(sampleEntity.getId());
        sampleHelper.putInCorrectDataSetFolder(sampleEntity);
        entityBean.loadLazyEntity(sampleEntity, true);
        removeLargeImageFiles(sampleEntity, new HashSet<Long>());
    }
    
    private void removeLargeImageFiles(Entity entity, Set<Long> visited) throws Exception {
        
        if (visited.contains(entity.getId())) return;
        visited.add(entity.getId());

        for(Entity child : entity.getChildren()) {
            removeLargeImageFiles(child, visited);
        }
        
        String entityType = entity.getEntityTypeName();
        
        if (entityType.equals(EntityConstants.TYPE_IMAGE_3D) 
                || entityType.equals(EntityConstants.TYPE_MOVIE)
                || entityType.equals(EntityConstants.TYPE_FILE)) {
            String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            deletePath(filepath);
            entityBean.deleteEntityTree(ownerKey, entity.getId());
        }
        else if (entityType.equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
            String filepath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            File sepArchivePath = new File(filepath, "archive");
            File sepFastLoadPath = new File(filepath, "fastLoad");
            deletePath(sepArchivePath.getAbsolutePath());
            deletePath(sepFastLoadPath.getAbsolutePath());
        }
    }
    
    private void deletePath(String filepath) {
        
        if (filepath.startsWith(JACS_DATA_DIR) || filepath.startsWith(JACS_DATA_ARCHIVE_DIR)) {
            logger.warn("Cannot delete path outside of filestore: "+filepath);
            return;
        }
        
        File file = new File(filepath);
        try {
            logger.info("Removing: "+file.getName());
            if (FileUtil.deletePath(filepath)) {
                throw new Exception("Unknown error attempting to delete path");
            }
        }
        catch (Exception e) {
            logger.error("Could not delete filepath: "+filepath,e);
        }
    }
}
