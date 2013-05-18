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
import org.janelia.it.jacs.compute.drmaa.DrmaaHelper;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.launcher.archive.ArchiveAccessHelper;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Base class for all alignment algorithms. Parameters:
 *   ALIGN_RESULT_FILE_NODE - the node for grid calculations
 *   OUTPUT_FILE_NODE - the node where the output should finally go
 *   SAMPLE_ENTITY_ID - the id of the sample to be aligned
 *   SAMPLE_AREAS - the sample areas within the sample
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
public abstract class AbstractAlignmentService extends SubmitDrmaaJobService {
	
	protected static final String CONFIG_PREFIX = "alignConfiguration.";
	protected static final int TIMEOUT_SECONDS = 3600;  // 60 minutes
	
    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");

    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;
    protected String ownerKey;
    protected SampleHelper sampleHelper;
    protected EntityBeanEntityLoader entityLoader;
    
    protected Entity sampleEntity;
    protected String alignedArea;
    protected String gender;
    protected File outputFile;
    
    protected boolean warpNeurons;
    protected AlignmentInputFile input1;
    protected AlignmentInputFile input2;
    
    @Override
    protected String getGridServicePrefixName() {
        return "align";
    }
    
    @Override
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	try {
            this.entityBean = EJBFactory.getLocalEntityBean();
            this.computeBean = EJBFactory.getLocalComputeBean();
            this.annotationBean = EJBFactory.getLocalAnnotationBean();
            String ownerName = ProcessDataHelper.getTask(processData).getOwner();
            Subject subject = computeBean.getSubjectByNameOrKey(ownerName);
            this.ownerKey = subject.getKey();
            this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
            this.entityLoader = new EntityBeanEntityLoader(entityBean);
            
            this.warpNeurons = !"false".equals((String)processData.getItem("WARP_NEURONS"));
            
            String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
            if (sampleEntityId == null || "".equals(sampleEntityId)) {
                throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
            }
            
            this.sampleEntity = entityBean.getEntityById(sampleEntityId);
            if (sampleEntity == null) {
                throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
            }
            
            List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>)processData.getItem("SAMPLE_AREAS");
            if (sampleAreas != null) {
                // The naive implementation tries to find the default brain area to align. Subclasses may have a different
                // strategy for finding input files and other parameters.
                for(AnatomicalArea anatomicalArea : sampleAreas) {
                    String areaName = anatomicalArea.getName();
                    if ("Brain".equalsIgnoreCase(areaName) || "".equals(areaName)) {
                        Entity result = entityBean.getEntityById(anatomicalArea.getSampleProcessingResultId());
                        entityLoader.populateChildren(result);
                        if (result!=null) {
                            if (alignedArea!=null) {
                                logger.warn("Found more than one default brain area to align. Using: "+alignedArea);
                            }
                            else {
                                Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
                                this.alignedArea = areaName;    
                                input1 = new AlignmentInputFile();
                                input1.setPropertiesFromEntity(image);
                                if (warpNeurons) input1.setInputSeparationFilename(getConsolidatedLabel(result));
                            }
                        }
                    }
                }
            }
            
            if (input1!=null) {
                logInputFound("input stack", input1); 
                logger.info("  Sample area: "+alignedArea);
                
                if (input1.getOpticalResolution()==null) {
                    // Interoperability with legacy samples
                    logger.warn("No optical resolution on the input file. Trying to find a consensus among the LSMs...");
                    input1.setOpticalResolution(sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, alignedArea));
                    if (input1.getOpticalResolution()!=null) {
                        logger.info("Found optical resolution consensus: "+input1.getOpticalResolution());
                    }
                }
                else {
                    logger.info("  Optical resolution: "+input1.getOpticalResolution());
                }
                
                if (input1.getPixelResolution()==null) {
                    // Interoperability with legacy samples
                    logger.warn("No pixel resolution on the input file. Trying to find a consensus among the LSMs...");
                    input1.setPixelResolution(sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, alignedArea));
                    if (input1.getPixelResolution()!=null) {
                        logger.info("Found pixel resolution consensus: "+input1.getPixelResolution());
                    }
                }
                else {
                    logger.info("  Pixel resolution: "+input1.getPixelResolution());
                }

                if (input1.getChannelColors()!=null) {
                    logger.info("  Channel colors: "+input1.getChannelColors());
                }
                
                this.gender = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
                if (gender!=null) {
                    logger.info("Found gender consensus: "+gender);
                }
                
                putOutputVars(input1.getChannelSpec(), input1.getChannelColors());
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    protected void logInputFound(String type, AlignmentInputFile input) {
        logger.info("Found "+type+": ");
        logger.info("  Input filename: "+input.getInputFilename());
        logger.info("  Input channel spec: "+input.getChannelSpec());
        logger.info("  Input channel colors: "+input.getChannelColors());
        logger.info("  Input separation to warp: "+input.getInputSeparationFilename());
        logger.info("  Reference channel: "+input.getRefChannel());
        logger.info("  Reference channel (one-indexed): "+input.getRefChannelOneIndexed());    
        logger.info("  Optical resolution: "+input.getOpticalResolution());
        logger.info("  Pixel resolution: "+input.getPixelResolution());
    }
    
    protected void putOutputVars(String chanSpec, String channelColors) {
        logger.info("Putting '"+chanSpec+"' in CHANNEL_SPEC");
        processData.putItem("CHANNEL_SPEC", chanSpec);
        String signalChannels = sampleHelper.getSignalChannelIndexes(chanSpec);
        logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
        processData.putItem("SIGNAL_CHANNELS", signalChannels);
        String referenceChannels = sampleHelper.getReferenceChannelIndexes(chanSpec);
        logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
        processData.putItem("REFERENCE_CHANNEL", referenceChannels);
        logger.info("Putting '"+channelColors+"' in CHANNEL_COLORS");
        processData.putItem("CHANNEL_COLORS", channelColors);
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
    
    @Override
    protected SerializableJobTemplate prepareJobTemplate(DrmaaHelper drmaa) throws Exception {
        
        // Copy input files over from archive if necessary
        if (input1!=null) copyFromArchive(input1);
        if (input2!=null) copyFromArchive(input2);
        
        return super.prepareJobTemplate(drmaa);
    }
    
    private void copyFromArchive(AlignmentInputFile input) throws Exception {
        if (!input.getInputFilename().startsWith("/archive")) return;
        
        List<String> archivedFiles = new ArrayList<String>();
        List<String> targetFiles = new ArrayList<String>();
        archivedFiles.add(input.getInputFilename());
        
        String newInput = new File(resultFileNode.getDirectoryPath(), new File(input.getInputFilename()).getName()).getAbsolutePath();
        targetFiles.add(newInput);
        if (input.getInputSeparationFilename()!=null) {
            archivedFiles.add(input.getInputSeparationFilename());
            String newInputSeperation = new File(resultFileNode.getDirectoryPath(), new File(input.getInputSeparationFilename()).getName()).getAbsolutePath();
            targetFiles.add(newInputSeperation);
        }
        
        logger.info("Sending archive message");
        ArchiveAccessHelper.sendCopyFromArchiveMessage(archivedFiles, targetFiles);
        
        logger.debug("Waiting for files to appear");
        if (!FileUtil.waitForFiles(targetFiles, TIMEOUT_SECONDS*1000)) {
            throw new ServiceException("TImed out after waiting "+TIMEOUT_SECONDS+" secs for file from archive");
        }
        
        logger.info("Retrieved necessary files for archive for alignment input "+input.getInputFilename());
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

        this.outputFile = new File(resultFileNode.getDirectoryPath(),"Aligned.v3draw");
        
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

    	if (outputFile!=null && !outputFile.exists()) {
    		throw new MissingDataException("Output file not found: "+outputFile.getAbsolutePath());
    	}
	}
}
