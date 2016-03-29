package org.janelia.it.jacs.compute.service.domain.discovery;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
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
public class SummaryResultsDiscoveryService extends AbstractDomainService {
    
    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;

    private Map<String,String> jsonEntityMap = new HashMap<String,String>();
    private Map<String,String> propertiesEntityMap = new HashMap<String,String>();
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();
        LSMSummaryResult result = sampleHelper.addNewLSMSummaryResult(run, resultName);
        result.setFilepath(rootPath);

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);
        for(String filepath : filepaths) {
            populateMaps(filepath);
        }

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

        sampleHelper.saveSample(sample);

        updateLSMS();
        
        contextLogger.info("Putting "+result.getId()+" in RESULT_ENTITY_ID");
        data.putItem("RESULT_ENTITY_ID", result.getId());
    }
    
    private void populateMaps(String filepath) {

        File file = new File(filepath);
        
        if (file.getName().endsWith(".json")) {
            String stub = file.getName().replaceFirst("\\.json", "");
            jsonEntityMap.put(stub, file.getAbsolutePath());
            contextLogger.info("Found JSON metadata file: "+file);
        }
    
        if (file.getName().endsWith(".properties")) {
            String stub = file.getName().replaceFirst("\\.properties", ".lsm");
            propertiesEntityMap.put(stub, file.getAbsolutePath());
            contextLogger.info("Found properties file: "+file);
        }
    }
    
    protected void updateLSMS() throws Exception {

        for(SampleTile tileEntity : objectiveSample.getTiles()) {
            
            List<LSMImage> lsms = domainDao.getDomainObjectsAs(tileEntity.getLsmReferences(), LSMImage.class);
            for(LSMImage lsm : lsms) {
                String lsmFilename = ArchiveUtils.getDecompressedFilepath(lsm.getName());
                contextLogger.debug("Processing metadata for LSM: "+lsmFilename);

                boolean dirty = false;
                
                String jsonFilepath = jsonEntityMap.get(lsmFilename);
                if (jsonFilepath!=null) {
	                List<String> colors = new ArrayList<>();
	                List<String> dyeNames = new ArrayList<>();
	                File jsonFile = new File(jsonFilepath);
	                
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
	                    contextLogger.info("  Setting LSM colors: "+colors);
	                    lsm.setChannelColors(Task.csvStringFromCollection(colors));
	                    dirty = true;
	                }
	                
	                if (!dyeNames.isEmpty() && !StringUtils.areAllEmpty(dyeNames)) {
	                    contextLogger.info("  Setting LSM dyes: "+dyeNames);
                        lsm.setChannelDyeNames(Task.csvStringFromCollection(dyeNames));
                        dirty = true;
	                }
                }
                
                String propertiesFilepath = propertiesEntityMap.get(lsmFilename);
                if (propertiesFilepath!=null) {
	                File propertiesFile = new File(propertiesFilepath);
	                Properties properties = new Properties();
	                properties.load(new FileReader(propertiesFile));
	                
	                String brightnessCompensation = properties.getProperty("image.brightness.compensation");
	                if (!StringUtils.isEmpty(brightnessCompensation)) {
	                    contextLogger.info("  Setting brightness compensation: "+brightnessCompensation);
	                    lsm.setBrightnessCompensation(brightnessCompensation);
                        dirty = true;
	                }
	            }
                
                if (dirty) {
                    sampleHelper.saveLsm(lsm);
                }
            }
        }   
    }
}
