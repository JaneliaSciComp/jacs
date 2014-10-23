package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Purge large files owned by the given sample and block further processing on it.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PurgeAndBlockSampleService extends AbstractEntityService {

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    public void execute() throws Exception {
        SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);

        final String sampleEntityIdStr = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
        
        List<Entity> samples = new ArrayList<Entity>();
        for(String oneSampleEntityIdStr : Task.listOfStringsFromCsvString(sampleEntityIdStr)) {
            final Entity sampleEntity = entityBean.getEntityById(oneSampleEntityIdStr);
            samples.add(sampleEntity);
        }
        
        for(Entity sample : samples) {
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_BLOCKED);
            entityBean.saveOrUpdateEntity(ownerKey, sample);
            sampleHelper.putInCorrectDataSetFolder(sample);
            entityBean.loadLazyEntity(sample, true);
            removeLargeImageFiles(sample, new HashSet<Long>());
        }
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
            entityBean.deleteEntityTreeById(ownerKey, entity.getId());
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
        
        if (!filepath.startsWith(JACS_DATA_DIR) && !filepath.startsWith(JACS_DATA_ARCHIVE_DIR)) {
            logger.warn("Cannot delete path outside of filestore: "+filepath);
            return;
        }
        
        File file = new File(filepath);
        try {
            logger.trace("Removing: "+file.getName());
            if (FileUtil.deletePath(filepath)) {
                throw new Exception("Unknown error attempting to delete path");
            }
        }
        catch (Exception e) {
            logger.error("Could not delete filepath "+filepath+" ("+e.getMessage()+")");
        }
    }
}
