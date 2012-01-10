
package org.janelia.it.jacs.compute.service.rnaSeq;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.rnaSeq.CufflinksTask;
import org.janelia.it.jacs.model.tasks.rnaSeq.GtfToPasaIntegrationTask;
import org.janelia.it.jacs.model.tasks.rnaSeq.RnaSeqPipelineTask;
import org.janelia.it.jacs.model.tasks.rnaSeq.TophatTask;
import org.janelia.it.jacs.model.tasks.utility.UploadGtfFileTask;
import org.janelia.it.jacs.model.tasks.utility.UploadSamFileTask;
import org.janelia.it.jacs.model.user_data.GtfFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.rnaSeq.CufflinksResultNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.RnaSeqPipelineResultNode;
import org.janelia.it.jacs.model.user_data.rnaSeq.TophatResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 2, 2010
 * Time: 1:58:41 PM
 */
public class RnaSeqPipelineService implements IService {

    private Logger logger;
    RnaSeqPipelineTask task;
    String sessionName;
    File scratchDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));
    Node readMapperResultNode;
    Node transcriptAssemblerResultNode;
    Node pasaResultNode;
    GtfFileNode transcriptAssemblerGtfOutputNode;
    File resultDir;
    RnaSeqPipelineResultNode resultNode;

    public class RnaSeqProcess {
        String processName;
        Task processTask;

        public RnaSeqProcess(String processName, Task processTask) {
            this.processName = processName;
            this.processTask = processTask;
        }

        public String getProcessName() {
            return processName;
        }

        public Task getProcessTask() {
            return processTask;
        }
    }

    public RnaSeqPipelineService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("RnaSeqPipelineService execute() start");
            init(processData);
            recordTaskEvent("Starting execution");
            resultNode = createResultFileNode();
            recordTaskEvent("Creating read mapper");
            RnaSeqProcess readMapperProcess = createReadMapperProcess();
            recordTaskEvent("Running read mapper");
            logger.info("RnaSeqPipelineService taskId=" + task.getObjectId() + " starting Read Mapper Process taskId=" + readMapperProcess.getProcessTask().getObjectId());
            readMapperResultNode = runProcess(readMapperProcess);
            recordTaskEvent("Creating transcript assembler");
            RnaSeqProcess transcriptAssemblerProcess = createTranscriptAssemblerProcess(readMapperResultNode);
            recordTaskEvent("Running transcript assembler");
            logger.info("RnaSeqPipelineService taskId=" + task.getObjectId() + " starting Transcript Assembly Process taskId=" + transcriptAssemblerProcess.getProcessTask().getObjectId());
            transcriptAssemblerResultNode = runProcess(transcriptAssemblerProcess);
            if (taskIndicatesPasaLoad()) {
                recordTaskEvent("Creating gtf-to-pasa process");
                RnaSeqProcess gtfToPasaProcess = createGtfToPasaProcess();
                recordTaskEvent("Running gtf-to-pasa");
                logger.info("RnaSeqPipelineService taskId=" + task.getObjectId() + " starting GtfToPasta Integration Process taskId=" + gtfToPasaProcess.getProcessTask().getObjectId());
                pasaResultNode = runProcess(gtfToPasaProcess);
            }
            logger.info("RnaSeqPipelineService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    boolean taskIndicatesPasaLoad() {
        String pasaDatabaseName = task.getParameter(RnaSeqPipelineTask.PARAM_pasa_db_name);
        logger.info("taskIndicatesPasaLoad() - evaluating pasaDatabaseName="+pasaDatabaseName);
        if (pasaDatabaseName==null || pasaDatabaseName.trim().length()==0) {
            logger.info("Not proceeding with pasa load - returning false");
            return false;
        }
        logger.info("Proceeding with pasa load - returning true");
        return true;
    }

    /**
     * Initialize the input parameters
     *
     * @param processData params of the task
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             cannot find data required to process
     * @throws java.io.IOException problem accessing file data
     */
    protected void init(IProcessData processData) throws MissingDataException, IOException {
        task = (RnaSeqPipelineTask) ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
    }

    protected ComputeBeanRemote getComputeBean() {
        return EJBFactory.getRemoteComputeBean();
    }

    protected RnaSeqProcess createReadMapperProcess() throws Exception {
        String readMapperType = task.getParameter(RnaSeqPipelineTask.PARAM_read_mapper);
        if (readMapperType.equalsIgnoreCase(RnaSeqPipelineTask.TOPHAT)) {
            TophatTask tophatTask = new TophatTask();
            tophatTask.setParentTaskId(task.getObjectId());
            tophatTask.setOwner(task.getOwner());
            tophatTask.setParameter(TophatTask.PARAM_project, task.getParameter(RnaSeqPipelineTask.PARAM_project));
            tophatTask.setJobName("Read Mapper for " + task.getObjectId());
            tophatTask.setParameter(TophatTask.PARAM_reads_fastQ_node_id, task.getParameter(RnaSeqPipelineTask.PARAM_input_reads_fastQ_node_id));
            tophatTask.setParameter(TophatTask.PARAM_refgenome_fasta_node_id, task.getParameter(RnaSeqPipelineTask.PARAM_input_refgenome_fasta_node_id));
            ComputeBeanRemote computeBean = getComputeBean();
            Task savedTask = computeBean.saveOrUpdateTask(tophatTask);
            return new RnaSeqProcess("Tophat", savedTask);
        }
        else {
            throw new Exception("Do not recognize read-mapper type=" + readMapperType);
        }
    }

    protected RnaSeqProcess createTranscriptAssemblerProcess(Node readMapperResultNode) throws Exception {
        File readSamFile;
        if (readMapperResultNode instanceof TophatResultNode) {
            TophatResultNode tn = (TophatResultNode) readMapperResultNode;
            readSamFile = new File(tn.getFilePathByTag(TophatResultNode.TAG_SAM_OUTPUT));
        }
        else {
            throw new Exception("Do not recognize result node type=" + readMapperResultNode.getClass().getName());
        }
        Long inputSamFileNodeId = createSamFileNodeFromFile(readSamFile);
        String transcriptAssemblerType = task.getParameter(RnaSeqPipelineTask.PARAM_transcript_assembler);
        if (transcriptAssemblerType.equalsIgnoreCase(RnaSeqPipelineTask.CUFFLINKS)) {
            CufflinksTask cufflinksTask = new CufflinksTask();
            cufflinksTask.setParentTaskId(task.getObjectId());
            cufflinksTask.setOwner(task.getOwner());
            cufflinksTask.setParameter(CufflinksTask.PARAM_project, task.getParameter(RnaSeqPipelineTask.PARAM_project));
            cufflinksTask.setJobName("Transcript Assembler for " + task.getObjectId());
            cufflinksTask.setParameter(CufflinksTask.PARAM_sam_file_input_node_id, inputSamFileNodeId.toString());
            cufflinksTask.setParameter(CufflinksTask.PARAM_inner_dist_mean, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_inner_dist_mean));
            cufflinksTask.setParameter(CufflinksTask.PARAM_inner_dist_std_dev, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_inner_dist_std_dev));
            cufflinksTask.setParameter(CufflinksTask.PARAM_collapse_thresh, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_collapse_thresh));
            cufflinksTask.setParameter(CufflinksTask.PARAM_max_intron_length, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_max_intron_length));
            cufflinksTask.setParameter(CufflinksTask.PARAM_min_isoform_fraction, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_min_isoform_fraction));
            cufflinksTask.setParameter(CufflinksTask.PARAM_min_intron_fraction, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_min_intron_fraction));
            cufflinksTask.setParameter(CufflinksTask.PARAM_junc_alpha, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_junc_alpha));
            cufflinksTask.setParameter(CufflinksTask.PARAM_small_anchor_fraction, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_small_anchor_fraction));
            cufflinksTask.setParameter(CufflinksTask.PARAM_min_mapqual, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_min_mapqual));
            cufflinksTask.setParameter(CufflinksTask.PARAM_pre_mrna_fraction, task.getParameter(RnaSeqPipelineTask.PARAM_CUFFLINKS_pre_mrna_fraction));

            ComputeBeanRemote computeBean = getComputeBean();
            Task savedTask = computeBean.saveOrUpdateTask(cufflinksTask);
            return new RnaSeqProcess("Cufflinks", savedTask);
        }
        else {
            throw new Exception("Do not recognize transcript-assembler type=" + transcriptAssemblerType);
        }
    }

    protected Node runProcess(RnaSeqProcess process) throws Exception {
        SubmitJobAndWaitHelper waitHelper = new SubmitJobAndWaitHelper(process.getProcessName(), process.getProcessTask().getObjectId());
        waitHelper.startAndWaitTillDone();
        ComputeBeanRemote computeBean = getComputeBean();
        return computeBean.getResultNodeByTaskId(process.getProcessTask().getObjectId());
    }

    protected Long createSamFileNodeFromFile(File samFile) throws Exception {
        UploadSamFileTask uploadSamFileTask = new UploadSamFileTask();
        logger.info("immediately after creation, uploadSamFileTask task name=" + uploadSamFileTask.getTaskName());
        uploadSamFileTask.setOwner(task.getOwner());
        uploadSamFileTask.setJobName("SamFileTask for RnaSeqPipeline id=" + task.getObjectId());
        uploadSamFileTask.setParentTaskId(task.getObjectId());
        uploadSamFileTask.setParameter(UploadSamFileTask.PARAM_project, task.getParameter(RnaSeqPipelineTask.PARAM_project));
        uploadSamFileTask.setParameter(UploadSamFileTask.PARAM_SOURCE_FILE, samFile.getAbsolutePath());
        ComputeBeanRemote computeBean = getComputeBean();
        logger.info("Before persisting uploadSamFileTask, its task name=" + uploadSamFileTask.getTaskName());
        uploadSamFileTask = (UploadSamFileTask) computeBean.saveOrUpdateTask(uploadSamFileTask);
        SubmitJobAndWaitHelper waitHelper = new SubmitJobAndWaitHelper("UploadSamFile", uploadSamFileTask.getObjectId());
        waitHelper.startAndWaitTillDone();
        Node samFileNode = computeBean.getResultNodeByTaskId(uploadSamFileTask.getObjectId());
        return samFileNode.getObjectId();
    }

    protected GtfFileNode createGtfFileNodeFromFile(File gtfFile) throws Exception {
        UploadGtfFileTask uploadGtfFileTask = new UploadGtfFileTask();
        logger.info("immediately after creation, uploadGtfFileTask task name=" + uploadGtfFileTask.getTaskName());
        uploadGtfFileTask.setOwner(task.getOwner());
        uploadGtfFileTask.setJobName("GtfFileTask for RnaSeqPipeline id=" + task.getObjectId());
        uploadGtfFileTask.setParentTaskId(task.getObjectId());
        uploadGtfFileTask.setParameter(UploadGtfFileTask.PARAM_project, task.getParameter(RnaSeqPipelineTask.PARAM_project));
        uploadGtfFileTask.setParameter(UploadGtfFileTask.PARAM_SOURCE_FILE, gtfFile.getAbsolutePath());
        ComputeBeanRemote computeBean = getComputeBean();
        logger.info("Before persisting uploadGtfFileTask, its task name=" + uploadGtfFileTask.getTaskName());
        uploadGtfFileTask = (UploadGtfFileTask) computeBean.saveOrUpdateTask(uploadGtfFileTask);
        SubmitJobAndWaitHelper waitHelper = new SubmitJobAndWaitHelper("UploadGtfFile", uploadGtfFileTask.getObjectId());
        waitHelper.startAndWaitTillDone();
        return (GtfFileNode) computeBean.getResultNodeByTaskId(uploadGtfFileTask.getObjectId());
    }

    protected RnaSeqProcess createGtfToPasaProcess() throws Exception {
        GtfToPasaIntegrationTask pasaTask = new GtfToPasaIntegrationTask();
        pasaTask.setParentTaskId(task.getObjectId());
        pasaTask.setOwner(task.getOwner());
        pasaTask.setParameter(GtfToPasaIntegrationTask.PARAM_project, task.getParameter(RnaSeqPipelineTask.PARAM_project));
        pasaTask.setJobName("Gtf-to-Pasa Integration for " + task.getObjectId());
        GtfFileNode gtfNode = getTranscriptAssemblerGtfOutput();
        pasaTask.setParameter(GtfToPasaIntegrationTask.PARAM_pasa_database_name, checkPasaDatabaseName(task.getParameter(RnaSeqPipelineTask.PARAM_pasa_db_name)));
        pasaTask.setParameter(GtfToPasaIntegrationTask.PARAM_gtf_node_id, gtfNode.getObjectId().toString());
        pasaTask.setParameter(GtfToPasaIntegrationTask.PARAM_refgenome_fasta_node_id, task.getParameter(RnaSeqPipelineTask.PARAM_input_refgenome_fasta_node_id));
        ComputeBeanRemote computeBean = getComputeBean();
        pasaTask = (GtfToPasaIntegrationTask) computeBean.saveOrUpdateTask(pasaTask);
        return new RnaSeqProcess("GtfToPasaIntegration", pasaTask);
    }

    String checkPasaDatabaseName(String name) {
        if (name!=null && (!name.endsWith("_pasa"))) {
            return name + "_pasa";
        }
        return name;
    }

    protected GtfFileNode getTranscriptAssemblerGtfOutput() throws Exception {
        if (transcriptAssemblerGtfOutputNode != null) {
            return transcriptAssemblerGtfOutputNode;
        }
        else {
            if (transcriptAssemblerResultNode == null) {
                throw new Exception("transcriptAssemblerResultNode unexpected null");
            }
            else {
                if (transcriptAssemblerResultNode instanceof CufflinksResultNode) {
                    CufflinksResultNode cufflinksNode = (CufflinksResultNode) transcriptAssemblerResultNode;
                    logger.info("getTranscriptAssemblerGtfOutput cufflinksNodeId=" + cufflinksNode.getObjectId());
                    String cufflinksNodeFilePath = cufflinksNode.getFilePathByTag(CufflinksResultNode.TAG_GTF_OUTPUT);
                    logger.info("TAG_GTF_OUTPUT file path=" + cufflinksNodeFilePath);
                    File gtfFile = new File(cufflinksNodeFilePath);
                    logger.info("Created gtfFile object with absolutPath=" + gtfFile.getAbsolutePath());
                    if (!gtfFile.exists()) {
                        throw new Exception("Could not find gtf file=" + gtfFile.getAbsolutePath());
                    }
                    else {
                        transcriptAssemblerGtfOutputNode = createGtfFileNodeFromFile(gtfFile);
                        return transcriptAssemblerGtfOutputNode;
                    }
                }
                else {
                    throw new Exception("Do not recognize result node type " + transcriptAssemblerResultNode.getClass().getName());
                }
            }
        }
    }

    private RnaSeqPipelineResultNode createResultFileNode() throws ServiceException, IOException, DaoException {
        RnaSeqPipelineResultNode resultFileNode;

        // Check if we already have a result node for this task
        Set<Node> outputNodes = task.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof RnaSeqPipelineResultNode) {
                return (RnaSeqPipelineResultNode) node;
            }
        }

        // Create new node
        ComputeBeanRemote computeBean = getComputeBean();
        resultFileNode = new RnaSeqPipelineResultNode(task.getOwner(), task,
                "RnaSeqPipelineResultNode", "RnaSeqPipelineResultNode for task " + task.getObjectId(),
                Node.VISIBILITY_PRIVATE, sessionName);
        resultFileNode = (RnaSeqPipelineResultNode) computeBean.saveOrUpdateNode(resultFileNode);

        FileUtil.ensureDirExists(resultFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(resultFileNode.getDirectoryPath());
        resultDir = new File(resultFileNode.getDirectoryPath());
        return resultFileNode;
    }

    private void recordTaskEvent(String eventString) throws Exception {
        ComputeBeanRemote computeBean=getComputeBean();
        computeBean.addEventToTask(task.getObjectId(), new Event(eventString, new Date(), Event.RUNNING_EVENT));
    }

}
