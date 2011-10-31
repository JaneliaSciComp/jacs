package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Extracts and outputs the two filepaths from an LsmPair entity. The parameter must be included in the ProcessData:
 *   SAMPLE_ENTITY_ID
 *   RESULT_FILE_NODE
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateLsmMetadataFilesService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "metadataConfiguration.";
    
    protected File outputDir;
    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;
    
    private List<File> inputFiles = new ArrayList<File>();
    private Entity sampleEntity;
    
    protected void init(IProcessData processData) throws Exception {
    	super.init(processData);
    	
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        annotationBean = EJBFactory.getLocalAnnotationBean();

    	FileNode outputNode = (FileNode)processData.getItem("OUTPUT_FILE_NODE");
    	if (outputNode == null) {
    		throw new IllegalArgumentException("OUTPUT_FILE_NODE may not be null");
    	}
    	
    	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
    	if (sampleEntityId == null) {
    		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
    	}
    	
    	sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
    	if (sampleEntity == null) {
    		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
    	}

    	for(Entity lsmPairEntity : sampleEntity.getDescendantsOfType(EntityConstants.TYPE_LSM_STACK_PAIR)) {
        	for(EntityData ed : lsmPairEntity.getOrderedEntityData()) {
        		Entity lsmStack = ed.getChildEntity();
        		if (lsmStack != null && lsmStack.getEntityType().getName().equals(EntityConstants.TYPE_LSM_STACK)) {
        			String filepath = lsmStack.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        			inputFiles.add(new File(filepath));
        		}
        	}
    	}
    	
    	outputDir = new File(outputNode.getDirectoryPath());
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
            File lsmDataFile = new File(outputDir, createLsmMetadataFilename(inputFile)+".metadata");
            fw.write(inputFile.getAbsolutePath() + "\n");
            fw.write(lsmDataFile.getAbsolutePath() + "\n");
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
        script.append("cd "+outputDir.getAbsolutePath()).append("\n");
        script.append("echo " + addQuotes("$INPUT_FILENAME") + " >> " + lsmFilePathsFile.getAbsolutePath()).append("\n");
        script.append(getScriptToCreateLsmMetadataFile()).append("\n");

        writer.write(script.toString());
    }
    
    private String getScriptToCreateLsmMetadataFile() throws ServiceException {
        String cmdLine = "cd " + outputDir.getAbsolutePath() + ";perl " +
                SystemConfigurationProperties.getString("Executables.ModuleBase") + "singleNeuronTools/lsm_metadata_dump.pl " +
                addQuotes("$INPUT_FILENAME") + " " + addQuotes("$METADATA_FILENAME");

        return cmdLine;
    }

    private String addQuotes(String s) {
    	return "\""+s+"\"";
    }
    
    private String createLsmMetadataFilename(File lsmFile) {
        return lsmFile.getName().replaceAll("\\s+","_");
    }
}
