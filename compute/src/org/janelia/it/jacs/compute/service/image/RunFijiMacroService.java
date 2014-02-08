package org.janelia.it.jacs.compute.service.image;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Run a Fiji macro and add the resulting files to the entity model.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class RunFijiMacroService extends AbstractEntityGridService {

    protected static final String EXECUTABLE_DIR = SystemConfigurationProperties.getString("Executables.ModuleBase");
    protected static final String FIJI_BIN_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Bin.Path");
    protected static final String FIJI_MACRO_PATH = EXECUTABLE_DIR + SystemConfigurationProperties.getString("Fiji.Macro.Path");
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes
    private static final String CONFIG_PREFIX = "fijiConfiguration.";
    
    private String macroName;
    private String outputFilePrefix;
    private Entity sampleEntity;
    private Entity pipelineRun;
    
    protected abstract String getMacroParameter(Entity sampleEntity, String outputFilePrefix) throws Exception; 
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

    	this.outputFilePrefix = "out";
    	
    	this.macroName = data.getRequiredItemAsString("MACRO_NAME");
        String sampleEntityId = data.getRequiredItemAsString("SAMPLE_ENTITY_ID");
        
        sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }
        
        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }
        
        logger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        String pipelineRunEntityId = data.getRequiredItemAsString("PIPELINE_RUN_ENTITY_ID");
        pipelineRun = entityBean.getEntityById(pipelineRunEntityId);
        if (pipelineRun == null) {
            throw new IllegalArgumentException("Pipeline run entity not found with id="+pipelineRunEntityId);
        }
        
    }

    @Override
    protected String getGridServicePrefixName() {
        return "fiji";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

        String macroFile = macroName+".ijm";
        
        writeInstanceFiles();
        setJobIncrementStop(1);
        
        StringBuffer script = new StringBuffer();
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix()).append("\n");
        
        script.append("cd "+resultFileNode.getDirectoryPath());
        script.append(FIJI_BIN_PATH+" -macro "+FIJI_MACRO_PATH+"/"+macroFile+" "+getMacroParameter(sampleEntity,outputFilePrefix));

        script.append("\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix());
        script.append("\n");
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

        FileDiscoveryHelper helper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        helper.addFileExclusion("*.log");
        helper.addFileExclusion("*.oos");
        helper.addFileExclusion("sge_*");
        helper.addFileExclusion("temp");
        helper.addFileExclusion("tmp.*");
        helper.addFileExclusion("core.*");
        
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

        try {
            helper.addFilesInDirToFolder(pipelineRun, outputDir);    
        }
        catch (Exception e) {
            throw new MissingDataException("Error discoverying files in "+outputDir,e);
        }
	}
}
