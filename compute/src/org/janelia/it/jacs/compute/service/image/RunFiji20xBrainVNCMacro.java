package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.ChanSpecUtils;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.DetectionChannel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.IlluminationChannel;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata.Track;

/**
 * Runs a Fiji macro that requires a 20x set of LSMs (Brain, and possibly VNC) as input.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunFiji20xBrainVNCMacro extends AbstractEntityGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String FIJI_BIN_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Bin.Path");
    protected static final String FIJI_MACRO_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Macro.Path");
    protected static final String SCRATCH_DIR = SystemConfigurationProperties.getString("computeserver.ClusterScratchDir");

	private static final int START_DISPLAY_PORT = 890;
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "fijiConfiguration.";
    private static final String DETECTION_CHANNEL_DYE_PREFIX = "Alexa Fluor ";
    
    private String macroName;
    private String outputFilePrefix;
    private Entity sampleEntity;
    private Entity pipelineRun;
    private Entity brainLsm;
    private Entity vncLsm;
    private String jsonFilepath;
    private Integer power;
    private Integer gain;
    private String brainFilepath = "";
    private String vncFilepath = "";
    private String chanSpec;

    @Override
    protected void init() throws Exception {

    	this.macroName = data.getRequiredItemAsString("MACRO_NAME");
        String sampleEntityId = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
        
        sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }

        String pipelineRunEntityId = data.getRequiredItemAsString("PIPELINE_RUN_ENTITY_ID");
        pipelineRun = entityBean.getEntityById(pipelineRunEntityId);
        if (pipelineRun == null) {
            throw new IllegalArgumentException("Pipeline run entity not found with id="+pipelineRunEntityId);
        }
        
    	String sampleName = sampleEntity.getName();
    	if (sampleName.contains("~")) {
    		sampleName = sampleName.substring(0, sampleName.indexOf('~'));
    	}
        this.outputFilePrefix = sampleName;
    	
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .childrenOfType(EntityConstants.TYPE_LSM_STACK)
                .run(new EntityVisitor() {
            public void visit(Entity lsmStack) throws Exception {
                String area = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
                if ("VNC".equalsIgnoreCase(area)) {
                    vncLsm = lsmStack;
                } 
                else if ("Brain".equalsIgnoreCase(area)) {
                    brainLsm = lsmStack;
                }
            }
        });
        
        if (brainLsm!=null) {
            registerLsmAttributes(sampleEntity, brainLsm);
            brainFilepath = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        }
        
        if (vncLsm!=null) {
            registerLsmAttributes(sampleEntity, vncLsm);
            vncFilepath = vncLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        }
        
        chanSpec = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
        if (chanSpec==null) {
        	String numChannelsStr = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_NUM_CHANNELS);
        	if (numChannelsStr==null) {
            	throw new IllegalStateException("Brain LSM does not specify chanSpec or numChannels");
        	}
        	int numChannels = Integer.parseInt(numChannelsStr);
        	chanSpec = ChanSpecUtils.createChanSpec(numChannels, numChannels);
        }
        
        if (vncLsm!=null) {
            String vncChanSpec = vncLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
            if (!chanSpec.equals(vncChanSpec)) {
                logger.warn("Brain chanspec ("+chanSpec+") does not match VNC chanspec ("+vncChanSpec+"). Will continue with Brain chanspec.");
            }
        }
        
        entityBean.setOrUpdateValue(pipelineRun.getId(), EntityConstants.ATTRIBUTE_FILE_PATH, resultFileNode.getDirectoryPath());
        
        logger.info("Running Fiji macro "+macroName+" for sample "+sampleEntity.getName()+" (id="+sampleEntityId+")");
    }
    
    private void registerLsmAttributes(final Entity sampleEntity, final Entity lsm) throws Exception {
        // The actual filename of the LSM we're dealing with is not compressed
		final String lsmName = ArchiveUtils.getDecompressedFilepath(lsm.getName());
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfAttr(EntityConstants.ATTRIBUTE_RESULT) // LSM Summary Result, or Sample Processing Result for legacy samples 
                .childrenOfType(EntityConstants.TYPE_SUPPORTING_DATA).first()
                .childrenOfType(EntityConstants.TYPE_TEXT_FILE)
                .run(new EntityVisitor() {
            public void visit(Entity textFile) throws Exception {
                if (textFile.getName().startsWith(lsmName) && textFile.getName().endsWith(".json")) {
                    jsonFilepath = textFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                }
            }
        });
    
        if (jsonFilepath==null) {
            throw new IllegalStateException("No JSON metadata file found for LSM file "+lsm.getName());
        }
        
        String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA);
        if (!"Brain".equalsIgnoreCase(area) && !"VNC".equalsIgnoreCase(area)) {
            // Skip junk like VNC-verify and incorrectly annotated areas
            return;
        }

        String chanspec = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);

        LSMMetadata zm = LSMMetadata.fromFile(new File(jsonFilepath));

        List<IlluminationChannel> illChannels = new ArrayList<IlluminationChannel>();
        List<DetectionChannel> detChannels = new ArrayList<DetectionChannel>();

        for(Track track : zm.getNonBleachTracks()) {
            illChannels.addAll(track.getIlluminationChannels());
            detChannels.addAll(track.getDetectionChannels());
        }

        Collections.sort(illChannels, new Comparator<IlluminationChannel>() {
            @Override
            public int compare(IlluminationChannel o1, IlluminationChannel o2) {
                Double o1w = Double.parseDouble(o1.getWavelength());
                Double o2w = Double.parseDouble(o2.getWavelength());
                return o1w.compareTo(o2w);
            }
        });

        Collections.sort(detChannels, new Comparator<DetectionChannel>() {
            @Override
            public int compare(DetectionChannel o1, DetectionChannel o2) {
            	Double o1w = null;
            	if (o1.getDyeName().contains(DETECTION_CHANNEL_DYE_PREFIX)) {
            		o1w = Double.parseDouble(o1.getDyeName().substring(DETECTION_CHANNEL_DYE_PREFIX.length()));
            	}
            	else {
            		o1w = getWavelengthForDye(o1.getDyeName());
            	}
            	Double o2w = null;
            	if (o2.getDyeName().contains(DETECTION_CHANNEL_DYE_PREFIX)) {
            		o2w = Double.parseDouble(o2.getDyeName().substring(DETECTION_CHANNEL_DYE_PREFIX.length()));
            	}
            	else {
            		o2w = getWavelengthForDye(o2.getDyeName());
            	}
                return o1w.compareTo(o2w);
            }
        });

        int signalIndex = chanspec.indexOf('s');
        if (signalIndex<0) {
        	throw new IllegalStateException("Image must have a signal channel");
        }
        Float powerFloat = new Float(illChannels.get(signalIndex).getPowerBc1());
        Float gainFloat = new Float(detChannels.get(signalIndex).getDetectorGain());

        Integer power = Math.round(powerFloat);
        Integer gain = Math.round(gainFloat);
        
        if (this.power!=null) {
            if (!this.power.equals(power)) {
                logger.warn("Inconsistent power ("+this.power+"!="+power+") for "+sampleEntity.getName());
                if ("Brain".equalsIgnoreCase(area)) {
                    this.power = power;
                }
            }
        }
        else {
            this.power = power;
        }

        if (this.gain!=null) {
            if (!this.gain.equals(gain)) {
                logger.warn("Inconsistent gain ("+this.gain+"!="+gain+") for "+sampleEntity.getName());
                if ("Brain".equalsIgnoreCase(area)) {
                    this.gain = gain;
                }
            }
        }
        else {
            this.gain = gain;
        }
    }

    private Double getWavelengthForDye(String dyeName) {
		if ("DY-547".equals(dyeName)) {
			return new Double(547);
		}
		else if ("Cy3".equals(dyeName)) {
			return new Double(564);
		}
		else {
			logger.warn("Unrecognized dye: "+dyeName+". Using 800 for wavelength.");
			return new Double(800);
		}
    }
    @Override
    protected String getGridServicePrefixName() {
        return "fiji";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        writeInstanceFiles();
        setJobIncrementStop(1);

        StringBuffer script = new StringBuffer();
        script.append("cd "+resultFileNode.getDirectoryPath()).append("\n");
        
        // Start Xvfb
        script.append("DISPLAY_PORT="+Vaa3DHelper.getRandomPort(START_DISPLAY_PORT)).append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT", "1280x1024x24")).append("\n");

        // Create temp dir
        script.append("export TMPDIR=").append(SCRATCH_DIR).append("\n");
        script.append("mkdir -p $TMPDIR\n");
        script.append("TEMP_DIR=`mktemp -d`\n");
        script.append("function cleanTemp {\n");
        script.append("    rm -rf $TEMP_DIR\n");
        script.append("    echo \"Cleaned up $TEMP_DIR\"\n");
        script.append("}\n");

        // Two EXIT handlers
        script.append("function exitHandler() { cleanXvfb; cleanTemp; }\n");
        script.append("trap exitHandler EXIT\n");
        
        // Deal with compressed LSMs by decompressing them to a temp directory
        
        if (brainFilepath!=null) {
	        if (brainFilepath.endsWith(".bz2")) {
	        	File brainFile = new File(brainFilepath);
	        	script.append("BRAIN_FILE=$TEMP_DIR/"+ArchiveUtils.getDecompressedFilepath(brainFile.getName())).append("\n");
	            script.append("echo \"Decompressing Brain LSM\"\n");
	        	script.append("bzcat "+brainFilepath+" > $BRAIN_FILE\n");
	        }
	        else {
	        	script.append("BRAIN_FILE="+brainFilepath).append("\n");
	        }
        }

        if (vncFilepath!=null) {
	        if (vncFilepath.endsWith(".bz2")) {
	        	File vncFile = new File(vncFilepath);
	        	script.append("VNC_FILE=$TEMP_DIR/"+ArchiveUtils.getDecompressedFilepath(vncFile.getName())).append("\n");
	        	script.append("echo \"Decompressing VNC LSM\"\n");
	        	script.append("bzcat "+vncFilepath+" > $VNC_FILE\n");
	        }
	        else {
	        	script.append("VNC_FILE="+vncFilepath).append("\n");
	        }
        }
        
        // Format parameter string for the Fiji script
        
        StringBuilder paramSb = new StringBuilder();
        paramSb.append(outputFilePrefix);
        paramSb.append(",");
        paramSb.append("$BRAIN_FILE");
        paramSb.append(",");
        paramSb.append("$VNC_FILE");
        paramSb.append(",");
        paramSb.append(power);
        paramSb.append(",");
        paramSb.append(gain);
        paramSb.append(",");
        paramSb.append(chanSpec);
        
        script.append(FIJI_BIN_PATH+" -macro "+FIJI_MACRO_PATH+"/"+macroName+".ijm "+paramSb).append(" &\n");
        script.append("fpid=$!\n");
        
        script.append(Vaa3DHelper.getXvfbScreenshotLoop("./xvfb.${PORT}", "PORT", "fpid", 30, 3600));
        
        script.append("for fin in *.avi; do\n");
        script.append("    fout=${fin%.avi}.mp4\n");
        script.append("    "+Vaa3DHelper.getFormattedH264ConvertCommand("$fin", "$fout", false)).append(" && rm $fin\n");
        script.append("done\n");
        
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix()).append("\n");
        
        writer.write(script.toString());
    }

    private void writeInstanceFiles() throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+1);
        if (!configFile.createNewFile()) { 
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath()); 
    	}
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 20;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

        File outputDir = new File(resultFileNode.getDirectoryPath());
        
        File[] files = outputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(outputFilePrefix);
            }
        });
        
        if (files.length==0) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "No output files found in directory "+outputDir);
        }
        
        FileDiscoveryHelper helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        helper.addFileExclusion("*.log");
        helper.addFileExclusion("*.oos");
        helper.addFileExclusion("sge_*");
        helper.addFileExclusion("temp");
        helper.addFileExclusion("tmp.*");
        helper.addFileExclusion("core.*");
        helper.addFileExclusion("xvfb");
        
        try {
            helper.addFilesInDirToFolder(pipelineRun, outputDir);    
            
            String defaultImageName = outputFilePrefix+"-Brain_MIP.png";
            Entity default2dImage = EntityUtils.findChildWithName(pipelineRun, defaultImageName);
            if (default2dImage!=null) {
                entityHelper.setDefault2dImage(pipelineRun, default2dImage);    
            }
            else {
                logger.warn("Could not find default image: "+defaultImageName);
            }
        }
        catch (Exception e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Error discoverying files in "+outputDir,e);
        }
	}
}
