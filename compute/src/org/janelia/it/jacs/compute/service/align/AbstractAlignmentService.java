package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.StringUtils;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * Base class for all alignment algorithms. Parameters:
 *   ALIGN_RESULT_FILE_NODE - the node for grid calculations
 *   OUTPUT_FILE_NODE - the node where the output should finally go
 *   SAMPLE_ENTITY_ID - the id of the sample to be aligned
 *   SAMPLE_AREAS - the sample areas within the sample to be processed
 *  
 * Outputs:
 *   ALIGNED_IMAGES - a list of all of the aligned output files as ImageStack objects
 *   ALIGNED_FILENAME - the main aligned output file
 *  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractAlignmentService extends SubmitDrmaaJobService implements Aligner {

	protected static final String BRAIN_AREA = "Brain";
	protected static final String VNC_AREA = "VNC";
	
    protected static final String ARCHIVE_PREFIX = "/archive";
    
    protected ComputeBeanLocal computeBean;
    protected String ownerKey;
    protected SampleHelperNG sampleHelper;

    protected Sample sample;
    protected ObjectiveSample objectiveSample;
    protected List<AnatomicalArea> alignedAreas = new ArrayList<AnatomicalArea>();
    protected String gender;
    
    protected boolean warpNeurons;
    protected AlignmentInputFile input1;
    protected AlignmentInputFile input2;
    protected List<String> archivedFiles = new ArrayList<>();
    protected List<String> targetFiles = new ArrayList<>();
    protected boolean runAligner = true;
    protected List<AlignmentInputFile> regenerateInputs = new ArrayList<>();
    
    // ****************************************************************************************************************
    // When this service is run with the Aligner interface method, it determines and outputs the alignment inputs
    // ****************************************************************************************************************
    
    @Override
    public void populateInputVariables(IProcessData processData) throws ServiceException {

        try {
            // From SubmitDrammaJobService
            super.initLoggersAndData(processData);
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            
            this.computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
            
            this.sample = sampleHelper.getRequiredSample(data);
            this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
            this.warpNeurons = !"false".equals((String)processData.getItem("WARP_NEURONS"));

            @SuppressWarnings("unchecked")
            List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getItem("SAMPLE_AREAS");
            if (sampleAreas != null) {
            	populateInputs(sampleAreas);
            }

            List<AlignmentInputFile> alignmentInputFiles = new ArrayList<>();

            if (input1!=null) {
                logInputFound("input stack 1", input1); 
                checkInput(input1);
                alignmentInputFiles.add(input1);    
            }
            
            if (input2!=null) {
                logInputFound("input stack 2", input2); 
                checkInput(input2);
                alignmentInputFiles.add(input2);
            }
            
            if (!alignmentInputFiles.isEmpty()) {
	            setLegacyConsensusValues();
	            putOutputVars(alignmentInputFiles);
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    /**
     * The naive implementation tries to find the default brain area to align. Subclasses may have a different strategy 
     * for finding input files and other parameters.
     * @param sampleAreas
     * @throws Exception
     */
    protected void populateInputs(List<AnatomicalArea> sampleAreas) throws Exception {
        // The naive implementation tries to find the default brain area to align. Subclasses may have a different
        // strategy for finding input files and other parameters.
        for(AnatomicalArea anatomicalArea : sampleAreas) {
            String areaName = anatomicalArea.getName();
            SampleProcessingResult result = getLatestResultOfType(objectiveSample, areaName);
        	if (result!=null) {
	            if (BRAIN_AREA.equalsIgnoreCase(areaName) || StringUtils.isEmpty(areaName)) {
	                if (!alignedAreas.isEmpty()) {
	                    contextLogger.warn("Found more than one default brain area to align. Using: "+alignedAreas.get(0).getName());
	                }
	                else {
	                    String imagePath = DomainUtils.getDefault3dImageFilePath(result);
	                    alignedAreas.add(anatomicalArea);
	                    input1 = new AlignmentInputFile();
	                    input1.setPropertiesFromEntity(result);
	                    input1.setSampleId(sample.getId());
	                    input1.setObjective(objectiveSample.getObjective());
	                    if (warpNeurons) {
	                        input1.setInputSeparationFilename(getConsolidatedLabel(result));
	                    }
	                }
	            }
        	}
        }
        
        if (input1==null) {
        	contextLogger.info("No input found, will not run aligner.");
        	runAligner = false;
        }
    }

    protected void setLegacyConsensusValues() throws Exception {
        setLegacyConsensusValues(input1);
        setLegacyConsensusValues(input2);
    }

    /** 
     * Interoperability with legacy samples
     * @param input
     * @throws Exception
     */
    protected void setLegacyConsensusValues(AlignmentInputFile input) throws Exception {

    	if (input==null) return;

        if (input.getObjective()==null) {
            contextLogger.warn("No objective on the input file. Trying to find a consensus among the LSMs...");
            input.setChannelColors(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, "objective"));
            if (input.getObjective()!=null) {
                contextLogger.info("Found objective consensus: "+input.getObjective());
            }
        }
        
        if (input.getChannelColors()==null) {
            contextLogger.warn("No channel colors on the input file. Trying to find a consensus among the LSMs...");
            input.setChannelColors(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, "channelColors"));
            if (input.getChannelColors()!=null) {
                contextLogger.info("Found channel colors consensus: "+input.getChannelColors());
            }
        }
        
        if (input.getChannelSpec()==null) {
            contextLogger.warn("No channel spec on the input file. Trying to find a consensus among the LSMs...");
            input.setChannelSpec(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, "channelSpec"));
            if (input.getChannelColors()!=null) {
                contextLogger.info("Found channel spec consensus: "+input.getChannelSpec());
            }
        }

        if (input.getOpticalResolution()==null) {
            contextLogger.warn("No optical resolution on the input file. Trying to find a consensus among the LSMs...");
            input.setOpticalResolution(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, "opticalResolution"));
            if (input.getOpticalResolution()!=null) {
                contextLogger.info("Found optical resolution consensus: "+input.getOpticalResolution());
            }
        }
        
        if (input.getPixelResolution()==null) {
            contextLogger.warn("No pixel resolution on the input file. Trying to find a consensus among the LSMs...");
            input.setPixelResolution(sampleHelper.getConsensusLsmAttributeValue(alignedAreas, "imageSize"));
            if (input.getPixelResolution()!=null) {
                contextLogger.info("Found pixel resolution consensus: "+input.getPixelResolution());
            }
        }
    }
    
    protected void logInputFound(String type, AlignmentInputFile input) {
        contextLogger.info("Found " + type + ": " + input);
    }

    protected void putOutputVars(List<AlignmentInputFile> alignmentInputFiles) {
        data.putItem("ALIGNMENT_INPUTS", alignmentInputFiles);
        
        if (!archivedFiles.isEmpty()) {
            data.putItem("COPY_FROM_ARCHIVE", Boolean.TRUE);
            data.putItem("SOURCE_FILE_PATHS", Task.csvStringFromCollection(archivedFiles));
            data.putItem("TARGET_FILE_PATHS", Task.csvStringFromCollection(targetFiles));
        }
        else {
            data.putItem("COPY_FROM_ARCHIVE", Boolean.FALSE);
            data.putItem("SOURCE_FILE_PATHS", null);
            data.putItem("TARGET_FILE_PATHS", null);
        }
        
        data.putItem("ALIGNED_AREAS", alignedAreas);
        data.putItem("RUN_ALIGNER", runAligner);
        if (regenerateInputs!=null) {
            contextLogger.info("Will regenerate "+regenerateInputs.size()+" H5J inputs");
            // Try to order by objective, since the smaller files will process faster
            Collections.sort(regenerateInputs, new Comparator<AlignmentInputFile>() {
                @Override
                public int compare(AlignmentInputFile o1, AlignmentInputFile o2) {
                    return ComparisonChain.start()
                            .compare(o1.getObjective(), o2.getObjective(), Ordering.natural())
                            .compare(o1.getFilepath(), o2.getFilepath(), Ordering.natural()).result();
                }
            });
            data.putItem("REGENERATE_INPUT", regenerateInputs);
        }
    }

    protected SampleProcessingResult getLatestResultOfType(ObjectiveSample objectiveSample, String anatomicalArea) throws Exception {

        contextLogger.debug("Looking for latest result with anatomicalArea="+anatomicalArea);
        
        for(SamplePipelineRun pipelineRun : Lists.reverse(objectiveSample.getPipelineRuns())) {

            contextLogger.debug("  Check pipeline run "+pipelineRun.getName()+" (id="+pipelineRun.getId()+")");

            if (pipelineRun.hasError()) {
                continue;
            }
            
            for(SampleProcessingResult result : Lists.reverse(pipelineRun.getSampleProcessingResults())) {

                contextLogger.debug("    Check result "+result.getName()+" (id="+result.getId()+")");
            
                if (anatomicalArea==null || anatomicalArea.equalsIgnoreCase(result.getAnatomicalArea())) {
                    return result;
                }
            }   
        }
        return null;
    }
    
    protected String getConsolidatedLabel(SampleProcessingResult result) throws Exception {
        NeuronSeparation separation = result.getLatestSeparationResult();
        if (separation==null) return null;
        File labelFile = new File(separation.getFilepath(), "ConsolidatedLabel.v3dpbd");
        return labelFile.getAbsolutePath();
    }

    protected void checkInput(AlignmentInputFile input) throws Exception {

        if (pathIsArchived(input.getFilepath())) {
            archivedFiles.add(input.getFilepath());
            String newInput = new File(resultFileNode.getDirectoryPath(), new File(input.getFilepath()).getName()).getAbsolutePath();
            targetFiles.add(newInput);
            input.setFilepath(newInput);
        }
        
        if (input.getInputSeparationFilename()!=null) {
            if (pathIsArchived(input.getInputSeparationFilename())) {
                archivedFiles.add(input.getInputSeparationFilename());
                String newInputSeperation = new File(resultFileNode.getDirectoryPath(), new File(input.getInputSeparationFilename()).getName()).getAbsolutePath();
                targetFiles.add(newInputSeperation);
                input.setInputSeparationFilename(newInputSeperation);
            }
        }
        
        if (input.getFilepath().endsWith(".h5j")) {
            regenerateInputs.add(input);
        }
    }
    
    private boolean pathIsArchived(String filepath) {
    	return filepath.startsWith(ARCHIVE_PREFIX) || filepath.startsWith(EntityConstants.SCALITY_PATH_PREFIX);
    }
    
    // ****************************************************************************************************************
    // When this service is run as a grid submission, it runs the aligner
    // ****************************************************************************************************************

    protected static final String CONFIG_PREFIX = "alignConfiguration.";
    protected static final int TIMEOUT_SECONDS = 60*30; // 30 minutes 
    
    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");

    @Override
    protected String getGridServicePrefixName() {
        return "align";
    }
    
    @Override
    protected void init(IProcessData processData) throws Exception {

        try {
            super.init(processData);

            this.computeBean = EJBFactory.getLocalComputeBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
            
            this.sample = sampleHelper.getRequiredSample(data);
            this.alignedAreas = (List<AnatomicalArea>) data.getItem("ALIGNED_AREAS");
            
            @SuppressWarnings("unchecked")
            List<AlignmentInputFile> alignmentInputs = (List) data.getRequiredItem("ALIGNMENT_INPUTS");

            final int numberOfAlignmentInputs = alignmentInputs.size();
            if ((numberOfAlignmentInputs < 1) || (numberOfAlignmentInputs > 2)) {
                throw new IllegalArgumentException("invalid number of ALIGNMENT_INPUTS (" +
                                                   numberOfAlignmentInputs + "): only 1 or 2 may be specified");
            }

            this.input1 = alignmentInputs.get(0);
            if (numberOfAlignmentInputs > 1) {
                this.input2 = alignmentInputs.get(1);
            }
            
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        if (input1==null) {
            throw new ServiceException("No input file was specified for alignment");
        }
        
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the alignment pipeline.");
        }
        createShellScript(writer);
        setJobIncrementStop(1);
    }

    protected abstract void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException;

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
    
	/**
	 * Every subclass must override this to verify its results and
	 * populate ALIGNED_IMAGES and ALIGNED_FILENAME.
	 */
    @Override
	public abstract void postProcess() throws MissingDataException;
}
