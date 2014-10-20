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
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
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
        
        this.outputFilePrefix = sampleEntity.getName();
        
        logger.info("Running Fiji macro "+macroName+" for sample "+sampleEntity.getName()+" (id="+sampleEntityId+")");

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
        if (vncLsm!=null) {
            String vncChanSpec = brainLsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
            if (!chanSpec.equals(vncChanSpec)) {
                throw new IllegalStateException("Brain chanspec ("+chanSpec+") does not match VNC chanspec ("+vncChanSpec+")");
            }
        }
    }
    
    private void registerLsmAttributes(final Entity sampleEntity, final Entity lsm) throws Exception {
        // The actual filename of the LSM we're dealing with is not compressed
		final String lsmName = ArchiveUtils.getDecompressedFilepath(lsm.getName());
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
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
                Double o1w = Double.parseDouble(o1.getDyeName().substring(DETECTION_CHANNEL_DYE_PREFIX.length()));
                Double o2w = Double.parseDouble(o2.getDyeName().substring(DETECTION_CHANNEL_DYE_PREFIX.length()));
                return o1w.compareTo(o2w);
            }
        });

        int signalIndex = chanspec.indexOf('s');
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

    @Override
    protected String getGridServicePrefixName() {
        return "fiji";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        writeInstanceFiles();
        setJobIncrementStop(1);

        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix()).append("\n");
        script.append("cd "+resultFileNode.getDirectoryPath()).append("\n");
        
        // Deal with compressed LSMs by decompressing them to the file node temporarily
        
        if (brainFilepath!=null) {
	        if (brainFilepath.endsWith(".bz2")) {
	        	File brainFile = new File(brainFilepath);
	        	File tmpFile = new File(resultFileNode.getDirectoryPath(), ArchiveUtils.getDecompressedFilepath(brainFile.getName()));
	        	script.append("BRAIN_FILE="+tmpFile.getAbsolutePath()).append("\n");
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
	        	File tmpFile = new File(resultFileNode.getDirectoryPath(), ArchiveUtils.getDecompressedFilepath(vncFile.getName()));
	        	script.append("VNC_FILE="+tmpFile.getAbsolutePath()).append("\n");
	        	script.append("echo \"Decompressing VNC LSM\"\n");
	        	script.append("bzcat "+vncFilepath+" > $VNC_FILE\n");
	        }
	        else {
	        	script.append("VNC_FILE="+vncFilepath).append("\n");
	        }
        }
        
        // Trap to clean up any LSMs that we may have decompressed here
        
        script.append("function cleanLsms {\n");
        script.append("    rm -f "+resultFileNode.getDirectoryPath()+"/*.lsm\n");
        script.append("    echo \"Cleaned up temporary files\"\n");
        script.append("}\n");
        script.append("trap cleanLsms EXIT\n");
        
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
        
        script.append(FIJI_BIN_PATH+" -macro "+FIJI_MACRO_PATH+"/"+macroName+".ijm "+paramSb).append("\n");
        
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
            throw new MissingDataException("No output files found in directory "+outputDir);
        }
        
        FileDiscoveryHelper helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        helper.addFileExclusion("*.log");
        helper.addFileExclusion("*.oos");
        helper.addFileExclusion("sge_*");
        helper.addFileExclusion("temp");
        helper.addFileExclusion("tmp.*");
        helper.addFileExclusion("core.*");
        
        try {
            helper.addFilesInDirToFolder(pipelineRun, outputDir);    
            
            String defaultImageName = outputFilePrefix+"_Brain_MIP.png";
            Entity default2dImage = EntityUtils.findChildWithName(pipelineRun, defaultImageName);
            if (default2dImage!=null) {
                entityHelper.setDefault2dImage(pipelineRun, default2dImage);    
            }
            else {
                logger.warn("Could not find default image: "+defaultImageName);
            }
        }
        catch (Exception e) {
            throw new MissingDataException("Error discoverying files in "+outputDir,e);
        }
	}
}
