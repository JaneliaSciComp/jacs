package org.janelia.it.jacs.compute.service.domain.discovery;

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

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        List<InputImage> inputImages = (List<InputImage>)data.getRequiredItem("INPUT_IMAGES");
        
        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();
        SamplePostProcessingResult result = sampleHelper.addNewSamplePostProcessingResult(run, resultName);
        result.setFilepath(rootPath);

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);
        
        Map<String,FileGroup> fileGroups = sampleHelper.createFileGroups(result, filepaths);
        
        Map<String,String> prefixToKeyMap = new HashMap<>();
        for(InputImage inputImage : inputImages) {
        	prefixToKeyMap.put(inputImage.getOutputPrefix(), inputImage.getKey());
        }
        
        Map<String,FileGroup> groups = new HashMap<>(); 
        for(String outputPrefix : fileGroups.keySet()) {
        	FileGroup fileGroup = fileGroups.get(outputPrefix);
        	String key = prefixToKeyMap.get(outputPrefix);
        	if (key==null) {
        		logger.warn("Unrecognized output prefix: "+outputPrefix);
        		continue;
        	}
        	groups.put(key, fileGroup);
        }
        
        result.setGroups(groups);
        
        sampleHelper.saveSample(sample);
        data.putItem("RESULT_ENTITY_ID", result.getId());
    }
}
