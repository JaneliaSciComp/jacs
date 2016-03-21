package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.domain.model.AnatomicalArea;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.service.vaa3d.MergedLsmPair;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.zeiss.LSMMetadata;

/**
 * Takes the bulk merge parameters and creates metadata files for each one. 
 * The parameters should be included in the ProcessData:
 *   SAMPLE_AREA 
 *   RESULT_FILE_NODE
 *   OUTPUT_FILE_NODE
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateLsmMetadataFilesService extends AbstractDomainGridService {

    private static final String CONFIG_PREFIX = "metadataConfiguration.";
    
    protected Long sampleId;
    protected File outputDir;
    protected Logger logger;
    private List<File> inputFiles = new ArrayList<File>();
    
    private File jsonDataFile;

    protected void init() throws Exception {

    	FileNode outputNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
    	if (outputNode == null) {
    		throw new IllegalArgumentException("OUTPUT_FILE_NODE may not be null");
    	}
    	outputDir = new File(outputNode.getDirectoryPath());
    	
        AnatomicalArea sampleArea = (AnatomicalArea) data.getRequiredItem("SAMPLE_AREA");
        List<MergedLsmPair> mergedLsmPairs = sampleArea.getMergedLsmPairs();

        this.sampleId = sampleArea.getSampleId();
        
        for(MergedLsmPair mergedLsmPair : mergedLsmPairs) {
            inputFiles.add(new File(mergedLsmPair.getLsmFilepath1()));
            if (mergedLsmPair.getLsmFilepath2() != null) {
                inputFiles.add(new File(mergedLsmPair.getLsmFilepath2()));    
            }
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
            this.jsonDataFile = new File(outputDir, metadataStub+".json");
            fw.write(inputFile.getAbsolutePath() + "\n");
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
        StringBuffer script = new StringBuffer();
        script.append("read INPUT_FILENAME\n");
        script.append("read JSON_FILENAME\n");
        script.append("cd "+outputDir.getAbsolutePath()).append("\n");
        script.append("echo \"Generating metadata for LSM files in sample "+sampleId+"\" \n");
        script.append(getScriptToCreateLsmJsonFile("$INPUT_FILENAME", "$JSON_FILENAME")).append("\n");
        
        writer.write(script.toString());
    }
    
    private String getScriptToCreateLsmJsonFile(String inputFile, String outputFile) throws ServiceException {
        return "perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + 
                SystemConfigurationProperties.getString("LSMJSONDump.CMD")+ " " +
                addQuotes(inputFile) + " > " + addQuotes(outputFile);
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

        if (!jsonDataFile.exists()) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "JSON metadata file does not exist: "+jsonDataFile);
        }

        if (jsonDataFile.length()<=0) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "JSON metadata file is empty: "+jsonDataFile);
        }
        
        try {
            LSMMetadata metadata = LSMMetadata.fromFile(jsonDataFile);
            if(metadata.getChannels().isEmpty()) {
                throw new MissingGridResultException(outputDir.getAbsolutePath(), "No channels in JSON metadata file: "+jsonDataFile);
            }
        }
        catch (MissingDataException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MissingGridResultException(outputDir.getAbsolutePath(), "JSON metadata file cannot be parsed: "+jsonDataFile,e);
        }
        
    }
}
