package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.entity.GetIncompleteSeparationsService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Run the mask/chan pipeline on a set of neuron separation result directories.
 * 
 * Inputs variables:
 *   ENTITY_LIST - separations to repair 
 *   RUN_MODE - the mode to run, defined in GetIncompleteSeparationsService
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RepairArtifactsPipelineGridService extends AbstractEntityGridService {

    private static final int TIMEOUT_SECONDS = 1800;  // 30 minutes

    private String runMode;
    private List<String> separationIds;
    
    @Override
    protected String getGridServicePrefixName() {
        return "repair";
    }

    @Override
    protected void init() throws Exception {
    	
    	runMode = (String)processData.getItem("RUN_MODE");
        if (runMode==null) {
        	throw new ServiceException("Input parameter RUN_MODE may not be null");
        }
    	
        Object entityListObj = processData.getItem("ENTITY_LIST");
        
        if (entityListObj instanceof String) {
            separationIds = Task.listOfStringsFromCsvString(entityListObj.toString());
        }
        else if (entityListObj instanceof List) {
            separationIds = (List<String>)entityListObj;    
        }
        else {
            throw new IllegalArgumentException("Expected String or List in ENTITY_LIST, but got "+entityListObj.getClass().getName());
        }
        
        if (separationIds==null) {
        	throw new ServiceException("Input parameter ENTITY_LIST may not be empty");
        }
        
        logger.info("Starting RepairArtifactsPipelineGridService with taskId=" + task.getObjectId() + " runMode=" + runMode + " resultDir=" + resultFileNode.getDirectoryPath());
    }
    
    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
    	int configIndex = 1;
    	for(String separationId : separationIds) {
    		Entity separation = entityBean.getEntityTree(Long.parseLong(separationId));
    		
    		// Check to make sure this separation exists. Otherwise vaa3d may hang indefinitely.
    		String sepPath = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    		File file = new File(sepPath);
    		if (file.exists()) {
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
            	Entity supportingData = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_SUPPORTING_DATA);

            	if (supportingData==null) {
            		throw new IllegalStateException("No supporting data found for neuron separation: "+separation.getId());
            	}
            	
            	// Load reference stack
            	Entity reference = null;
        		logger.debug("Checking "+supportingData.getName()+" "+supportingData.getId());
	        	for(Entity image3d : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_3D)) {
	        		logger.debug("Checking "+image3d.getName());
	        		if (image3d.getName().startsWith("Reference.")) {
	        			reference = image3d;
	        			break;
	        		};
	        	}
            	
            	if (reference==null) {
            		throw new IllegalStateException("No reference stack found for neuron separation: "+separation.getId());
            	}
            	
            	String refPath = reference.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                fw.write(refPath + "\n");
                fw.write(sepPath + "/archive/maskChan\n");
        	}
        	else if (runMode.equals(GetIncompleteSeparationsService.MODE_ALL_MASK_CHAN)) {
        		fw.write(sepPath + "\n");
        	}
        	else if (runMode.equals(GetIncompleteSeparationsService.MODE_FASTLOAD)) {
        		fw.write(sepPath + "\n");
        		fw.write(getSeparationInputFile(separation) + "\n");
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
        	script.append("read ORIG_FILE\n");
        	script.append(NeuronSeparatorHelper.getFastLoadCommands(getGridResourceSpec().getSlots(), "$INPUT") + " $ORIG_FILE\n");        	
        }
        
        writer.write(script.toString());
    }
    
    private String getSeparationInputFile(Entity separation) throws Exception {

        Set<Long> parentIds = entityBean.getParentIdsForAttribute(separation.getId(), EntityConstants.ATTRIBUTE_RESULT);
        if (parentIds.isEmpty() || parentIds.size()>1) {
            logger.warn("Unexpected number of result parents: "+parentIds.size());
            return "";
        }
        else {
            Entity resultEntity = entityBean.getEntityById(parentIds.iterator().next());
            entityLoader.populateChildren(resultEntity);
            Entity default3dImage = resultEntity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            return default3dImage.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        }
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredSlots() {
        return 16;
    }
    
    @Override
	public void postProcess() throws MissingDataException {
    	
	}
}