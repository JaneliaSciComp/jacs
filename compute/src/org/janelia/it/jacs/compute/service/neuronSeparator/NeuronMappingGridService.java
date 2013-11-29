package org.janelia.it.jacs.compute.service.neuronSeparator;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityGridService;
import org.janelia.it.jacs.compute.service.vaa3d.Vaa3DHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Run neuron mapping between two inputs. Parameters:
 * 
 *   INPUT_FILENAME_1 - source input file to map from
 *   INPUT_FILENAME_2 - target input file to map onto
 *   
 *   OR
 *   
 *   SEPARATION_ID_1 - source separation to map from
 *   SEPARATION_ID_2 - target separation to map onto
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronMappingGridService extends AbstractEntityGridService {
	
    private static final String CONFIG_PREFIX = "neuMapConfiguration.";
    private static final int TIMEOUT_SECONDS = 60 * 60;

    private String inputFilename1;
    private String inputFilename2;
    
    @Override
    protected String getGridServicePrefixName() {
        return "neuMap";
    }

    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);

        inputFilename1 = data.getItemAsString("INPUT_FILENAME_1");
        inputFilename2 = data.getItemAsString("INPUT_FILENAME_2");
        
        if (inputFilename1==null || inputFilename2==null) {
            Long separationId1 = data.getRequiredItemAsLong("SEPARATION_ID_1");
            Long separationId2 = data.getRequiredItemAsLong("SEPARATION_ID_2");
            Entity separation1 = entityBean.getEntityById(separationId1);
            Entity separation2 = entityBean.getEntityById(separationId2);
            inputFilename1 = getLabelFile(separation1);
            inputFilename2 = getLabelFile(separation2);
        }
    	
        logger.info("Starting NeuronMappingGridService with taskId=" + task.getObjectId() + " resultNodeId=" + resultFileNode.getObjectId() + " resultDir=" + resultFileNode.getDirectoryPath()+
                " inputFilename1="+inputFilename1+ " inputFilename2="+inputFilename2);
    }
    
    private String getLabelFile(Entity separation) throws Exception {

        populateChildren(separation);
        
        Entity supportingFiles = EntityUtils.getSupportingData(separation);
        populateChildren(supportingFiles);
        
        String labelV3d = null;
        String labelOther = null;
        String mappedNsp = null;
        String unmappedNsp = null;
        
        for(Entity child : supportingFiles.getChildren()) {
            String filepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            if (child.getName().startsWith("ConsolidatedLabel.v3d")) { // either v3dpbd or v3draw
                labelV3d = filepath;
            }
            else if (child.getName().startsWith("ConsolidatedLabel")) { // maybe a TIFF? This shouldn't happen in general.
                labelOther = filepath;
            }
            else if (child.getName().startsWith("SeparationResult.nsp")) {
                mappedNsp = filepath;
            }
            else if (child.getName().startsWith("SeparationResultUnmapped.nsp")) {
                unmappedNsp = filepath;
            }
        }
        
        // Preference order
        if (labelV3d!=null) return labelV3d;
        if (mappedNsp!=null) return mappedNsp;
        if (unmappedNsp!=null) return unmappedNsp;
        if (labelOther!=null) return labelOther;
        return null;
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        writeInstanceFiles(1);
        createShellScript(writer);
        setJobIncrementStop(1);
    }

    private void writeInstanceFiles(int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            fw.write(resultFileNode.getDirectoryPath() + "\n");
            fw.write(inputFilename1 + "\n");
            fw.write(inputFilename2 + "\n");
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
        script.append("read INPUT_FILE_1\n");
        script.append("read INPUT_FILE_2\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandPrefix() + "\n");
        script.append(Vaa3DHelper.getVaa3dLibrarySetupCmd()+"\n");
        script.append(NeuronSeparatorHelper.getNeuronMappingCommands("$OUTPUT_DIR", "$INPUT_FILE_1", "$INPUT_FILE_2") + "\n");
        script.append(Vaa3DHelper.getVaa3DGridCommandSuffix() + "\n");
        script.append("\n");
        writer.write(script.toString());
    }
    
	@Override
    public int getJobTimeoutSeconds() {
        return TIMEOUT_SECONDS;
    }

    @Override
    protected int getRequiredMemoryInGB() {
        return 4;
    }
    
    @Override
	public void postProcess() throws MissingDataException {

    	File outputDir = new File(resultFileNode.getDirectoryPath());
    	File[] coreFiles = outputDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("core");
			}
		});
    	
    	if (coreFiles.length > 0) {
    		throw new MissingDataException("Neuron mapping core dumped for "+resultFileNode.getDirectoryPath());
    	}

    	File[] resultFiles = outputDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.startsWith("mapping_issues.txt");
			}
		});

    	if (resultFiles.length < 1) {
    		throw new MissingDataException("mapping_issues.txt not found in "+resultFileNode.getDirectoryPath());
    	}
	}
}