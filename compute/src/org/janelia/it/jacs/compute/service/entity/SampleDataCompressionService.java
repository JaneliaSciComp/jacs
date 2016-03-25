package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Compress a set of existing files to a new set of formats.  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDataCompressionService extends AbstractDomainService {

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
	private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
	
    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final String MODE_CREATE_OUTPUT_LIST = "CREATE_OUTPUT_LIST";
    public static final String MODE_COMPLETE = "COMPLETE";
    public transient static final String PARAM_testRun = "is test run";
	
    private boolean isDebug = false;

    private SampleHelperNG sampleHelper;
    
    private String mode;
    private String inputType;
    private String outputType;
    
    private boolean deleteSourceFiles = true;
    private Set<Pattern> exclusions = new HashSet<>();
    
    private Sample sample;
    private final Set<String> inputFiles = new LinkedHashSet<>();
    
    protected int numChanges;

    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        this.mode = data.getRequiredItemAsString("MODE");
        this.inputType = data.getRequiredItemAsString("INPUT_TYPE");
        this.outputType = data.getRequiredItemAsString("OUTPUT_TYPE");
        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
                
        if (mode.equals(MODE_CREATE_INPUT_LIST)) {
            doCreateInputList();
        }
        else if (mode.equals(MODE_CREATE_OUTPUT_LIST)) {
            doCreateOutputList();
        }
        else if (mode.equals(MODE_COMPLETE)) {
            doComplete();
        } 
        else {
            throw new IllegalStateException("Do not recognize mode '"+mode+"'");
        }
    }
    
    private void doCreateInputList() throws ComputeException {

        String excludeFiles = data.getItemAsString("EXCLUDE_FILES");
        if (!StringUtils.isEmpty(excludeFiles)) {
            for(String filePattern : excludeFiles.split("\\s*,\\s*")) {
            	Pattern p = Pattern.compile(filePattern.replaceAll("\\*", "(.*?)"));
                exclusions.add(p);
            }
        }

        this.deleteSourceFiles = !"false".equals(data.getRequiredItem("DELETE_INPUTS"));

        if (isDebug) {
            contextLogger.info("This is a test run. Nothing will actually happen.");
        }
        else {
            if (deleteSourceFiles) {
                contextLogger.info("This is the real thing. Files will get compressed, and then the originals will be deleted!");    
            }
            else {
                contextLogger.info("This is the real thing. Files will get compressed, and added to the existing entities.");
            }
        }
        
        contextLogger.info("Finding files to compress under sample "+sample.getId()+" with type "+inputType);
        
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                for(PipelineResult result : run.getResults()) {
                    addFilepathToInputList(result);
                }
            }
        }
   
        processData.putItem("INPUT_PATH_LIST", new ArrayList<>(inputFiles));
        
        if (inputFiles.isEmpty()) {
            contextLogger.info("Nothing to be done.");
        }
        else {
            contextLogger.info("Processed "+inputFiles.size()+" filepaths.");
        }
    }
    
    private void addFilepathToInputList(PipelineResult result) throws ComputeException {

        String llpath = DomainUtils.getFilepath(result, FileType.LosslessStack);
        String vlpath = DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack);
        String filepath = llpath;
        
        if (filepath==null || !filepath.endsWith(inputType)) {
            return;
        }
        
        if (outputType.equals("pbd")) {
            if (llpath!=null && llpath.endsWith(outputType)) {
                // The PBD already exists
                contextLogger.info("Result already has correct type: "+vlpath);
                return;
            }
        }
        else if (outputType.equals("h5j")) {
            if (vlpath!=null && vlpath.endsWith(outputType)) {
                // The H5J already exists
                contextLogger.info("Result already has correct type: "+vlpath);
                return;
            }
        }

        File file = new File(filepath);
        
        if (filepath.startsWith(DomainConstants.SCALITY_PATH_PREFIX)) {
            contextLogger.warn("Cannot process file in Scality: "+llpath);
            // TODO: implement compression of files in Scality
            return;
        }

        if (!filepath.startsWith(centralDir)) {
            contextLogger.warn("Sample has path outside of filestore: "+sample.getId());
            return;
        }
        
        if (isExcluded(file.getName())) {
            contextLogger.debug("Excluding file: "+filepath);
            return;
        }
        
        if (!file.exists()) {
            contextLogger.warn("Entity file does not exist: "+filepath);
            return;
        }
        
        contextLogger.info("Will compress file: "+filepath);
    	inputFiles.add(filepath);
    }

	private boolean isExcluded(String filename) {		
		for(Pattern p : exclusions) {
			Matcher m = p.matcher(filename);
			if (m.matches()) {
				return true;
			}
		}
		return false;
    }
    
    private void doCreateOutputList() throws ComputeException {

        List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");
        List<String> outputPaths = new ArrayList<String>();
        
        for(String filepath : inputPaths) {
            String extension = getExtension(filepath);
            outputPaths.add(filepath.replaceAll(extension, outputType));
        }
        
        processData.putItem("OUTPUT_PATH_LIST", outputPaths);
    }
    
    private void doComplete() throws ComputeException {

        this.deleteSourceFiles = !"false".equals(data.getRequiredItem("DELETE_INPUTS"));
    	List<String> inputPaths = (List<String>)data.getRequiredItem("INPUT_PATH_LIST");
        List<String> outputPaths = (List<String>)data.getRequiredItem("OUTPUT_PATH_LIST");
    	
    	for(int i=0; i<inputPaths.size(); i++) {
    		String inputPath = inputPaths.get(i);
            String outputPath = outputPaths.get(i);
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
                contextLogger.warn("Missing or corrupt output file: "+outputFile);
            }
            else {
                try {
                    updateEntities(inputPath, outputPath);
                }
                catch (Exception e) {
                    logger.error("Error updating samples for "+inputPath,e);
                }
            }
    	}

    	contextLogger.info("Modified "+numChanges+" entities.");
    }
    
    private void updateEntities(String inputPath, String outputPath) throws Exception {
        
        String inputExtension = getExtension(inputPath);
        String outputExtension = getExtension(outputPath);
        
		// Check to make sure we generated the file
		File outputFile = new File(outputPath);
        if (!outputFile.exists() || !outputFile.canRead() || outputFile.length()<=0) {
            contextLogger.warn("Missing or corrupt output file: "+outputFile);
            return;
        }
        
        int numUpdated = 0;

        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                for(PipelineResult result : run.getResults()) {
                    String llpath = DomainUtils.getFilepath(result, FileType.LosslessStack);
                    String vlpath = DomainUtils.getFilepath(result, FileType.VisuallyLosslessStack);
                    
                    if (llpath!=null && llpath.equals(inputPath)) {
                        if ("h5j".equals(outputExtension)) {
                            if (vlpath!=null) {
                                logger.warn("Overriding VL stack: "+vlpath);
                                // TODO: delete the existing stack?
                            }
                            DomainUtils.setFilepath(result, FileType.VisuallyLosslessStack, outputPath);    
                        }
                        else {
                            DomainUtils.setFilepath(result, FileType.LosslessStack, outputPath);
                        }
                        
                        numUpdated++;
                    }
                    
                }
            }
        }

        sampleHelper.saveSample(sample);
        contextLogger.info("Updated "+numUpdated+" filepaths to use new compressed file "+outputPath+" in sample "+sample);
    	deleteIfNecessary(inputPath);
	}
    
    private void deleteIfNecessary(String filepath) {
    	
        if (!deleteSourceFiles) return;
        
        if (!filepath.startsWith(centralDir)) {
            contextLogger.warn("Path outside of filestore: "+filepath);
            return;
        }
        
        if (!isDebug) {
            if (filepath.startsWith(DomainConstants.SCALITY_PATH_PREFIX)) {
            	// TODO: use JFS to delete the path
            	contextLogger.warn("Deletion from Scality is not yet implemented! This file should be deleted: "+filepath);
            }
            else {
                File file = new File(filepath);
                try {
                    FileUtils.forceDelete(file);
                    contextLogger.info("Deleted old file: "+filepath);
                }
                catch (Exception e) {
                    logger.info("Error deleting file "+filepath,e);
                }
            }
        }
    }

    private String getExtension(String filepath) {
        int dot = filepath.indexOf('.');
        if (dot>0) {
            return filepath.substring(dot+1);
        }
        return "";
    }
}
