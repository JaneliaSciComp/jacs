package org.janelia.it.jacs.compute.service.domain;

import java.io.File;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
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
public class SampleTrashCompactorService extends AbstractDomainService {

    public transient static final String PARAM_testRun = "is test run";
    
	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	public transient static final String CENTRAL_ARCHIVE_DIR_PROP = "FileStore.CentralDir.Archived";

    protected static final String JACS_DATA_DIR =
        SystemConfigurationProperties.getString("JacsData.Dir.Linux");
    
    protected static final String JACS_DATA_ARCHIVE_DIR =
            SystemConfigurationProperties.getString("JacsData.Dir.Archive.Linux");
    
    private boolean isDebug = false;
    private boolean archiveEnabled = false;
	private String username;
    private SolrConnector solr;
    private File userFilestore;
    private File archiveFilestore;
    
    private int numDirs = 0;
    private int numResultNodes = 0;
    private int numDeletedResultNodes = 0;
    private int numDefunctDirs = 0;
    
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }
        
        this.username = DomainUtils.getNameFromSubjectKey(ownerKey);
        this.solr = new SolrConnector(DomainDAOManager.getInstance().getDao());
        this.userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
        this.archiveFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_ARCHIVE_DIR_PROP) + File.separator + username + File.separator);
        
        logger.info("Synchronizing file share directory to DB: "+userFilestore.getAbsolutePath());
        
        if (isDebug) {
        	logger.info("This is a test run. No files will be moved or deleted.");
        }
        else {
        	logger.info("This is the real thing. Files will be moved and/or deleted!");
        }
        
        archiveEnabled = !userFilestore.getAbsolutePath().equals(archiveFilestore.getAbsolutePath());
        
        processFilestore(userFilestore);
        if (archiveEnabled) {
        	processFilestore(archiveFilestore);
        }
        
		logger.info("Processed "+numDirs+" directories. Found "+numResultNodes+" result nodes, of which "+numDefunctDirs+
		        " were defunct. Trashed "+numDeletedResultNodes+" nodes. Left "+(numResultNodes-numDeletedResultNodes)+" nodes alone.");
    }
    
    private void processFilestore(File filestoreDir) throws Exception {
        processChildren(new File(filestoreDir, "Summary"));
        processChildren(new File(filestoreDir, "Sample"));
        processChildren(new File(filestoreDir, "Post"));
        processChildren(new File(filestoreDir, "Alignment"));
        processChildren(new File(filestoreDir, "Separation"));
        processChildren(new File(filestoreDir, "Intersection"));
        processChildren(new File(filestoreDir, "NeuronMerge"));
        processChildren(new File(filestoreDir, "Normalization"));
        processChildren(new File(filestoreDir, "Temp"));
        processChildren(new File(filestoreDir, "Error"));
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
                String path = dir.getAbsolutePath();
                
                numResultNodes++;
                long numEntities = getCountWithPathPrefix(path);
                if (numEntities==0) {
                    
                    if (archiveEnabled) {
                        // Because some nodes may have dual citizenship on groups and archive, for legacy reasons, we need
                        // to check the sister directory as well:
                        
                        String sisterPath = null;
                        if (path.startsWith(JACS_DATA_DIR)) {
                            sisterPath = path.replaceFirst(JACS_DATA_DIR, JACS_DATA_ARCHIVE_DIR);
                        }
                        else  if (path.startsWith(JACS_DATA_ARCHIVE_DIR)) {
                            sisterPath = path.replaceFirst(JACS_DATA_ARCHIVE_DIR, JACS_DATA_DIR);
                        }
                        else {
                            logger.error("Unknown path prefix: "+path);
                            return;
                        }
                        
                        numEntities = getCountWithPathPrefix(sisterPath);
                    }
                    
                    if (numEntities==0) {
                        logger.info(dir+ " has no references, trashing it.");    
                        if (!isDebug) computeBean.trashNode(username, node.getObjectId(), true);
                        numDeletedResultNodes++;
                    }
                    else {
                        logger.debug(dir +" has "+numEntities+" references to its sister path, leaving it alone.");
                    }
                }
                else {
                    logger.debug(dir +" has "+numEntities+" references to it, leaving it alone.");
                }
            }
            
        }
        else {
			logger.warn("Ignoring subdir which is not a recognized node type (class is "+node.getClass().getName()+")");
        }
    }
    
    private long getCountWithPathPrefix(String path) throws Exception {
        // TODO: since it's very hard to search all of Mongo, the way we used to search all the Entities, we are using Solr instead.
        // However, this relies on Solr always having an up-to-date view of Mongo. We need to either ensure that's the case, or 
        // find another solution to this. 
        SolrQuery query = new SolrQuery(path);
        QueryResponse qr = solr.search(query);
        return qr.getResults().getNumFound();
    }
}
