package org.janelia.it.jacs.compute.service.domain.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.image.InputImage;
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
public class PostProcessingResultsDiscoveryService extends AbstractDomainService {

    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;

    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        List<InputImage> inputImages = (List<InputImage>)data.getRequiredItem("INPUT_IMAGES");
        
        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();
        SamplePostProcessingResult result = sampleHelper.addNewSamplePostProcessingResult(run, resultName);
        result.setFilepath(rootPath);

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);
        
        List<FileGroup> fileGroups = sampleHelper.createFileGroups(result, filepaths);
        
        Map<String,String> keyToCorrectedKeyMap = new HashMap<>();
        for(InputImage inputImage : inputImages) {
            contextLogger.info("Will replace output prefix "+inputImage.getOutputPrefix()+" with key "+inputImage.getKey());
        	keyToCorrectedKeyMap.put(inputImage.getOutputPrefix(), inputImage.getKey());
        }
        
        Map<String,FileGroup> groups = new HashMap<>(); 
        for(FileGroup fileGroup : fileGroups) {
            String key = fileGroup.getKey();
        	String correctedKey = keyToCorrectedKeyMap.get(key);
        	if (correctedKey==null) {
                contextLogger.warn("Unrecognized output prefix: "+key);
        		continue;
        	}
            fileGroup.setKey(correctedKey);
        	groups.put(correctedKey, fileGroup);
        }
        
        result.setGroups(new ArrayList<>(groups.values()));
        
        sampleHelper.saveSample(sample);
        data.putItem("RESULT_ENTITY_ID", result.getId());
    }
}
