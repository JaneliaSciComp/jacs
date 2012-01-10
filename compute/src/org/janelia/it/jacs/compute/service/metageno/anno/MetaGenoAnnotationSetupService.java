
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.tasks.metageno.PriamTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoAnnotationResultNode;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:32:32 PM
 */
public class MetaGenoAnnotationSetupService implements IService {
    private Logger logger;

    String setupType = "UNKNOWN";
    String sessionName;
    ComputeBeanRemote computeBean;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.setupType = (String) processData.getMandatoryItem("SETUP_TYPE");
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            computeBean = EJBFactory.getRemoteComputeBean();
            MetaGenoAnnotationTask parentTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
            if (checkParentTaskForError(processData)) {
                logger.error("Parent task indicates ERROR event - returning immediately");
                return;
            }
            if (setupType.equals("START")) {
                logger.info(this.getClass().getName() + " START execute() start");
                // Create new result node
                MetaGenoAnnotationResultNode resultFileNode = new MetaGenoAnnotationResultNode(parentTask.getOwner(), parentTask, "MetaGenoAnnotationResultNode", "MetaGenoAnnotationResultNode for task "
                        + parentTask.getObjectId(), Node.VISIBILITY_PRIVATE, sessionName);
                resultFileNode = (MetaGenoAnnotationResultNode) computeBean.saveOrUpdateNode(resultFileNode);
                FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
                FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
                Long queryNodeId = getInputFileNodeIdFromTask(parentTask);
                File inputFile = getFastaFile(queryNodeId);
                processData.putItem("META_GENO_ANNOTATION_RESULT_NODE", resultFileNode);
                processData.putItem("MG_INPUT_FILE", inputFile);
                processData.putItem("MGA_FILE_ID", queryNodeId.toString());
                logger.info("ProcessData META_GENO_ANNOTATION_RESULT_NODE directoryPath=" + resultFileNode.getDirectoryPath());
                logger.info("ProcessData MG_INPUT_FILE path=" + inputFile.getAbsolutePath());
                logger.info("ProcessData MGA_FILE_ID=" + queryNodeId.toString());
                logger.info(this.getClass().getName() + " START execute() finish");
            }
            else if (setupType.equals("PIPELINE")) {
                logger.info(this.getClass().getName() + " PIPELINE execute() start");

                // With this version, we let the top-level MGA_FILE_ID pass through for consolidating

                // Create Fasta File Node from input file
                //File inputFile=(File)processData.getItem("MG_INPUT_FILE");
                //Long fastaFileNodeId=createFastaFileNode(inputFile);
                // the parsed files.
                //String MGA_FILE_ID=fastaFileNodeId.toString();
                //processData.putItem("MGA_FILE_ID", MGA_FILE_ID);

                PriamTask priamTask = PriamEcService.createDefaultTask();
                priamTask.setOwner(parentTask.getOwner());
                priamTask.setParameter("project", parentTask.getParameter("project"));
                priamTask.setParentTaskId(parentTask.getObjectId());
                priamTask = (PriamTask) computeBean.saveOrUpdateTask(priamTask);
                logger.info("Adding subtask PriamTask id=" + priamTask.getObjectId() + " to processData with parentTaskId=" + parentTask.getObjectId());
                processData.putItem(PriamEcService.PRIAM_TASK, priamTask);

                logger.info(this.getClass().getName() + " PIPELINE execute() finish");
            }
            else {
                throw new Exception("Do not recognize setupType=" + setupType);
            }
        }
        catch (Exception e) {
            logger.error("Exception in MetaGenoAnnotationSetupService: " + e.getMessage());
            setParentTaskToErrorStatus(e.getMessage(), processData);
        }
    }

    private void setParentTaskToErrorStatus(String message, IProcessData processData) {
        try {
            MetaGenoAnnotationTask parentTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.saveEvent(parentTask.getObjectId(), Event.ERROR_EVENT, message, new Date());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean checkParentTaskForError(IProcessData processData) {
        try {
            MetaGenoAnnotationTask parentTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            return status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }

    protected static synchronized Long createPeptideFastaFileNode(String user, String name, String description,
                                                                  String sourcePath, Logger logger, String sessionName)
            throws Exception {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        File sourceFile = new File(sourcePath);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
        FastaFileNode ffn = new FastaFileNode(user, null/*Task*/, name, description,
                Node.VISIBILITY_PUBLIC, FastaFileNode.PEPTIDE, (int) sequenceCountAndTotalLength[0], sessionName);
        ffn.setLength(sequenceCountAndTotalLength[1]);
        ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
        File ffnDir = new File(ffn.getDirectoryPath());
        ffnDir.mkdirs();
        String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
        if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
        SystemCall call = new SystemCall(logger);
        int exitVal = call.emulateCommandLine(copyCmd, true);
        if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        return ffn.getObjectId();
    }

    private Long getInputFileNodeIdFromTask(Task task) throws ServiceException, IOException, InterruptedException {
        Long queryNodeId;
        if (task instanceof MetaGenoOrfCallerTask) {
            queryNodeId = Long.parseLong(task.getParameter(MetaGenoOrfCallerTask.PARAM_input_node_id));
        }
        else if (task instanceof MetaGenoAnnotationTask) {
            queryNodeId = Long.parseLong(task.getParameter(MetaGenoAnnotationTask.PARAM_input_node_id));
        }
        else {
            throw new ServiceException("Do not recognize task type=" + task.getDisplayName());
        }
        return queryNodeId;
    }

    /**
     * Returns the original fasta file created by FileUploadController (either through upload or query entry on GUI)
     *
     * @param inputNodeId
     * @return
     * @throws ServiceException
     */
    private File getFastaFile(Long inputNodeId) throws ServiceException {
        ComputeDAO computeDAO = new ComputeDAO(logger);
        FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
        if (inputNode == null) {
            logger.info("FastaFileNode with inputNodeId:" + inputNodeId + " does not exist");
            return null;
        }
        else {
            return new File(inputNode.getFastaFilePath());
        }
    }

}
