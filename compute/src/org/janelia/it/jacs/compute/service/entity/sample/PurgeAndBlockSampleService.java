package org.janelia.it.jacs.compute.service.entity.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Purge large files owned by the given sample and block further processing on it.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class PurgeAndBlockSampleService extends AbstractDomainService {

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    public void execute() throws Exception {
        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger);
        String sampleEntityIdStr = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
        
        List<Sample> samples = new ArrayList<>();
        for(String oneSampleEntityIdStr : Task.listOfStringsFromCsvString(sampleEntityIdStr)) {
            final Sample sampleEntity = domainDao.getDomainObject(ownerKey, Sample.class, new Long(oneSampleEntityIdStr));
            if (sampleEntity==null) {
                throw new IllegalArgumentException("Sample does not exist: "+oneSampleEntityIdStr);
            }
            samples.add(sampleEntity);
        }
        
        for(Sample sample : samples) {
            sample.setStatus(EntityConstants.VALUE_BLOCKED);
            sampleHelper.saveSample(sample);
            removeLargeImageFiles(sample);
        }
    }
    
    private void removeLargeImageFiles(Sample sample) throws Exception {
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                for(PipelineResult result : run.getResults()) {
                    String filepath = DomainUtils.getFilepath(result, FileType.LosslessStack);
                    deletePath(filepath);
                    DomainUtils.setFilepath(result, FileType.LosslessStack, null);
                    for(NeuronSeparation separation : result.getResultsOfType(NeuronSeparation.class)) {
                        // TODO: need a better way of clearing out separations, now that we don't have the files in the database
                        String separationFilepath = separation.getFilepath();
                        File sepArchivePath = new File(separationFilepath, "archive");
                        File sepFastLoadPath = new File(separationFilepath, "fastLoad");
                        File consolidatedLabelPath = new File(separationFilepath, "ConsolidatedLabel.v3dpbd");
                        File consolidatedSignalPath = new File(separationFilepath, "ConsolidatedSignal.v3dpbd");
                        File referencePath = new File(separationFilepath, "Reference.v3dpbd");
                        deletePath(sepArchivePath.getAbsolutePath());
                        deletePath(sepFastLoadPath.getAbsolutePath());
                        deletePath(consolidatedLabelPath.getAbsolutePath());
                        deletePath(consolidatedSignalPath.getAbsolutePath());
                        deletePath(referencePath.getAbsolutePath());
                    }
                }
            }
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
