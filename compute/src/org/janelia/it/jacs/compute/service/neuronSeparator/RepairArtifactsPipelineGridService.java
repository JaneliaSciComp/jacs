package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.entity.GetIncompleteSeparationsService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Run the mask/chan pipeline on a set of neuron separation result directories.
 * 
 * Inputs variables:
 *   FILE_PATHS - input paths (e.g. /.../separate)
 *   RUN_MODE - the mode to run, defined in GetIncompleteSeparationsService
 *   
 * Output variables:
 *   OUTPUT_PATHS - resulting artifact paths (probably located in some temp directory that will go away)
 *   FINAL_PATHS - paths as they should look when everything is said and done (may be located in the archive)
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RepairArtifactsPipelineGridService extends AbstractEntityGridService {

    public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
    
    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private String runMode;
    private List<Entity> separations;
    
    @Override
    protected String getGridServicePrefixName() {
        return "repair";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
    	runMode = (String)processData.getItem("RUN_MODE");
        if (runMode==null) {
        	throw new ServiceException("Input parameter RUN_MODE may not be null");
        }

        if (!runMode.equals(GetIncompleteSeparationsService.MODE_REF_MASK_CHAN)) {
        	logger.error("Run mode "+runMode+" is currently not supported.");
        	return;
        }
    	
        separations = (List<Entity>)processData.getItem("ENTITY_LIST");
        if (separations==null) {
        	throw new ServiceException("Input parameter ENTITY_LIST may not be empty");
        }
        
        logger.info("Starting MaskChanArtifactPipelineGridService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath());
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
    	int configIndex = 1;
    	for(Entity separation : separations) {
        	String inputPath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (!inputPath.contains(centralDir)) {
                logger.info("Cannot repair artifacts in dir which is not in the FileStore.CentralDir: "+inputPath);
            }
            else {
            	writeInstanceFiles(separation, configIndex++);	
            }
    	}
    	writeShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(Entity separation, int configIndex) throws Exception {
    	File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName()+"Configuration."+configIndex);
        FileWriter fw = new FileWriter(configFile);
        
        try {
        	String sepPath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        	
        	if (runMode.equals(GetIncompleteSeparationsService.MODE_REF_MASK_CHAN)) {

            	// Load children
            	populateChildren(separation);
            	Entity supportingData = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_SUPPORTING_DATA);
            	populateChildren(supportingData);

            	// Load reference stack
            	Entity reference = null;
            	if (supportingData!=null) {
    	        	for(Entity image3d : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_3D)) {
    	        		if (image3d.getName().startsWith("Reference.")) {
    	        			reference = image3d;
    	        		};
    	        	}
    	        	populateChildren(reference);
            	}
            	
            	String refPath = reference.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                fw.write(refPath + "\n");
                fw.write(sepPath + "/archive/maskChan\n");
        	}
        	else {
        		fw.write(sepPath + "\n");
        	}
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    protected void writeShellScript(FileWriter writer) throws Exception {
        StringBuffer script = new StringBuffer();
        script.append("read INPUT\n");
        
        if (runMode.equals(GetIncompleteSeparationsService.MODE_REF_MASK_CHAN)) {
            script.append("read OUTPUT_DIR\n");
            script.append("mkdir -p $OUTPUT_DIR\n");
        	script.append(Vaa3DHelper.getFormattedMaskFromStackCommand("$INPUT","$OUTPUT_DIR","ref","1","0.08"));
        }
        else if (runMode.equals(GetIncompleteSeparationsService.MODE_ALL_MASK_CHAN)) {
        	script.append(NeuronSeparatorHelper.getMaskChanCommands(getGridResourceSpec().getSlots(), "$INPUT") + "\n");
        }
        else if (runMode.equals(GetIncompleteSeparationsService.MODE_FASTLOAD)) {
        	script.append(NeuronSeparatorHelper.getFastLoadCommands(getGridResourceSpec().getSlots(), "$INPUT") + "\n");        	
        }
        
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 8;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
    	
	}
}