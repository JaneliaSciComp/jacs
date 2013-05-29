package org.janelia.it.jacs.compute.service.entity;

import java.io.File;
import java.util.List;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
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
public class SampleTrashCompactorService extends AbstractEntityService {

    public transient static final String PARAM_testRun = "is test run";
    
	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	public transient static final String CENTRAL_ARCHIVE_DIR_PROP = "FileStore.CentralDir.Archived";
	
	private String username;
    private boolean isDebug = false;
    private int numDirs = 0;
    private int numResultNodes = 0;
    private int numDeletedResultNodes = 0;
    private int numDefunctDirs = 0;
    
    private File userFilestore;
    private File archiveFilestore;
    
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        this.username = ownerKey.split(":")[1];
        this.userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
        this.archiveFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_ARCHIVE_DIR_PROP) + File.separator + username + File.separator);
        
        logger.info("Synchronizing file share directory to DB: "+userFilestore.getAbsolutePath());
        
        if (isDebug) {
        	logger.info("This is a test run. No files will be moved or deleted.");
        }
        else {
        	logger.info("This is the real thing. Files will be moved and/or deleted!");
        }
        
        processFilestore(userFilestore);
        processFilestore(archiveFilestore);
        
		logger.info("Processed "+numDirs+" directories. Found "+numResultNodes+" result nodes, of which "+numDefunctDirs+
		        " were defunct. Trashed "+numDeletedResultNodes+" nodes. Left "+(numResultNodes-numDeletedResultNodes)+" nodes alone.");
    }
    
    private void processFilestore(File filestoreDir) throws Exception {
        processChildren(new File(filestoreDir, "Sample"));
        processChildren(new File(filestoreDir, "Alignment"));
        processChildren(new File(filestoreDir, "Separation"));
        processChildren(new File(filestoreDir, "Intersection"));
        processChildren(new File(filestoreDir, "Temp"));
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
            // This may be a node owned by another database, or it may be a rogue directory. Let's just leave it alone.
        	logger.warn("Ignoring subdir because it is not a node: "+dir.getName());
        }
        else if (node instanceof SampleResultNode || node instanceof AlignmentResultNode 
        		|| node instanceof SeparationResultNode || node instanceof NamedFileNode) {
        	
            FileNode filenode = (FileNode)node;
            
            if (!dir.getAbsolutePath().equals(filenode.getDirectoryPath())) {
                logger.warn("Node "+node.getObjectId()+" is no longer located at "+dir);
                numDefunctDirs++;
                if (!isDebug) {
                    if (dir.delete()) {
                        logger.info("Deleted defunct node directory "+dir);
                    }
                    else {
                        logger.error("Defunct node directory cannot be deleted: "+dir);
                    }
                }
            }
            else {
                numResultNodes++;
                List<Entity> entities = entityBean.getEntitiesWithAttributeValue(EntityConstants.ATTRIBUTE_FILE_PATH, dir.getAbsolutePath()+"%");
                if (entities.isEmpty()) {
                    logger.info(dir+ " has no references, trashing it.");    
                    if (!isDebug) computeBean.trashNode(username, node.getObjectId(), true);
                    numDeletedResultNodes++;
                }
                else {
                    logger.info(dir +" has "+entities.size()+" references to it, leaving it alone.");
                }
            }
            
        }
        else {
			logger.info("Ignoring subdir which is not a recognized node type (class is "+node.getClass().getName()+")");
        }
    }
    
}
