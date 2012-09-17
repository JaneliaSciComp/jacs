package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.entity.AlignmentResultNode;
import org.janelia.it.jacs.model.user_data.entity.NamedFileNode;
import org.janelia.it.jacs.model.user_data.entity.SampleResultNode;
import org.janelia.it.jacs.model.user_data.entity.SeparationResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Synchronizes the Samples in the database to the FileNodes on the fileshare.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleFileNodeSyncService implements IService {

    public transient static final String PARAM_testRun = "is test run";
    
	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
    protected Logger logger;
    protected Task task;
    protected String username;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    
    private boolean isDebug = false;
    private int numDirs = 0;
    private int numResultNodes = 0;
    private int numDeletedResultNodes = 0;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            username = task.getOwner();
            
            String testRun = task.getParameter(PARAM_testRun);
            if (testRun!=null) {
            	isDebug = Boolean.parseBoolean(testRun);	
            }
            
            File userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
            
            logger.info("Synchronizing file share directory to DB: "+userFilestore.getAbsolutePath());
            
            if (isDebug) {
            	logger.info("This is a test run. No files will be moved or deleted.");
            }
            else {
            	logger.info("This is the real thing. Files will be moved and/or deleted!");
            }
            
            processChildren(new File(userFilestore, "Sample"));
            processChildren(new File(userFilestore, "Alignment"));
            processChildren(new File(userFilestore, "Separation"));
            processChildren(new File(userFilestore, "Intersection"));
            processChildren(new File(userFilestore, "Temp"));
            
			logger.info("Processed "+numDirs+" directories. Found "+numResultNodes+" result nodes. Trashed "+
					numDeletedResultNodes+" nodes. Left "+(numResultNodes-numDeletedResultNodes)+" nodes alone.");
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running SampleFileNodeSyncService:" + e.getMessage(), e);
        }
    }
    
    private void processChildren(File dir) throws Exception {
    	if (dir==null || !dir.canRead() || !dir.isDirectory()) return;
    	for(File childDir : FileUtil.getSubDirectories(dir)) {
    		if (childDir.getName().matches("^\\d{3}$")) {
    			processChildren(childDir);
    		}
    		else {
    			processDir(childDir);
    		}
    	}
    }
    
    private void processDir(File dir) throws Exception {

    	numDirs++;
    	
		Node node = null;
		try {
			long fileNodeId = Long.parseLong(dir.getName());
			node = computeBean.getNodeById(fileNodeId);
		}
		catch (NumberFormatException e) {
			// Not an identifier, that's ok, just ignore it
			logger.info("Ignoring subdir because name is not an id: "+dir.getName());
			return;
		}
		
        if (null == node) {
            // This may be a node owned by another database... just leave it alone 
        	logger.info("Ignoring subdir because it is not a node: "+dir.getName());
        }
        else if (node instanceof SampleResultNode || node instanceof AlignmentResultNode 
        		|| node instanceof SeparationResultNode || node instanceof NamedFileNode) {
        	
        	numResultNodes++;
        	
        	List<Entity> entities = entityBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath()+"%");
        	if (entities.isEmpty()) {
                if (!isDebug) computeBean.trashNode(username, node.getObjectId(), true);
                logger.debug("Trashed unreferenced node: " + node.getObjectId());	
                numDeletedResultNodes++;
        	}
        	else {
        		logger.debug("Node " + node.getObjectId() +" has "+entities.size()+" references to it, leaving it alone.");
        	}
        }
        else {
			logger.info("Ignoring subdir which is not a recognized node type (class is "+node.getClass().getName()+")");
        }
    }
    
}
