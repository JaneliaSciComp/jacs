package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.entity.SampleResultNode;

/**
 * Synchronizes the Samples in the database to the FileNodes on the fileshare.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleFileNodeSyncService implements IService {
	
	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	
    protected Logger logger;
    protected Task task;
    protected AnnotationBeanLocal annotationBean;
    protected ComputeBeanLocal computeBean;
    
    private boolean isDebug = false;
    
    public void execute(IProcessData processData) throws ServiceException {

    	try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            annotationBean = EJBFactory.getLocalAnnotationBean();
            computeBean = EJBFactory.getLocalComputeBean();
            String username = task.getOwner();
            
            File sampleDir = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator + "Sample");
            
            logger.info("Synchronizing file share directory to DB: "+sampleDir.getAbsolutePath());
            
            File[] dirs = sampleDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory();
				}
            });
        	
            int numSampleResultNodes = 0;
            int numDeletedFileNodes = 0;
            
        	for(File dir : dirs) {
        		Node node = null;
        		try {
        			long fileNodeId = Long.parseLong(dir.getName());
        			node = computeBean.getNodeById(fileNodeId);
        		}
        		catch (NumberFormatException e) {
        			// Not an identifier, that's ok, just ignore it
        			logger.info("Ignoring subdir because name is not an id: "+dir.getName());
        			continue;
        		}
        		
                if (null == node) {
                    // If we get here, we have a numeric dirname which the DB knows nothing about. But maybe the production DB does?? Yikes, we can't delete it. 
//                    if (!isDebug) FileUtil.deleteDirectory(dir);
//                    logger.debug("Deleted orphaned node " + dir.getName());
                	logger.info("Ignoring subdir because it is not a node: "+dir.getName());
                }
                else if (node instanceof SampleResultNode) {
                	numSampleResultNodes++;
                	
                	List<Entity> entities = annotationBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath()+"%");
                	
                	if (entities.isEmpty()) {
                        if (!isDebug) computeBean.deleteNode(username, node.getObjectId(), true);
                        logger.debug("Deleted unreferenced node: " + node.getObjectId());	
                        numDeletedFileNodes++;
                	}
                	else {
                		logger.debug("Node " + node.getObjectId() +" has "+entities.size()+" references to it, leaving it alone.");
                	}
                }
                else {
        			logger.info("Ignoring subdir which is not a SampleResultNode but a "+node.getClass().getName());
                }
        	}
        	
			logger.info("Processed "+dirs.length+" directories. Found "+numSampleResultNodes+" sample result nodes. Deleted "+numDeletedFileNodes+" nodes.");
    	}
        catch (Exception e) {
        	if (e instanceof ServiceException) {
            	throw (ServiceException)e;
            }
            throw new ServiceException("Error running FileCopyingService:" + e.getMessage(), e);
        }
    	
    }
}
