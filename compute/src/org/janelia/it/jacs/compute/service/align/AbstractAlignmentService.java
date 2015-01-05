package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Base class for all alignment algorithms. Parameters:
 *   ALIGN_RESULT_FILE_NODE - the node for grid calculations
 *   OUTPUT_FILE_NODE - the node where the output should finally go
 *   SAMPLE_ENTITY_ID - the id of the sample to be aligned
 *   SAMPLE_AREAS - the sample areas within the sample to be processed
 *  
 * Outputs:
 *   ALIGNED_FILENAMES - a list of all of the aligned output files
 *   ALIGNED_FILENAME - the main aligned output file
 *   CHANNEL_SPEC - the channel specification for the main aligned output file
 *   SIGNAL_CHANNELS - the signal channels for the main aligned output file
 *   REFERENCE_CHANNEL - the reference channels for the main aligned output file
 *  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractAlignmentService extends SubmitDrmaaJobService implements Aligner {

    protected static final String ARCHIVE_PREFIX = "/archive";
    
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected String ownerKey;
    protected SampleHelper sampleHelper;
    protected EntityBeanEntityLoader entityLoader;
    
    protected Entity sampleEntity;
    protected String alignedArea;
    protected String gender;
    
    protected boolean warpNeurons;
    protected AlignmentInputFile input1;
    protected AlignmentInputFile input2;
    protected List<String> archivedFiles = new ArrayList<>();
    protected List<String> targetFiles = new ArrayList<>();
    
    // ****************************************************************************************************************
    // When this service is run with the Aligner interface method, it determines and outputs the alignment inputs
    // ****************************************************************************************************************
    
    @Override
    public void populateInputVariables(IProcessData processData) throws ServiceException {

        try {
            // From SubmitDrammaJobService
            super.initLoggersAndData(processData);
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            
            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger, contextLogger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            
            this.sampleEntity = sampleHelper.getRequiredSampleEntity(data);
            this.warpNeurons = !"false".equals((String)processData.getItem("WARP_NEURONS"));

            @SuppressWarnings("unchecked")
            List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) data.getItem("SAMPLE_AREAS");
            if (sampleAreas != null) {
            	populateInputs(sampleAreas);
            }

            List<AlignmentInputFile> alignmentInputFiles = new ArrayList<>();

            if (input1!=null) {
                logInputFound("input stack 1", input1); 
                checkForArchival(input1);
                alignmentInputFiles.add(input1);    
            }
            
            if (input2!=null) {
                logInputFound("input stack 2", input2); 
                checkForArchival(input2);
                alignmentInputFiles.add(input2);
            }
            
            if (!alignmentInputFiles.isEmpty()) {
	            setConsensusValues();
	            putOutputVars(input1.getChannelSpec(), input1.getChannelColors(), alignmentInputFiles);
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
            if ("Brain".equalsIgnoreCase(areaName) || "".equals(areaName)) {
                Entity result = entityBean.getEntityById(anatomicalArea.getSampleProcessingResultId());
                entityLoader.populateChildren(result);
                if (result!=null) {
                    if (alignedArea!=null) {
                        contextLogger.warn("Found more than one default brain area to align. Using: "+alignedArea);
                    }
                    else {
                        Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                        this.alignedArea = areaName;    
                        input1 = new AlignmentInputFile();
                        input1.setPropertiesFromEntity(image);
                        if (warpNeurons) {
                            input1.setInputSeparationFilename(getConsolidatedLabel(result));
                        }
                    }
                }
            }
        }
    }

    protected void setConsensusValues() throws Exception {

        setConsensusValues(input1);
        setConsensusValues(input2);
        
        this.gender = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
        if (gender!=null) {
            contextLogger.info("Found gender consensus: "+gender);
        }
    }
    
    protected void setConsensusValues(AlignmentInputFile input) throws Exception {

    	if (input==null) return;
    	
        if (input.getOpticalResolution()==null) {
            // Interoperability with legacy samples
            contextLogger.warn("No optical resolution on the input file. Trying to find a consensus among the LSMs...");
            input.setOpticalResolution(sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, alignedArea));
            if (input.getOpticalResolution()!=null) {
                contextLogger.info("Found optical resolution consensus: "+input.getOpticalResolution());
            }
        }
        
        if (input.getPixelResolution()==null) {
            // Interoperability with legacy samples
            contextLogger.warn("No pixel resolution on the input file. Trying to find a consensus among the LSMs...");
            input.setPixelResolution(sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, alignedArea));
            if (input.getPixelResolution()!=null) {
                contextLogger.info("Found pixel resolution consensus: "+input.getPixelResolution());
            }
        }
        
        if (input.getChannelColors()!=null) {
            contextLogger.info("  Channel colors: "+input.getChannelColors());
        }
    }
    
    protected void logInputFound(String type, AlignmentInputFile input) {
        contextLogger.info("Found " + type + ": " + input);
    }

    protected void putOutputVars(String chanSpec, String channelColors, List<AlignmentInputFile> alignmentInputFiles) {
        data.putItem("CHANNEL_SPEC", chanSpec);
        final String signalChannels = sampleHelper.getSignalChannelIndexes(chanSpec);
        data.putItem("SIGNAL_CHANNELS", signalChannels);
        final String referenceChannels = sampleHelper.getReferenceChannelIndexes(chanSpec);
        data.putItem("REFERENCE_CHANNEL", referenceChannels);
        data.putItem("CHANNEL_COLORS", channelColors);
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
    }
    
    protected String getConsolidatedLabel(Entity result) throws Exception {

        Entity separation = EntityUtils.findChildWithType(result, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT);
        if (separation!=null) {
            entityLoader.populateChildren(separation);
            Entity nsSupportingFiles = EntityUtils.getSupportingData(separation);
            if (nsSupportingFiles!=null) { 
                entityLoader.populateChildren(nsSupportingFiles);
                Entity labelFile = EntityUtils.findChildWithNameAndType(nsSupportingFiles, "ConsolidatedLabel.v3dpbd", EntityConstants.TYPE_IMAGE_3D);
                if (labelFile==null) {
                    labelFile = EntityUtils.findChildWithNameAndType(nsSupportingFiles, "ConsolidatedLabel.v3draw", EntityConstants.TYPE_IMAGE_3D);
                }
                if (labelFile!=null) {
                    return labelFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH); 
                }   
            }
        }
        return null;
    }

    protected void checkForArchival(AlignmentInputFile input) throws Exception {

        if (input.getInputFilename().startsWith(ARCHIVE_PREFIX)) {
            archivedFiles.add(input.getInputFilename());
            String newInput = new File(resultFileNode.getDirectoryPath(), new File(input.getInputFilename()).getName()).getAbsolutePath();
            targetFiles.add(newInput);
            input.setInputFilename(newInput);
        }
        
        if (input.getInputSeparationFilename()!=null) {
            if (input.getInputSeparationFilename().startsWith(ARCHIVE_PREFIX)) {
                archivedFiles.add(input.getInputSeparationFilename());
                String newInputSeperation = new File(resultFileNode.getDirectoryPath(), new File(input.getInputSeparationFilename()).getName()).getAbsolutePath();
                targetFiles.add(newInputSeperation);
                input.setInputSeparationFilename(newInputSeperation);
            }
        }
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

            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger, contextLogger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            
            this.sampleEntity = sampleHelper.getRequiredSampleEntity(data);

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
    
    @Override
	public void postProcess() throws MissingDataException {

        File outputFile = new File(resultFileNode.getDirectoryPath(),"Aligned.v3draw");
        
        File alignDir = new File(resultFileNode.getDirectoryPath());
    	File[] coreFiles = alignDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});

    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Brain alignment core dumped for "+resultFileNode.getDirectoryPath());
    	}

    	if (! outputFile.exists()) {
    		throw new MissingDataException("Output file not found: "+outputFile.getAbsolutePath());
    	}
	}
}
