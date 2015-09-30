
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoAnnotationResultNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 24, 2009
 * Time: 2:06:42 PM
 */
public abstract class MgAnnoBaseService implements IService {
    protected Logger logger;

    protected static String queue = SystemConfigurationProperties.getString("MgAnnotation.Queue");
    protected static String hmmpfam2HtabCmd = SystemConfigurationProperties.getString("MgAnnotation.Hmmpfam2HtabCmd");
    protected static String hmmpfam2BsmlCmd = SystemConfigurationProperties.getString("MgAnnotation.Hmmpfam2BsmlCmd");
    protected static String hmmpfam3DbId = SystemConfigurationProperties.getString("MgAnnotation.Hmmpfam3DbId");
    protected static String hmmpfamFullDbId = SystemConfigurationProperties.getString("MgAnnotation.HmmpfamFullDbId");
    protected static String hmmpfamFragDbId = SystemConfigurationProperties.getString("MgAnnotation.HmmpfamFragDbId");
    protected static String pandaBlastpAlignments = SystemConfigurationProperties.getString("MgAnnotation.PandaBlastp.Alignments");
    protected static String pandaBlastpHits = SystemConfigurationProperties.getString("MgAnnotation.PandaBlastp.Hits");
    protected static String pandaBlastpDbId = SystemConfigurationProperties.getString("MgAnnotation.PandaBlastpDbId");
    protected static String parserCmd = SystemConfigurationProperties.getString("MgAnnotation.Parser");
    protected static String lipoIsMyco = SystemConfigurationProperties.getString("MgAnnotation.Lipoprotein.IsMycoplasma");
    protected static String lipoCmd = SystemConfigurationProperties.getString("MgAnnotation.Lipoprotein.Cmd");
    protected static String tmHmmCmd = SystemConfigurationProperties.getString("MgAnnotation.TmHmmCmd");
    protected static String tmHmmBsmlCmd = SystemConfigurationProperties.getString("MgAnnotation.TmHmmBsmlCmd");
    protected static String rulesAnnotateCmd = SystemConfigurationProperties.getString("MgAnnotation.RulesAnnotateCmd");
    protected static String htabValidationCmd = SystemConfigurationProperties.getString("MgAnnotation.HmmpfamHtabValidationCmd");
    protected static String htabValidationThreshold = SystemConfigurationProperties.getString("MgAnnotationHmmpfamHtabValidationThreshold");
    protected static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    protected static String changeHtabParsedFullToFragCmd = SystemConfigurationProperties.getString("MgAnnotation.ChangeHtabParsedFullToFragCmd");
    protected static String snapshotDir = SystemConfigurationProperties.getString("MgAnnotation.SnapshotDir"); 

    protected static Map<String, List<File>> parsedFileMap = new HashMap<String, List<File>>();

    MetaGenoAnnotationTask parentTask;
    MetaGenoAnnotationResultNode resultNode;
    File topInputFile;
    File inputFile;
    List<File> inputList;
    File parentDir;
    File workingDir;
    File resultFile;
    String serviceName;
    String fileId;
    boolean parentTaskErrorFlag = false;
    IProcessData processData;

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        this.processData = processData;
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        parentTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
        if (parentTask == null) {
            throw new MissingDataException("Could not get parent task for " + this.getClass().getName());
        }
        if (checkParentTaskForError(parentTask)) {
            this.parentTaskErrorFlag = true;
            throw new MissingDataException("Parent task has ERROR event");
        }
        resultNode = (MetaGenoAnnotationResultNode) processData.getItem("META_GENO_ANNOTATION_RESULT_NODE");
        if (resultNode == null) {
            throw new MissingDataException("Could not get result node for task=" + parentTask.getObjectId());
        }
        topInputFile = (File) processData.getItem("MG_INPUT_FILE");
        Object inputArray = processData.getItem("MG_INPUT_ARRAY");
        if (inputArray != null && inputArray instanceof File) {
            inputFile = (File) inputArray;
        }
        else if (inputArray != null && inputArray instanceof List) {
            inputList = (List<File>) inputArray;
        }
        else if (inputArray != null) {
            throw new MissingDataException("Could not interpret inputArray object type=" + inputArray.getClass().getName());
        }
        if (inputFile == null) {
            inputFile = topInputFile;
        }
        if (inputFile == null || !inputFile.exists()) {
            if (inputFile == null) {
                throw new MissingDataException("InputFile and TopInputFile are null");
            }
            else {
                throw new MissingDataException("Could not find file=" + inputFile.getAbsolutePath());
            }
        }
        fileId = (String) processData.getItem("MGA_FILE_ID");
        if (fileId == null) {
            throw new MissingDataException("Could not find MGA_FILE_ID in processData");
        }
    }

    public boolean isSubFileMode() {
        Object inputArray = processData.getItem("MG_INPUT_ARRAY");
        return inputArray != null && inputArray instanceof File;
    }

    protected void setup(String serviceName, String resultExtension) {
        this.parentDir = new File(resultNode.getDirectoryPath());
        this.workingDir = new File(parentDir, serviceName);
        if (!workingDir.exists()) {
            workingDir.mkdirs();
        }
        long uniqueFileId = (Long) TimebasedIdentifierGenerator.generate(1);
        this.resultFile = new File(workingDir, inputFile.getName() + "_" + uniqueFileId + resultExtension);
    }

    protected File getIdRepositoryDir() throws Exception {
        File idRepositoryDir = new File(parentDir, "id_repository");
        int maxRetries = 3;
        int retry = 0;
        boolean success = false;
        while (!success && retry < maxRetries) {
            try {
                if (!idRepositoryDir.exists()) {
                    if (!idRepositoryDir.mkdirs()) {
                        throw new Exception("Could not create id repository dir=" + idRepositoryDir.getAbsolutePath());
                    }
                    File idRepositoryFile = new File(idRepositoryDir, "valid_id_repository");
                    if (!idRepositoryFile.createNewFile()) {
                        throw new Exception("Could not create valid repository file=" + idRepositoryFile.getAbsolutePath());
                    }
                }
                success = true;
            }
            catch (Exception ex) {
                logger.info("getIdRepository failed with error=" + ex.getMessage() + ". Retry=" + retry + " of " + maxRetries);
                Thread.sleep(1000); // one second
            }
            retry++;
        }
        if (!success) {
            throw new Exception("Could not create or access id repository dir=" + idRepositoryDir.getAbsolutePath());
        }
        return idRepositoryDir;
    }

    private static synchronized List<File> getParsedFileListForFileId(String fileId) {
        List<File> parsedFileList = parsedFileMap.get(fileId);
        if (parsedFileList == null) {
            // need to allocate
            parsedFileList = new ArrayList<File>();
            parsedFileMap.put(fileId, parsedFileList);
        }
        return parsedFileList;
    }

    protected synchronized List<File> getParsedFileList() {
        return getParsedFileListForFileId(fileId);
    }

    protected synchronized void addParsedFile(File file) {
        List<File> parsedFileList = getParsedFileListForFileId(fileId);
        logger.info("To final parsed file list, adding file=" + file.getAbsolutePath());
        parsedFileList.add(file);
    }

    public static synchronized void addParsedFile(File file, String fileId) {
        List<File> parsedFileList = getParsedFileListForFileId(fileId);
        parsedFileList.add(file);
    }

    protected void setParentTaskToErrorStatus(Task parentTask, String message) {
        try {
            ComputeBeanRemote computeBean = getComputeBean();
            computeBean.saveEvent(parentTask.getObjectId(), Event.ERROR_EVENT, message, new Date());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected boolean checkParentTaskForError(Task parentTask) {
        try {
            ComputeBeanRemote computeBean = getComputeBean();
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            return status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }

    protected ComputeBeanRemote getComputeBean() {
        return EJBFactory.getRemoteComputeBean();
    }

}