package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Copy LSM metadata files into a separation result node. The files must be present for NeuronAnnotator to get 
 * an accurate z height for the stack.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MigrateLsmMetadataFilesService implements IService {

    protected Logger logger;
    protected EntityBeanLocal entityBean;


    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            entityBean = EJBFactory.getLocalEntityBean();

        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	FileNode resultFileNode = (FileNode)processData.getItem("SEPARATE_RESULT_FILE_NODE");
        	if (resultFileNode == null) {
        		throw new IllegalArgumentException("SEPARATE_RESULT_FILE_NODE may not be null");
        	}
        	
        	File targetDir = new File(resultFileNode.getDirectoryPath());
        	
        	Entity sampleEntity = entityBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	File metadataDir = new File(getLatestDir(sampleEntity, EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT), "metadata");
        	
        	if (metadataDir==null || !metadataDir.exists()) {
        		logger.warn("Could not find metadata directory at "+metadataDir+" and will fallback on last neuron separation result");
        		metadataDir = new File(getLatestDir(sampleEntity, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).getParentFile(), "metadata");
        	}
        	
        	if (metadataDir==null || !metadataDir.exists()) {
        		throw new Exception("Could not find metadata directory at "+metadataDir);
        	}
        	
        	migrateLsmMetadata(metadataDir, targetDir);
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private File getLatestDir(Entity sampleEntity, String entityTypeName) {
    	Entity sampleProcessingResult = sampleEntity.getLatestChildOfType(entityTypeName);
    	if (sampleProcessingResult == null) return null;
    	String filepath = sampleProcessingResult.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
    	return new File(filepath);
    }
    
    private void migrateLsmMetadata(File sourceDir, File targetDir) throws Exception {
    	
    	File[] files = sourceDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.endsWith("metadata");
			}
		});
    	
    	if (files==null || files.length==0) {
    		throw new Exception("Could not find metadata files in "+sourceDir.getAbsolutePath());
    	}
		
    	List<File> metadataFiles = Arrays.asList(files);

        StringBuffer script = new StringBuffer();
    	for(File file : metadataFiles) {
    		script.append("cp ");
    		script.append(file.getAbsolutePath());
    		script.append(" ");
    		script.append(targetDir.getAbsolutePath());
    		script.append("\n");
    	}
    	
    	File scriptFile = new File(targetDir.getAbsolutePath(), "migrateMetadata.sh");
    	FileUtils.writeStringToFile(scriptFile, script.toString());
    	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine("sh "+scriptFile.getAbsolutePath(), true, 60);

        if (exitCode != 0) {
        	throw new Exception("LSM Migration failed with exit code "+exitCode);
        }
    }
}
