
package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.compute.util.EntityBeanEntityLoader;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Runs a Fiji macro that requires sets of 63x LSMs or a stitched file. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunFiji63xBrainVNCMacro extends AbstractEntityGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String FIJI_BIN_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Bin.Path");
    protected static final String FIJI_MACRO_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Macro.Path");
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
	private static final int START_DISPLAY_PORT = 890;
    private static final String CONFIG_PREFIX = "fijiConfiguration.";

    private int randomPort; 
    private int configIndex = 1;
    
    private String macroName;
    private String outputFilePrefix;
    private Entity sampleEntity;
    private Entity pipelineRun;
    
    private Collection<LsmPair> lsmPairs = new ArrayList<LsmPair>();
    private Entity stitchedFile;

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

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
        
        logger.info("Running Fiji macro "+macroName+" for sample "+sampleEntity.getName()+" (id="+sampleEntityId+")");
        
        EntityVistationBuilder.create(new EntityBeanEntityLoader(entityBean)).startAt(sampleEntity)
                .childOfType(EntityConstants.TYPE_SUPPORTING_DATA)
                .childrenOfType(EntityConstants.TYPE_IMAGE_TILE)
                .run(new EntityVisitor() {
            public void visit(Entity tile) throws Exception {
            	populateChildren(tile);
            	LsmPair pair = new LsmPair();
            	pair.tileName = tile.getName().replaceAll(" ", "_");
            	for(Entity lsm : EntityUtils.getChildrenOfType(tile, EntityConstants.TYPE_LSM_STACK)) {
            		if (pair.lsm1==null) pair.lsm1 = lsm;
            		else if (pair.lsm2==null) pair.lsm2 = lsm;
            		else {
            			logger.warn("Too many LSMs for tile "+tile.getId()+" in sample "+sampleEntity.getId());
            		}
            	}
            	lsmPairs.add(pair);
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
        
        if (lsmPairs.isEmpty()) {
        	throw new Exception("No LSM pairs found for sample "+sampleEntity.getName());
        }
        
        if (stitchedFile==null) {
        	throw new Exception("No stitched file found for sample "+sampleEntity.getName());
        }
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

    	for(LsmPair pair : lsmPairs) {
    		String inputFile1 = pair.lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		String inputFile2 = pair.lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		String chanSpec1 = pair.lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    		String chanSpec2 = pair.lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    		String effector1 = pair.lsm1.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR);
    		String effector2 = pair.lsm2.getValueByAttributeName(EntityConstants.ATTRIBUTE_EFFECTOR);
    		if (!effector1.equals(effector2)) {
                logger.warn("Inconsistent effector ("+effector1+"!="+effector2+") for "+sampleEntity.getName());
    		}
    		writeInstanceFile(sampleEntity.getName()+"-"+pair.tileName+"-"+effector1, inputFile1, inputFile2, chanSpec1, chanSpec2, configIndex++);
    	}

        this.outputFilePrefix = sampleEntity.getName()+"-stitched";
		String inputFile = stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
		String chanSpec = stitchedFile.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHANNEL_SPECIFICATION);
    	writeInstanceFile(outputFilePrefix, inputFile, null, chanSpec, null, configIndex++);
    }

    private void writeInstanceFile(String outputPrefix, String inputFile1, String inputFile2, String chanSpec1, String chanSpec2, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
        	fw.write(outputPrefix + "\n");
        	fw.write(resultFileNode.getDirectoryPath() + "\n");
            fw.write((inputFile1==null?"":inputFile1) + "\n");
            fw.write((chanSpec1==null?"":chanSpec1) + "\n");
            fw.write((inputFile2==null?"":inputFile2) + "\n");
            fw.write((chanSpec2==null?"":chanSpec2) + "\n");
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
        script.append("read OUTPUT_PREFIX\n");
        script.append("read OUTPUT_DIR\n");
        script.append("read INPUT_FILE_1\n");
        script.append("read CHAN_SPEC_1\n");
        script.append("read INPUT_FILE_2\n");
        script.append("read CHAN_SPEC_2\n");
        script.append("read DISPLAY_PORT\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix("$DISPLAY_PORT")).append("\n");
        script.append("cd "+resultFileNode.getDirectoryPath());
        script.append("\n");
        script.append(FIJI_BIN_PATH+" -macro "+FIJI_MACRO_PATH+"/"+macroName+".ijm $OUTPUT_PREFIX,$OUTPUT_DIR,$INPUT_FILE_1,$CHAN_SPEC_1,$INPUT_FILE_2,$CHAN_SPEC_2");
        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
        writer.write(script.toString());
    }
    
    @Override
    protected int getRequiredMemoryInGB() {
    	return 24;
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
            
            String defaultImageName = outputFilePrefix+"_MIP.png";
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
    
    private class LsmPair {
    	String tileName;
    	Entity lsm1;
    	Entity lsm2;
    }
    
}
