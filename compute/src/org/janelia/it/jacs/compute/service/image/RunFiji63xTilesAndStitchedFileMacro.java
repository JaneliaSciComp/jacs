package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.ArchiveUtils;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Runs a Fiji macro that requires sets of 63x LSMs or a stitched file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunFiji63xTilesAndStitchedFileMacro extends AbstractEntityGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String FIJI_BIN_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Bin.Path");
    protected static final String FIJI_MACRO_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Macro.Path");
    protected static final String SCRATCH_DIR = SystemConfigurationProperties.getString("computeserver.ClusterScratchDir");
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "fijiConfiguration.";

    private int randomPort; 
    private int configIndex = 1;
    
    private String macroName;
    private String mode;
    private String outputFilePrefix;
    private Entity sampleEntity;
    private Entity pipelineRun;
    private String mergedChanSpec = null;
    private String outputColorSpec = null;
    private Map<String,Entity> lsmEntityMap = new HashMap<String,Entity>();
    private List<MergedLsmPair> mergedLsmPairs;
    private Entity stitchedFile;
    private String effector;

    @Override
    protected void init() throws Exception {
    	
    	this.macroName = data.getRequiredItemAsString("MACRO_NAME");
        this.mode = data.getRequiredItemAsString("MACRO_MODE");
        
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

        this.outputColorSpec = data.getItemAsString("OUTPUT_COLOR_SPEC");

        String outputChannelOrder = data.getItemAsString("OUTPUT_CHANNEL_ORDER");
		StringBuilder csSb = new StringBuilder();
		for(String channel : outputChannelOrder.split(",")) {
			csSb.append(channel.equals("reference")?"r":"s");
		}
		this.mergedChanSpec = csSb.length()>0?csSb.toString():null;
		
        Object bulkMergeParamObj = processData.getItem("BULK_MERGE_PARAMETERS");
        if (bulkMergeParamObj==null) {
        	throw new ServiceException("Input parameter BULK_MERGE_PARAMETERS may not be null");
        }

        if (!(bulkMergeParamObj instanceof List)) {
        	throw new IllegalArgumentException("Input parameter BULK_MERGE_PARAMETERS must be a List");
        }
        
    	this.mergedLsmPairs = (List<MergedLsmPair>)bulkMergeParamObj;
        
        final boolean gatherLsms = mergedLsmPairs==null;
        
        if (gatherLsms) {
        	this.mergedLsmPairs = new ArrayList<MergedLsmPair>();
        }
        
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .run(new EntityVisitor() {
            public void visit(Entity tile) throws Exception {
            	populateChildren(tile);
            	
            	Entity lsm1 = null;
            	Entity lsm2 = null;
            	for(Entity lsm : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
            		String lsmName = lsm.getName();
                    // The actual filename of the LSM we're dealing with is not compressed
            		String lsmPath = ArchiveUtils.getDecompressedFilepath(lsmName);
            		lsmEntityMap.put(lsmPath, lsm);
            		if (lsm1==null) {
            			lsm1 = lsm;
            		}
            		else if (lsm2==null) {
            			lsm2 = lsm;
            		}
            		else {
            			logger.warn("Too many LSMs for tile "+tile.getId()+" in sample "+sampleEntity.getId());
            		}
            	}

            	if (gatherLsms) {
                	String lsmName1 = lsm1.getName();
                	String lsmName2 = lsm2.getName();
                	String lsmPath1 = ArchiveUtils.getDecompressedFilepath(lsmName1);
                	String lsmPath2 = ArchiveUtils.getDecompressedFilepath(lsmName2);
	            	String tileName = tile.getName().replaceAll(" ", "_");
		        	mergedLsmPairs.add(new MergedLsmPair(lsm1.getId(), lsm2.getId(), lsmName1, lsmName2, lsmPath1, lsmPath2, null, tileName));
            	}
            }
        });
        
        // Find the latest stitched file for this sample 
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
                .childrenOfAttr(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE).last()
                .run(new EntityVisitor() {
            public void visit(Entity file) throws Exception {
            	stitchedFile = file;
            }
        });
        
        if (mergedLsmPairs.isEmpty()) {
        	throw new Exception("No LSM pairs found for sample "+sampleEntity.getName());
        }
        
        if (stitchedFile==null) {
        	throw new Exception("No stitched file found for sample "+sampleEntity.getName());
        }
        
        entityBean.setOrUpdateValue(pipelineRun.getId(), EntityConstants.ATTRIBUTE_FILE_PATH, resultFileNode.getDirectoryPath());
        
        logger.info("Running Fiji macro "+macroName+" for sample "+sampleEntity.getName()+
        		" (id="+sampleEntityId+") with "+mergedLsmPairs.size()+" tiles");
    }
    
    @Override
    protected String getGridServicePrefixName() {
        return "fiji";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        this.randomPort = Vaa3DHelper.getRandomPort(START_DISPLAY_PORT);
        writeInstanceFiles();
        setJobIncrementStop(configIndex-1);
    	createShellScript(writer);
    }

    private void writeInstanceFiles() throws Exception {

    	String sampleName = sampleEntity.getName();
    	if (sampleName.contains("~")) {
    		sampleName = sampleName.substring(0, sampleName.indexOf('~'));
    	}
    	
    	for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {

            File lsm1 = new File(mergedLsmPair.getLsmFilepath1());
            Entity lsm1Entity = lsmEntityMap.get(lsm1.getName());
            
            String inputFile = mergedLsmPair.getMergedFilepath();
    		String chanSpec = mergedChanSpec;
    		String effector = lsm1Entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR);
            this.outputFilePrefix = sampleName+"-"+mergedLsmPair.getTag().replaceAll("\\s+","_")+"-"+effector;
            String colorSpec = outputColorSpec;

			prefixToChanspec.put(outputFilePrefix, mergedChanSpec);
			prefixToPixelRes.put(outputFilePrefix, lsm1Entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
			writeInstanceFile(outputFilePrefix, mode, inputFile, chanSpec, colorSpec, configIndex++);
    	}

    	// Stitched file is only relevant if there is more than one tile
		if (configIndex>2) {
	        this.outputFilePrefix = sampleName+"-stitched"+(effector==null?"":"-"+effector);
			String inputFile = stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			if (mergedChanSpec==null) {
				mergedChanSpec = stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
			}
			prefixToChanspec.put(outputFilePrefix, mergedChanSpec);
			prefixToPixelRes.put(outputFilePrefix, stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION));
	    	writeInstanceFile(outputFilePrefix, mode, inputFile, mergedChanSpec, outputColorSpec, configIndex++);
		}
    }
    
    private Map<String,String> prefixToChanspec = new HashMap<String,String>();
    private Map<String,String> prefixToPixelRes = new HashMap<String,String>();

    private void writeInstanceFile(String outputPrefix, String mode, String inputFile, String chanSpec, String colorSpec, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(resultFileNode.getDirectoryPath() + "\n");
            fw.write(outputPrefix + "\n");
            fw.write((mode==null?"none":mode) + "\n");
            fw.write((inputFile==null?"":inputFile) + "\n");
            fw.write((chanSpec==null?"":chanSpec) + "\n");
            fw.write((colorSpec==null?"":colorSpec) + "\n");
            fw.write((randomPort+configIndex) + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }
    
    private void createShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read OUTPUT_DIR\n");
        script.append("read OUTPUT_PREFIX\n");
        script.append("read MODE\n");
        script.append("read INPUT_FILE\n");
        script.append("read CHAN_SPEC\n");
        script.append("read COLOR_SPEC\n");
        script.append("read DISPLAY_PORT\n");
        script.append("cd ").append(resultFileNode.getDirectoryPath()).append("\n");
        
        // Start Xvfb
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
        
        // Prepare
        script.append(Vaa3DHelper.getEnsureRawFunction()).append("\n");
        script.append(Vaa3DHelper.getEnsureRawCommand("$TEMP_DIR", "$INPUT_FILE_1", "RAW_1")).append("\n");
        script.append(Vaa3DHelper.getEnsureRawCommand("$TEMP_DIR", "$INPUT_FILE_2", "RAW_2")).append("\n");
        script.append("sleep 5\n"); // wait for files to appear
        script.append("if [ ! -f $RAW_1 ]; then\n");
        script.append("  echo \"Input file does not exist: $RAW_1\"\n");
        script.append("  exit 1\n");
        script.append("fi\n");

        // Run Fiji macro in the background
        script.append(FIJI_BIN_PATH+" -macro "+FIJI_MACRO_PATH+"/"+macroName+".ijm $OUTPUT_DIR,$OUTPUT_PREFIX,$MODE,$RAW,$CHAN_SPEC,$COLOR_SPEC,mips:movies").append(" &\n");
        script.append("fpid=$!\n");
        
        // Spy on Xvfb
        script.append(Vaa3DHelper.getXvfbScreenshotLoop("./xvfb.${PORT}", "PORT", "fpid", 30, 7200));
        
		// Convert movies to H.246
        script.append("fin=$OUTPUT_PREFIX.avi\n");
        script.append("fout=$OUTPUT_PREFIX.mp4\n");
        script.append(Vaa3DHelper.getFormattedH264ConvertCommand("$fin", "$fout", false)).append(" && rm $fin\n");
        script.append("fin=${OUTPUT_PREFIX}-Signal.avi\n");
        script.append("fout=${OUTPUT_PREFIX}-Signal.mp4\n");
        script.append(Vaa3DHelper.getFormattedH264ConvertCommand("$fin", "$fout", false)).append(" && rm $fin\n");
        
        // Clean up 
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix()).append("\n");
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 88;
    }

	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }
	
    @Override
	public void postProcess() throws MissingDataException {

        File outputDir = new File(resultFileNode.getDirectoryPath());

    	File[] aviFiles = FileUtil.getFilesWithSuffixes(outputDir, ".avi");
    	if (aviFiles.length > 0) {
			throw new MissingGridResultException(outputDir.getAbsolutePath(), "MP4 generation failed for "+resultFileNode.getDirectoryPath());
    	}

    	File[] mp4Files = FileUtil.getFilesWithSuffixes(outputDir, ".mp4");
    	if (mp4Files.length < 1) {
			throw new MissingGridResultException(outputDir.getAbsolutePath(), "MP4 generation failed for "+resultFileNode.getDirectoryPath());
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
            
            String defaultImageName = outputFilePrefix+"_MIP.png";
            Entity default2dImage = EntityUtils.findChildWithName(pipelineRun, defaultImageName);
            if (default2dImage!=null) {
                entityHelper.setDefault2dImage(pipelineRun, default2dImage);    
            }
            else {
                logger.warn("Could not find default image: "+defaultImageName);
            }
            
            pipelineRun = entityBean.getEntityAndChildren(pipelineRun.getId());
            for(Entity artifactEntity : pipelineRun.getOrderedChildren()) {
            	if (EntityConstants.TYPE_MOVIE.equals(artifactEntity.getEntityTypeName())) {
            		for(String prefix : prefixToChanspec.keySet()) {
            			if (artifactEntity.getName().startsWith(prefix)) {
            				entityBean.setOrUpdateValue(artifactEntity.getId(), EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION, prefixToChanspec.get(prefix));
            				entityBean.setOrUpdateValue(artifactEntity.getId(), EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, prefixToPixelRes.get(prefix));
            				break;
            			}
            		}
            	}
            }
        }
        catch (Exception e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "Error discovering files in "+outputDir,e);
        }
	}
}
