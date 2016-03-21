package org.janelia.it.jacs.compute.service.fileDiscovery;

import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.service.domain.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SamplePostProcessingResult;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Incremental file discovery service for post-processing results.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IncrementalPostProcessingResultsDiscoveryService extends AbstractDomainService {

    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;

    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();
        SamplePostProcessingResult result = sampleHelper.addNewSamplePostProcessingResult(run, resultName);
        result.setFilepath(rootPath);

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);

        // TODO: is this code from MongoDbImport needed here?
//        Set<String> keys = new HashSet<>();
//        for(LSMImage lsm : lsms) {
//            String name = lsm.getName();
//            int index = name.indexOf('.');
//            String key = index<1 ? name : name.substring(0, index);
//            keys.add(key);
//        }
//        keys.add("montage");
        
        Map<String,FileGroup> groups = sampleHelper.createFileGroups(result, filepaths);
        result.setGroups(groups);    
        contextLogger.info("Putting "+result.getId()+" in RESULT_ENTITY_ID");
        data.putItem("RESULT_ENTITY_ID", result.getId());
        
        sampleHelper.saveSample(sample);
    }
}
