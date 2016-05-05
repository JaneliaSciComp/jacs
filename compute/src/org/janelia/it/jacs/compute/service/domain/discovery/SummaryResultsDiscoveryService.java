package org.janelia.it.jacs.compute.service.domain.discovery;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.FileDiscoveryHelperNG;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.FileGroup;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.LSMSummaryResult;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Channel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;

/**
 * Incremental file discovery service for LSM summary results.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SummaryResultsDiscoveryService extends AbstractDomainService {
    
    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;

    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);

        FileNode resultFileNode = (FileNode)data.getRequiredItem("ROOT_FILE_NODE");
        String rootPath = resultFileNode.getDirectoryPath();

        FileDiscoveryHelperNG helper = new FileDiscoveryHelperNG(computeBean, ownerKey, logger);
        List<String> filepaths = helper.getFilepaths(rootPath);

        LSMSummaryResult result;

        Long resultId = data.getItemAsLong("RESULT_ENTITY_ID");
        if (resultId==null) {
            String resultName = data.getRequiredItemAsString("RESULT_ENTITY_NAME");
            result = sampleHelper.addNewLSMSummaryResult(run, resultName);
            result.setFilepath(rootPath);
        }
        else {
            List<LSMSummaryResult> results = run.getResultsById(LSMSummaryResult.class, resultId);
            if (results==null || results.isEmpty()) {
                throw new IllegalStateException("Result not found: "+resultId);
            }
            if (results.size()>1) {
                logger.warn("More than one result with id: "+resultId+". Only the latest will be updated.");
            }

            result = results.get(results.size()-1);
        }

        List<FileGroup> groups = sampleHelper.createFileGroups(result, filepaths);

        for(FileGroup group : groups) {

            FileGroup existingGroup = result.getGroup(group.getKey());
            if (existingGroup==null) {
                logger.info("Discovered group "+group.getKey());
                result.addGroup(group);
            }
            else {
                logger.info("Discovered update for group "+group.getKey());
                for(FileType fileType : group.getFiles().keySet()) {
                    String filepath = DomainUtils.getFilepath(group, fileType);
                    DomainUtils.setFilepath(existingGroup, fileType, filepath);
                }
            }
        }

        sampleHelper.saveSample(sample);

        data.putItem("RESULT_ENTITY_ID", result.getId());
        updateLSMS(filepaths, result);
    }
    
    protected void updateLSMS(List<String> filepaths, LSMSummaryResult result) throws Exception {

        // Discover properties files which are not found by createFileGroups
    	Map<String,String> propertiesFileMap = new HashMap<>();
        for(String filepath : filepaths) {
            File file = new File(filepath);
            if (file.getName().endsWith(".properties")) {
                String lsmKey = file.getName().replaceFirst("\\.properties", ".lsm");
                propertiesFileMap.put(lsmKey, file.getAbsolutePath());
                contextLogger.info("Found properties file: "+file);
            }
        }
        
        for(SampleTile tileEntity : objectiveSample.getTiles()) {
            
            List<LSMImage> lsms = domainDao.getDomainObjectsAs(tileEntity.getLsmReferences(), LSMImage.class);
            for(LSMImage lsm : lsms) {
                String lsmFilename = ArchiveUtils.getDecompressedFilepath(lsm.getName());
                contextLogger.debug("Processing metadata for LSM: "+lsmFilename);

                String lsmKey = FilenameUtils.getBaseName(lsmFilename);
                FileGroup lsmSummaryGroup = result.getGroup(lsmKey);

                boolean dirty = false;

                for (FileType fileType : lsmSummaryGroup.getFiles().keySet()) {
                    String filepath = DomainUtils.getFilepath(lsmSummaryGroup, fileType);
                    String existingFilepath = DomainUtils.getFilepath(lsm, fileType);
                    if (!StringUtils.areEqual(filepath, existingFilepath)) {
                        contextLogger.debug("  Updating LSM with: "+filepath);
                        DomainUtils.setFilepath(lsm, fileType, filepath);
                        dirty = true;
                    }
                }

                String jsonFilepath = DomainUtils.getFilepath(lsmSummaryGroup, FileType.LsmMetadata);
                if (jsonFilepath!=null) {

                    contextLogger.info("  Setting JSON Metadata: "+jsonFilepath);
                	DomainUtils.setFilepath(lsm, FileType.LsmMetadata, jsonFilepath);
                    dirty = true;
                    
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
                
                String propertiesFilepath = propertiesFileMap.get(lsmFilename);
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
