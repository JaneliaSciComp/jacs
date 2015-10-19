
package org.janelia.it.jacs.web.gwt.common.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.*;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.genomics.SequenceType;
//import org.janelia.it.jacs.model.prokPipeline.ProkGenomeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.blast.Blastable;
//import org.janelia.it.jacs.model.user_data.geci.GeciImageDirectoryVO;
//import org.janelia.it.jacs.model.user_data.geci.NeuronalAssayAnalysisResultNode;
//import org.janelia.it.jacs.model.user_data.prokAnnotation.ProkAnnotationResultFileNode;
import org.janelia.it.jacs.server.access.*;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.server.api.BlastAPI;
import org.janelia.it.jacs.server.api.DataSetAPI;
import org.janelia.it.jacs.server.api.EntityAPI;
import org.janelia.it.jacs.server.api.UserAPI;
import org.janelia.it.jacs.server.utils.SystemException;
import org.janelia.it.jacs.shared.node.NodeFactory;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.web.control.blast.FileUploadController;
import org.janelia.it.jacs.web.control.blast.FileUploadController.FastaFileInfo;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.service.DataService;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.file.FileNodeRetriever;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class DataServiceImpl extends JcviGWTSpringController implements DataService {

    static Logger logger = Logger.getLogger(DataServiceImpl.class.getName());

    SystemConfigurationProperties properties = SystemConfigurationProperties.getInstance();
    private static final String UPLOAD_SCRATCH_DIR_PROP = "Upload.ScratchDir";

    private BlastAPI blastAPI = new BlastAPI();
    private DataSetAPI dataSetAPI = new DataSetAPI();
    private UserAPI userAPI = new UserAPI();
    private EntityAPI entityAPI = new EntityAPI();
    private FileNodeDAO _fileNodeDAO;
    private NodeDAO nodeDAO;
    private TaskDAO taskDAO;
    private UserDAO userDAO;
    private EntityDAO entityDAO;

    public void setBlastAPI(BlastAPI blastAPI) {
        this.blastAPI = blastAPI;
    }

    public void setDataSetAPI(DataSetAPI dataSetAPI) {
        this.dataSetAPI = dataSetAPI;
    }

    public void setUserAPI(UserAPI userAPI) {
        this.userAPI = userAPI;
    }

    public void setEntityAPI(EntityAPI entityAPI) {
        this.entityAPI = entityAPI;
    }

    public void setFileNodeDAO(FileNodeDAO fileNodeDAO) {
        _fileNodeDAO = fileNodeDAO;
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        this.nodeDAO = nodeDAO;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setEntityDAO(EntityDAO entityDAO) {
        this.entityDAO = entityDAO;
    }

    public BlastTask getBlastTaskById(Long taskId) throws GWTServiceException {
        logger.debug("DataServiceImpl.getBlastTaskById");
        try {
            BlastTask task = (BlastTask) blastAPI.getBlastTaskById(taskId);
            validateUserByLogin(task.getOwner());
            return task;
        }
        catch (SystemException e) {
            logger.error("Error in getBlastTaskById().", e);
            throw new GWTServiceException(e);
        }
    }

    public Set<String> getTaskMessages(String taskId) {
        logger.debug("DataServiceImpl.getTaskMessages");
        try {
            Task task = taskDAO.getTaskWithMessages(Long.parseLong(taskId));
            validateUserByLogin(task.getOwner());
            Set<String> messageStrings = new HashSet<String>();
            for (Object o : task.getMessages()) {
                TaskMessage message = (TaskMessage) o;
                messageStrings.add(message.getMessage());
            }
            return messageStrings;
        }
        catch (DaoException e) {
            logger.error("Error in getTaskWithMessages().", e);
        }
        return null;
    }

    /**
     * This method checks to see if the task corresponding to taskId has completed with
     * a status of Completed or Error
     *
     * @param taskId task to check status on
     * @return true if taask is in Complete or Error status; false otherwise
     */
    public Boolean isTaskDone(String taskId) throws GWTServiceException {
        Long longTaskId = Long.parseLong(taskId);
        Task task = getBlastTaskById(longTaskId);
        return task.isDone();
    }

    protected void createFastaFileNodeFromTmp(FastaFileNode fastaFileNode, String tmpFilename) throws Exception {
        logger.debug("Starting createFastaFileNodeFromTmp nodeId=" + fastaFileNode.getObjectId() + " tmpFilename=" + tmpFilename);
        String tmpDirname = properties.getProperty(UPLOAD_SCRATCH_DIR_PROP);
        if (tmpDirname == null)
            throw new Exception("System property not found for key=" + UPLOAD_SCRATCH_DIR_PROP);
        File tmpDir = new File(tmpDirname);
        File tmpFastaFile = new File(tmpDir, tmpFilename);
        long length = tmpFastaFile.length();
        String filestorePath = fastaFileNode.getFilePathByTag(FastaFileNode.TAG_FASTA);
        String filestoreDirPath = fastaFileNode.getDirectoryPath();
        File filestoreDir = new File(filestoreDirPath);
        if (!filestoreDir.exists()) {
            logger.debug("Creating directory=" + filestoreDir.getAbsolutePath());
            if (!filestoreDir.mkdirs())
                throw new Exception("createFastaFileNodeFromTmp() - unable to create directory " + filestoreDir.getAbsolutePath());
        }
        logger.debug("Before replaceAll, filestorePath=" + filestorePath);
        filestorePath = filestorePath.replaceAll("\\\\", "/");
        logger.debug("Using filestore path=" + filestorePath);
        File fileNodeFile = new File(filestorePath);
        if (fileNodeFile.exists()) {
            throw new Exception("createFastaFileNodeFromTmp() - fasta file node path=" + fileNodeFile.getAbsolutePath() + " already exists - will not copy over");
        }
        if (!tmpFastaFile.exists()) {
            throw new Exception("createFastaFileNodeFromTmp() - could not find tmp file=" + tmpFastaFile.getAbsolutePath());
        }
        // Note: this is done rather than system call to make the implementation platform independent.
        // If necessary, this could be done by system call instead rather than streaming through JVM.
        logger.debug("Starting file copy using nio channels");
        FileUtil.copyFile(tmpFastaFile, fileNodeFile);
        tmpFastaFile.delete();
        Long currentFastaLength = fastaFileNode.getLength();
        if (currentFastaLength == null || currentFastaLength <= 0) {
            // the length for this node should actually be the total query sequence length
            // however if that value is not set -> set it to the file length
            // which should at least give an indication of the sequence length
            fastaFileNode.setLength(length);
        }
        _fileNodeDAO.saveOrUpdateFileNode(fastaFileNode);
        logger.debug("Finished creating FastaFileNode id=" + fastaFileNode.getObjectId());
    }

    public UserDataNodeVO saveUserDefinedFastaNode(String nodeName, String fastaText, String visibility) throws Exception {
        Node newNode;
        String fastaFilename;
        if (nodeName == null || nodeName.length() == 0) {
            nodeName = "User FASTA";
        }
        if (logger.isDebugEnabled()) {
            String debugString = fastaText;
            if (null != debugString && debugString.length() > 200) {
                debugString = debugString.substring(0, 200);
            }
            logger.debug("FASTA Text is \n" + debugString + "\n...(truncated)...");
        }
        try {
            if (Constants.UPLOADED_FILE_NODE_KEY.equals(fastaText)) {
                newNode = (FastaFileNode) this.getThreadLocalRequest().getSession().getAttribute(Constants.UPLOADED_FILE_NODE_KEY);
                this.getThreadLocalRequest().getSession().removeAttribute(Constants.UPLOADED_FILE_NODE_KEY);
                if (newNode == null)
                    throw new Exception("FastaFileNode from Session is null for attribute key=" + Constants.UPLOADED_FILE_NODE_KEY);
            }
            else {
                if (!fastaText.trim().startsWith(">")) {
                    fastaText = ">sequence\n" + fastaText;
                }
                FastaFileInfo info = FileUploadController.createTmpFastaFile(new ByteArrayInputStream(fastaText.getBytes()));
                newNode = FileUploadController.createFastaFileNodeFromInfo(info, nodeName);
            }
            fastaFilename = newNode.getName();  // switch these as a mechanism for communicating filename
            newNode.setDescription(nodeName); // update description (which will become node name) in case it changed on the GUI since being uploaded
            if (fastaFilename == null) {
                throw new Exception("fastaFilename is null");
            }
        }
        catch (Exception e) {
            logger.error("Exception in saveUserDefinedFastaNode()=" + e.getMessage(), e);
            return null;
        }
        catch (OutOfMemoryError e) {
            logger.error("Out of memory in saveUserDefinedFastaNode(). Memory available: " +
                    Runtime.getRuntime().freeMemory() / 1024 + "K", e);
            return null;
        }
        newNode.setVisibility(visibility);
        logger.debug("Finished retrieving node from session");
        Node returnNode;
        try {
            // Save assume FastaFileNode since already checked possibilities above
            logger.debug("Calling dataSetAPI.saveOrUpdateNode");
            returnNode = dataSetAPI.saveOrUpdateNode(getSessionUser().getUserLogin(), newNode);
            if (returnNode == null) {
                throw new Exception("returnNode from dataSetAPI.saveOrUpdateNode() is null");
            }
            logger.debug("Casting returnNode to FastaFileNode");
            FastaFileNode fastaFileNode = (FastaFileNode) returnNode;
            logger.debug("Calling createFastaFileNodeFromTmp()");
            createFastaFileNodeFromTmp(fastaFileNode, fastaFilename);
            if (null == returnNode) {
                return null;
            }
        }
        catch (SystemException e) {
            logger.error("Failed saving user-provided data persist.\n" + e.getMessage(), e);
            return null;
        }
        catch (OutOfMemoryError e) {
            logger.error("Out of memory in saveUserDefinedFastaNode(). Memory available: " +
                    Runtime.getRuntime().freeMemory() / 1024 + "K", e);
            return null;
        }
        catch (Exception e) {
            logger.error("Exception in saveUserDefinedFastaNode()=" + e.getMessage(), e);
            return null;
        }

        logger.debug("Save of node to filestore returned successfully. Returning BlastableNodeVO");
        return new UserDataNodeVO(
                returnNode.getObjectId().toString(),
                returnNode.getDescription(),
                returnNode.getVisibility(),
                returnNode.getDataType(),
                ((Blastable) returnNode).getSequenceType(),
                returnNode.getName(),
                returnNode.getOwner(),
                Integer.toString(fastaText.length()),
                TimebasedIdentifierGenerator.getTimestamp(returnNode.getObjectId()),
                /*sequence*/ null,
                /* parentTaskStatus */ null);
    }


    public String getSequenceTypeForFASTA(String fastaText) {
        try {
            return NodeFactory.determineFastaSequenceType(fastaText);
        }
        catch (Exception e) {
            logger.error("Error determining the FASTA sequence type. " + e.getMessage(), e);
            return SequenceType.NOT_SPECIFIED;
        }
    }

    public String getFilenodeByTaskId(String taskId, String contentType) throws Exception {
        return new FileNodeRetriever(_fileNodeDAO).retrieveFileNode(Long.parseLong(taskId), contentType, getSessionUser());
    }

    public List<BlastHit> getPagedBlastHitsByTaskId(String taskId, int startIndex, int numRows, SortArgument[] sortArgs) {
        try {
            validateUserByTaskId(taskId);
            return (dataSetAPI.getPagedBlastHitsByTaskId(taskId, startIndex, numRows, sortArgs));
        }
        catch (Exception e) {
            logger.error("Error retrieving blast result persist: " + e.getMessage(), e);
            return null;
        }
    }

//    public Map<Site, Integer> getSitesForBlastResult(String taskId) {
//        try {
//            validateUserByTaskId(taskId);
//            return (dataSetAPI.getSitesForBlastResult(taskId));
//        }
//        catch (Exception e) {
//            logger.error("Error retrieving sites for blast result : " + e.getMessage(), e);
//            return null;
//        }
//    }
//
    public String replaceNodeName(String nodeId, String nodeName) {
        try {
            return dataSetAPI.replaceNodeName(nodeId, nodeName);
        }
        catch (Exception e) {
            logger.error("Error calling dataSetAPI.replaceNodeName(nodeId=" + nodeId + ", nodeName=" + nodeName + ") : " + e.getMessage());
            return null;
        }
    }

    public void deleteNode(String nodeId) {
        try {
            dataSetAPI.deleteNode(getSessionUser(), nodeId);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    public void markUserForDeletion(String userId) {
        try {
            userAPI.markUserForDeletion(userId);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    public Integer getNumBlastableNodesForUser(String searchString, String sequenceType) {
        User user;
        try {
            user = getSessionUser();
            if (user == null)
                logger.error("getNumBlastableNodesForUser: user is null");
            return dataSetAPI.getNumBlastableNodesForUser(searchString, sequenceType, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public UserDataNodeVO[] getPagedBlastableNodesForUser(String searchString, String sequenceType, int startIndex, int numRows, SortArgument[] sortArgs) {
        try {
            return dataSetAPI.getPagedBlastableNodesForUser(searchString, sequenceType, startIndex, numRows, sortArgs, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public List<String> getBlastableNodeNamesForUser(String searchString, String sequenceType) {
        try {
            return dataSetAPI.getBlastableNodeNamesForUser(searchString, sequenceType, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public void setTaskExpirationAndName(String taskId, Date expirationDate, String jobName) throws GWTServiceException {
        try {
            taskDAO.setTaskExpirationAndName(new Long(taskId), expirationDate, jobName);
        }
        catch (DaoException e) {
            logger.error("Exception: " + e.getMessage());
            throw new GWTServiceException("Update Task Failed");
        }
    }

    public List<String> getUserLogins() throws GWTServiceException {
        try {
            List<User> tmpUsers = userDAO.findAll();
            ArrayList<String> tmpUserLogins = new ArrayList<String>();
            for (User tmpUser : tmpUsers) {
                tmpUserLogins.add(tmpUser.getUserLogin());
            }
            Collections.sort(tmpUserLogins);
            return tmpUserLogins;
        }
        catch (DaoException e) {
            logger.error("Exception: " + e.getMessage());
            throw new GWTServiceException("getUsers Failed");
        }
    }

    public UserDataVO[] getPagedUsers(String searchString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException {
        try {
            return userAPI.getPagedUsers(searchString, startIndex, numRows, sortArgs);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public Integer getNumUsers(String searchString) throws GWTServiceException {
        try {
            return userAPI.getNumUsers(searchString);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

//    public BlastableNodeVO[] getReversePsiBlastDatasets() {
//        try {
//            return dataSetAPI.getReversePsiBlastDatasets();
//        }
//        catch (Exception e) {
//            logger.error("Error: ", e);
//            return null;
//        }
//    }
//
    public String submitJob(Task newTask) throws GWTServiceException {
        logger.info("org.janelia.it.jacs.web.gwt.common.server.DataServiceImpl.submitJob()");
        String jobId;
        try {
            jobId = dataSetAPI.submitJob(getSessionUser(), newTask);
        }
        catch (Throwable e) {
            logger.error("Job failed: " + e.getMessage(), e);
            jobId = "Job failed.";
            throw new GWTServiceException(jobId, e);
        }

        return (jobId);
    }

    public List<String> getFiles(String tmpDirectory, boolean directoriesOnly) throws GWTServiceException {
        try {
            return dataSetAPI.getFiles(tmpDirectory, directoriesOnly);
        }
        catch (SystemException e) {
            throw new GWTServiceException("Unable to obtain directory information.");
        }
    }

    public HashSet<String> getProjectCodes() throws GWTServiceException {
        try {
            return dataSetAPI.getProjectCodes();
        }
        catch (SystemException e) {
            throw new GWTServiceException("Unable to obtain project code information.");
        }
    }

//    public ProkGenomeVO getProkGenomeVO(String targetGenome) throws GWTServiceException {
//        try {
//            ProkGenomeVO tmpGenomeVO = new ProkGenomeVO(targetGenome);
//            ProkAnnotationResultFileNode tmpNode = (ProkAnnotationResultFileNode) nodeDAO.getNodeByName(targetGenome);
//            tmpGenomeVO.setTargetOutputDirectory(tmpNode.getDirectoryPath());
//
//            tmpGenomeVO.setEvents(nodeDAO.getAllEventsRelatedToData(tmpNode, "targetDirectory", targetGenome, "prok"));
//            return tmpGenomeVO;
//        }
//        catch (Exception e) {
//            throw new GWTServiceException("Unable to obtain genome information for " + targetGenome);
//        }
//    }
//
    @Override
    public void validateFilePath(String filePath) throws GWTServiceException {
        try {
            dataSetAPI.validateFilePath(filePath);
        }
        catch (Exception e) {
            throw new GWTServiceException("Unable to validate the path:  " + filePath);
        }
    }

    public Integer getNumNodesForUserByName(String nodeClassName) {
        User user;
        try {
            user = getSessionUser();
            if (user == null)
                logger.error("getNumNodesForUserByName: user is null");
            return dataSetAPI.getNumNodesForUserByName(nodeClassName, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public UserDataNodeVO[] getPagedNodesForUserByName(String nodeClassName, int startIndex, int numRows, SortArgument[] sortArgs) {
        try {
            return dataSetAPI.getPagedNodesForUserByName(nodeClassName, startIndex, numRows, sortArgs, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public List<String> getEntityTypeNames() throws GWTServiceException {
        try {
            List<EntityType> tmpEntityTypes = entityDAO.findAllEntityTypes();
            ArrayList<String> tmpEntityTypeNames = new ArrayList<String>();
            for (EntityType tmpEntityType : tmpEntityTypes) {
                tmpEntityTypeNames.add(tmpEntityType.getName());
            }
            Collections.sort(tmpEntityTypeNames);
            return tmpEntityTypeNames;
        }
        catch (DaoException e) {
            logger.error("Exception: " + e.getMessage());
            throw new GWTServiceException("getEntityTypes Failed");
        }
    }

    public Integer getNumEntityTypes(String searchString) throws GWTServiceException {
        try {
            return entityAPI.getNumEntityTypes(searchString);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

    public List<EntityType> getPagedEntityTypes(String searchString, int startIndex, int numRows, SortArgument[] sortArgs)
            throws GWTServiceException {
        try {
            return entityAPI.getPagedEntityTypes(searchString, startIndex, numRows, sortArgs);
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }
    }

//    // NOTE: This method assumes the fileshare is the same for web server and compute server.  A little risky.  Find a better way to abstract this.
//    public List<GeciImageDirectoryVO> findPotentialResultNodes(String filePath) throws GWTServiceException {
//        List<GeciImageDirectoryVO> returnList = new ArrayList<GeciImageDirectoryVO>();
//        File rootDir = new File(filePath);
//        if (!rootDir.exists()||!rootDir.canRead()) {
//            throw new GWTServiceException("Cannot access "+filePath+" or the directory does not exist.");
//        }
//        File[] plateFolders = rootDir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File file, String s) {
//                return new File(file,s).isDirectory() && s.startsWith("P");
//            }
//        });
//        for (File plateFolder : plateFolders) {
//            // Check in the database for a path to this node
//            try {
//                Node plateNode = nodeDAO.getNodeByName(plateFolder.getName());
//                // If we don't know this plate then create the node
//                if (null==plateNode || !(plateNode instanceof NeuronalAssayAnalysisResultNode)) {
//                    NeuronalAssayAnalysisResultNode tmpNode = new NeuronalAssayAnalysisResultNode(getSessionUser().getUserLogin(), null,
//                            plateFolder.getName(), plateFolder.getName(), Node.VISIBILITY_PRIVATE, null);
//                    tmpNode.setPathOverride(plateFolder.getAbsolutePath());
//                    plateNode = dataSetAPI.saveOrUpdateNode(getSessionUser().getUserLogin(), tmpNode);
//                }
//                GeciImageDirectoryVO tmpVO = new GeciImageDirectoryVO();
//                tmpVO.setLocalDirName(plateNode.getName());
//                tmpVO.setTargetDirectoryPath(((NeuronalAssayAnalysisResultNode)plateNode).getDirectoryPath());
//                tmpVO.setNodeId(plateNode.getObjectId());
//                File[] imagesDir = plateFolder.listFiles(new FilenameFilter() {
//                    @Override
//                    public boolean accept(File file, String s) {
//                        return s.equalsIgnoreCase("imaging") && new File(file,s).isDirectory();
//                    }
//                });
//                if (null!=imagesDir && imagesDir.length>=1) {
//                    tmpVO.setProcessed(true);
//                }
//                returnList.add(tmpVO);
//            } catch (Exception e) {
//                logger.error("Error checking for node with name "+plateFolder.getName());
//                throw new GWTServiceException("Error looking for NAA Nodes", e);
//            }
//        }
//        return returnList;
//    }
//
    @Override
    public List<String> getNodeNamesForUserByName(String nodeClassName) throws GWTServiceException {
        try {
            return dataSetAPI.getNodeNamesForUserByName(nodeClassName, getSessionUser().getUserLogin());
        }
        catch (Exception e) {
            logger.error("Error: ", e);
            return null;
        }

    }

    @Override
    public void syncUserData(String username) throws GWTServiceException {
        try {
            dataSetAPI.syncUserData(getSessionUser(), username);
        }
        catch (SystemException e) {
            throw new GWTServiceException("Error syncing the user data", e);
        }
    }
}
