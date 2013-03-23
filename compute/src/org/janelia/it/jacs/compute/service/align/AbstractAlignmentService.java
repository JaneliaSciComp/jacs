package org.janelia.it.jacs.compute.service.align;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
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
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.vo.ParameterException;

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
    protected String inputFilename;
    protected String channelSpec;
    protected String opticalResolution;
    protected String gender;
    protected Integer refChannel;
    protected Integer refChannelOneIndexed;
    protected File outputFile;
    
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
            
            String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
            if (sampleEntityId == null || "".equals(sampleEntityId)) {
                throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
            }
            
            this.sampleEntity = entityBean.getEntityById(sampleEntityId);
            if (sampleEntity == null) {
                throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
            }
            
            List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>)processData.getItem("SAMPLE_AREAS");
            if (sampleAreas == null) {
                throw new IllegalArgumentException("SAMPLE_AREAS may not be null");
            }
            
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
                            this.inputFilename = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                            this.channelSpec = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
                        }
                    }
                }
            }
    
            if (alignedArea!=null) {
                logger.info("Found sample area to align: "+alignedArea);
                logger.info("  Input filename: "+inputFilename);
                logger.info("  Input channel spec: "+channelSpec);
                
                if (channelSpec.contains("r")) {
                    this.refChannel = channelSpec.indexOf('r');
                    this.refChannelOneIndexed = refChannel + 1;
                    logger.info("  Reference channel: "+refChannel);
                    logger.info("  Reference channel (one-indexed): "+refChannelOneIndexed);
                    
                    String signalChannels = sampleHelper.getSignalChannelIndexes(channelSpec);
                    String referenceChannels = sampleHelper.getReferenceChannelIndexes(channelSpec);
                    
                    logger.info("Putting '"+channelSpec+"' in CHANNEL_SPEC");
                    processData.putItem("CHANNEL_SPEC", channelSpec);
                    logger.info("Putting '"+signalChannels+"' in SIGNAL_CHANNELS");
                    processData.putItem("SIGNAL_CHANNELS", signalChannels);
                    logger.info("Putting '"+referenceChannels+"' in REFERENCE_CHANNEL");
                    processData.putItem("REFERENCE_CHANNEL", referenceChannels);
                }
            }
            
            this.gender = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_GENDER, alignedArea);
            if (gender!=null) {
                logger.info("Found gender consensus: "+gender);
            }
            
            this.opticalResolution = sampleHelper.getConsensusLsmAttributeValue(sampleEntity, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, alignedArea);
            if (opticalResolution!=null) {
                opticalResolution = opticalResolution.replaceAll("x", " ");
                logger.info("Found optical resolution consensus: "+opticalResolution);
            }
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        
        if (inputFilename==null) {
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
