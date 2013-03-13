package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Takes a Sample Entity and finds all the LSM stacks which are part of it, then creates lsmFileNames.txt with all the
 * LSM filenames, and a metadata file for each LSM. The parameters should be included in the ProcessData:
 *   SAMPLE_ENTITY_ID 
 *   RESULT_FILE_NODE
 *   OUTPUT_FILE_NODE
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateLsmMetadataFilesService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "metadataConfiguration.";
    
    protected File outputDir;
    protected Logger logger;
    
    private List<File> inputFiles = new ArrayList<File>();
    private Entity sampleEntity;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());

    	FileNode outputNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
    	if (outputNode == null) {
    		throw new IllegalArgumentException("OUTPUT_FILE_NODE may not be null");
    	}
    	outputDir = new File(outputNode.getDirectoryPath());
    	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	sampleEntity = EJBFactory.getLocalEntityBean().getEntityTree(new Long(sampleEntityId));
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}
    	
    	for(Entity lsmStack : EntityUtils.getDescendantsOfType(sampleEntity, EntityConstants.TYPE_LSM_STACK, true)) {
			String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
			inputFiles.add(new File(filepath));
    	}
    	
    	File sampleIdFile = new File(outputNode.getDirectoryPath(), "sampleEntityId.txt");
    	try {
    		FileUtils.writeStringToFile(sampleIdFile, sampleEntityId);
    	}
    	catch (Exception e) {
    		logger.error("Error writing "+sampleIdFile.getAbsolutePath(),e);
    	}
    }

    @Override
    protected String getGridServicePrefixName() {
        return "metadata";
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {

    	int configIndex = 1;
    	for(File inputFile : inputFiles) {
    		writeInstanceFiles(inputFile, configIndex++);
    	}
    	createShellScript(writer);
        setJobIncrementStop(configIndex-1);
    }

    private void writeInstanceFiles(File inputFile, int configIndex) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory(), CONFIG_PREFIX+configIndex);
        FileWriter fw = new FileWriter(configFile);
        try {
            String metadataStub = createLsmMetadataFilename(inputFile);
            File lsmDataFile = new File(outputDir, metadataStub+".metadata");
            File jsonDataFile = new File(outputDir, metadataStub+".json");
            fw.write(inputFile.getAbsolutePath() + "\n");
            fw.write(lsmDataFile.getAbsolutePath() + "\n");
            fw.write(jsonDataFile.getAbsolutePath() + "\n");
        }
        catch (IOException e) {
        	throw new ServiceException("Unable to create SGE Configuration file "+configFile.getAbsolutePath(),e); 
        }
        finally {
            fw.close();
        }
    }

    private void createShellScript(FileWriter writer) throws Exception {
    	File lsmFilePathsFile = new File(outputDir,"lsmFilePaths.txt");
    	
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILENAME\n");
        script.append("read METADATA_FILENAME\n");
        script.append("read JSON_FILENAME\n");
        script.append("cd "+outputDir.getAbsolutePath()).append("\n");
        script.append("echo " + addQuotes("$INPUT_FILENAME") + " >> " + lsmFilePathsFile.getAbsolutePath()).append("\n");
        script.append(getScriptToCreateLsmMetadataFile("$INPUT_FILENAME", "$METADATA_FILENAME")).append("\n");
        script.append(getScriptToCreateLsmJsonFile("$INPUT_FILENAME", "$JSON_FILENAME")).append("\n");
        
        writer.write(script.toString());
    }
    
    private String getScriptToCreateLsmMetadataFile(String inputFile, String outputFile) throws ServiceException {
        return "perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + 
                SystemConfigurationProperties.getString("LSMMetadataDump.CMD")+ " " +
                addQuotes(inputFile) + " " + addQuotes(outputFile);
    }

    private String getScriptToCreateLsmJsonFile(String inputFile, String outputFile) throws ServiceException {
        return "perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + 
                SystemConfigurationProperties.getString("LSMJSONDump.CMD")+ " " +
                addQuotes(inputFile) + " " + addQuotes(outputFile);
    }
    
    private String addQuotes(String s) {
    	return "\""+s+"\"";
    }
    
    private String createLsmMetadataFilename(File lsmFile) {
        return lsmFile.getName().replaceAll("\\s+","_");
    }

    @Override
    protected int getRequiredMemoryInGB() {
    	return 3;
    }
    
    @Override
    public void postProcess() throws MissingDataException {

    }
}
