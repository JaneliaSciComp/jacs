
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectImportTask;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectBlastFrvTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Todd Safford
 */
public class GenomeProjectBlastFrvService implements IService {
    private Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            // Prep for execution
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            ComputeDAO computeDAO = new ComputeDAO(logger);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);

            // Get the values from the task
            String gpNodeId = task.getParameter(GenomeProjectBlastFrvTask.GENOME_PROJECT_NODE_ID);
            String genbankFileName = task.getParameter(GenomeProjectBlastFrvTask.GENBANK_FILE_NAME);
            GenomeProjectFileNode gpFileNode = (GenomeProjectFileNode) computeDAO.getNodeById(Long.valueOf(gpNodeId));
            File genbankFile = new File(gpFileNode.getDirectoryPath() + File.separator + genbankFileName);

            // STEP 1: Work on the blast side of things
            // Get the sequence
            GenomeProjectImportTask gpImportTask = (GenomeProjectImportTask) EJBFactory.getRemoteComputeBean().getTaskForNodeId(gpFileNode.getObjectId());
            String gpName = gpImportTask.getParameter(GenomeProjectImportTask.PARAM_GENOME_PROJECT_NAME);
            logger.debug("Getting the sequence for Genome Project: " + gpName);
            GenbankFile gbFile = new GenbankFile(genbankFile.getAbsolutePath());
//                        GenbankFile gbFile = new GenbankFile("Xruntime-shared/filestore/system/genomeProject/1167236322503426404/NC_002253.gbk");
            String tmpSequence = gbFile.getFastaFormattedSequence().toUpperCase();
            if (null != tmpSequence && tmpSequence.length() >= 1000) {
                logger.debug("Output: (" + tmpSequence.length() + ")\n" + tmpSequence.substring(0, 1000));
            }
            else {
                logger.debug("\n\nWARNING: Blast-FRV sequence for " + gpName + " does not exist or is less than 1000 bps!!!\n\n");
            }

            // Make a FASTA file for blasting
            FastaFileNode ffn = new FastaFileNode(task.getOwner(),
                    null/*Task*/, genbankFile.getName(), genbankFile.getName(),
                    Node.VISIBILITY_INACTIVE, FastaFileNode.NUCLEOTIDE, 1, sessionName);
            // todo Check these for the proper length.  Formatted sequence contains newlines and would throw off the length value!!!
            ffn.setLength((long) tmpSequence.length());
            ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
            // Verify the save
            if (null == ffn || null == ffn.getObjectId()) {
                logger.debug("The FastaFileNode was not properly saved! Ensure computeserver.ejb.service property is correct!");
            }
            File ffnDir = new File(ffn.getDirectoryPath());
            boolean dirSuccess = ffnDir.mkdirs();
            if (!dirSuccess){
                logger.error("Unable to make the fasta directory for task "+task.getObjectId());
            }
            FileWriter fos = new FileWriter(ffn.getDirectoryPath() + File.separator + FastaFileNode.NUCLEOTIDE
                    + "." + FastaFileNode.TAG_FASTA);
            try {
                fos.append(">").append(genbankFile.getName()).append("\n");
                fos.append(tmpSequence).append("\n");
            }
            finally {
                fos.close();
            }
            // As the tmpSequence could be very large, and the session of this transaction very long, and we're done with it,
            // AND we can run lots of this pipeline, flush the memory for it.  If the below calls were in services instead
            // of inline, we would have the scope end clear the memory better.
            tmpSequence = "";
            logger.debug("Sequence is now "+tmpSequence);

            // Run BlastN using the new query node and All Metagenomic Reads subject db
            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setParameter(BlastNTask.PARAM_query, ffn.getObjectId().toString());
            // todo Need an explicit check for this node's existence in db and filestore!!!!!!!!!!!!!!!
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, task.getParameter(GenomeProjectBlastFrvTask.NEW_BLASTABLE_DATABASE_NODES));
            blastNTask.setParameter(BlastNTask.PARAM_databaseAlignments, "10000");
            blastNTask.setParameter(BlastNTask.PARAM_lowerCaseFiltering, "true");
            blastNTask.setParameter(BlastNTask.PARAM_evalue, "-4");
            blastNTask.setParameter(BlastNTask.PARAM_mismatchPenalty, "-5");
            // NOTE: The databaseSize is calculated on-the-fly and not on any number passed to the task
            //blastNTask.setParameter(BlastNTask.PARAM_databaseSize, "3000000000");
            blastNTask.setParameter(BlastNTask.PARAM_databaseDescriptions, "5");
            blastNTask.setParameter(BlastNTask.PARAM_gappedAlignmentDropoff, "150");
            blastNTask.setParameter(BlastNTask.PARAM_matchReward, "4");
            blastNTask.setParameter(BlastNTask.PARAM_filter, "m L");

            blastNTask.setOwner(task.getOwner());
            blastNTask.setParentTaskId(task.getObjectId());
            blastNTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            blastNTask = (BlastNTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(blastNTask);
            // SUBMITTING BLAST JOB - Async operation so wait for task to complete
            EJBFactory.getRemoteComputeBean().submitJob("FRVBlast", blastNTask.getObjectId());
            String status = waitAndVerifyCompletion(blastNTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                logger.error("\n\n\nERROR: the blast job has not actually completed!\nStatus is " + status);
                throw new ServiceException("Error running the GenomeProjectBlastFrvService FRVBlast");
            }

            // STEP 2: Recruit data from the blast results
            BlastResultFileNode blastOutputNode = EJBFactory.getRemoteComputeBean().getBlastResultFileNodeByTaskId(blastNTask.getObjectId());
            HashSet<BlastResultFileNode> rvRtInputNodes = new HashSet<BlastResultFileNode>();
            rvRtInputNodes.add(blastOutputNode);
            RecruitmentViewerRecruitmentTask recruitmentTask = new RecruitmentViewerRecruitmentTask(gpFileNode.getObjectId().toString(),
                    genbankFile.getName(),
                    rvRtInputNodes, task.getOwner(), new ArrayList(), gpImportTask.getTaskParameterSet(),
                    blastOutputNode.getTask().getJobName(),
                    gbFile.getDefinition(), gbFile.getMoleculeLength(), gbFile.getGINumber());
            recruitmentTask.setParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS, task.getParameter(GenomeProjectBlastFrvTask.NEW_BLASTABLE_DATABASE_NODES));
            recruitmentTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            recruitmentTask.setParentTaskId(task.getObjectId());
            recruitmentTask = (RecruitmentViewerRecruitmentTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(recruitmentTask);
            // SUBMITTING THE RECRUITMENT JOB - Synchronous operation so no need for wait() method
            EJBFactory.getRemoteComputeBean().submitJob("FrvDataRecruitment", recruitmentTask.getObjectId());

            // STEP 3: Filter the recruited results
            RecruitmentFileNode recruitmentFileNode = (RecruitmentFileNode) EJBFactory.getRemoteComputeBean().getResultNodeByTaskId(recruitmentTask.getObjectId());
            HashSet<RecruitmentFileNode> filterInputNodes = new HashSet<RecruitmentFileNode>();
            filterInputNodes.add(recruitmentFileNode);
            RecruitmentViewerFilterDataTask filterDataTask = new RecruitmentViewerFilterDataTask(filterInputNodes,
                    task.getOwner(), new ArrayList(),
                    recruitmentTask.getTaskParameterSet(), recruitmentTask.getParameter(RecruitmentViewerTask.SUBJECT),
                    recruitmentTask.getParameter(RecruitmentViewerTask.QUERY), 0l,
                    50, 100, 0.0,
                    Double.parseDouble(recruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.GENOME_SIZE)),
                    EJBFactory.getRemoteComputeBean().getAllSampleNamesAsList(),
                    RecruitmentViewerFilterDataTask.INITIAL_MATE_BITS, null,
                    null,
                    RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE);
            filterDataTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            filterDataTask.setParentTaskId(task.getObjectId());
            filterDataTask = (RecruitmentViewerFilterDataTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(filterDataTask);
            // SUBMITTING THE DATA FILTER JOB - Async operation but just leave anyway after submission
            EJBFactory.getRemoteComputeBean().submitJob("FrvNovelGrid", filterDataTask.getObjectId());
            logger.debug("\nCompleted Blast-FRV for " + gpName);
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }


    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = computeBean.getTaskStatus(taskId);
        }
        logger.debug(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

}