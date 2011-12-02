package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

/**
 * Copy LSMs from the latest run of a Sample to the given file node. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MigrateLsmMetadataFilesService implements IService {

    protected Logger logger;
    protected AnnotationBeanLocal annotationBean;


    public void execute(IProcessData processData) throws ServiceException {
        try {
        	
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            annotationBean = EJBFactory.getLocalAnnotationBean();

        	String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        	if (sampleEntityId == null) {
        		throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        	}
        	
        	FileNode resultFileNode = (FileNode)processData.getItem("SEPARATE_RESULT_FILE_NODE");
        	if (resultFileNode == null) {
        		throw new IllegalArgumentException("SEPARATE_RESULT_FILE_NODE may not be null");
        	}
        	
        	File targetDir = new File(resultFileNode.getDirectoryPath());
        	
        	Entity sampleEntity = annotationBean.getEntityTree(new Long(sampleEntityId));
        	if (sampleEntity == null) {
        		throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        	}
        	
        	List<Entity> children = sampleEntity.getOrderedChildren();
        	Collections.reverse(children);
        	for(Entity child : children) {
        		if (!child.getEntityType().getName().equals(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) continue;
        		// Child is the latest separation result
            	String filepath = child.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            	File dir = new File(filepath);
            	migrateLsmMetadata(new File(dir.getParent(),"metadata"), targetDir);
        		break;
        	}
        	
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }
    
    private void migrateLsmMetadata(File sourceDir, File targetDir) throws Exception {

    	List<File> metadataFiles = Arrays.asList(sourceDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
	            return name.endsWith("metadata");
			}
		}));

        StringBuffer script = new StringBuffer();
    	for(File file : metadataFiles) {
    		script.append("cp ");
    		script.append(file.getAbsolutePath());
    		script.append(" ");
    		script.append(targetDir.getAbsolutePath());
    		script.append("; ");
    	}
    	
        SystemCall call = new SystemCall(logger);
        int exitCode = call.emulateCommandLine(script.toString(), true, 60);
        if (exitCode != 0) {
        	throw new Exception("LSM Migration failed with exit code "+exitCode);
        }
    }
}
