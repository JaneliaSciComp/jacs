
package org.janelia.it.jacs.compute.service.recruitment;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectImportTask;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectBlastFrvUpdateTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 25, 2008
 * Time: 2:45:37 PM
 */
public class GenomeProjectBlastFrvUpdateService implements IService {
    private Task task;
    private Logger logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            // Prep for execution
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);

            // Get the values from this task and the old tasks and nodes.
            // NOTE: Anything obtained from ComputeBean will not be able to get lazy loaded atts!!!!!
            String gpNodeId = task.getParameter(GenomeProjectBlastFrvUpdateTask.GENOME_PROJECT_NODE_ID);
            String genbankFileName = task.getParameter(GenomeProjectBlastFrvUpdateTask.GENBANK_FILE_NAME);
            String newBlastDBCommaList = task.getParameter(GenomeProjectBlastFrvUpdateTask.NEW_BLASTABLE_DATABASE_NODES);
            List<String> newBlastDBList = Task.listOfStringsFromCsvString(newBlastDBCommaList);
            String oldFRVFilterTaskID = task.getParameter(GenomeProjectBlastFrvUpdateTask.PREVIOUS_FRV_FILTER_TASK_ID);
            // This node needs to be the anchor for data in this process.
            RecruitmentFileNode oldRecruitmentFileNode = (RecruitmentFileNode) EJBFactory.getRemoteComputeBean().getInputNodeForTask(Long.valueOf(oldFRVFilterTaskID));
            RecruitmentViewerRecruitmentTask oldRecruitmentTask = (RecruitmentViewerRecruitmentTask) oldRecruitmentFileNode.getTask();
            Long oldRecruitmentTaskId = oldRecruitmentTask.getObjectId();
            List<String> oldBlastDBList = Task.listOfStringsFromCsvString(oldRecruitmentTask.getParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS));
            BlastResultFileNode oldBlastResultNode = (BlastResultFileNode) EJBFactory.getRemoteComputeBean().getInputNodeForTask(Long.valueOf(oldRecruitmentTask.getObjectId()));
            BlastTask oldBlastTask = (BlastTask) oldBlastResultNode.getTask();
            String oldFastaFileNodeId = oldBlastTask.getParameter(BlastTask.PARAM_query);
            FastaFileNode oldFastaFileNode = (FastaFileNode) EJBFactory.getRemoteComputeBean().getNodeById(Long.valueOf(oldFastaFileNodeId));

            GenomeProjectFileNode gpFileNode = (GenomeProjectFileNode) EJBFactory.getRemoteComputeBean().getNodeById(Long.valueOf(gpNodeId));
            GenomeProjectImportTask gpImportTask = (GenomeProjectImportTask) EJBFactory.getRemoteComputeBean().getTaskForNodeId(gpFileNode.getObjectId());

            Subject tmpOwner = EJBFactory.getRemoteComputeBean().getSubjectByNameOrKey(task.getOwner());
            File genbankFile = new File(gpFileNode.getDirectoryPath() + File.separator + genbankFileName);

            // STEP 0: Check the NEW_BLASTABLE_DATABASE_NODES against ones already recruited for this organism
            ArrayList<String> finalBlastDBCommaList = new ArrayList<String>();
            for (String o : newBlastDBList) {
                if (!oldBlastDBList.contains(o)) {
                    finalBlastDBCommaList.add(o);
                }
                else {
                    logger.debug("RecruitmentFileNode " + oldRecruitmentFileNode.getObjectId() + " has already blasted and added reads from blast database " + o + ". Ignoring...");
                }
            }
            // If there is nothing new to execute, then leave
            if (null == finalBlastDBCommaList || 0 >= finalBlastDBCommaList.size()) {
                logger.debug("There is nothing new to Blast/Recruitment here.  Leaving pipeline...");
                return;
            }

            // If null, recreate the fasta file in the filestore
            String gpName = gpImportTask.getParameter(GenomeProjectImportTask.PARAM_GENOME_PROJECT_NAME);
            if (null == oldFastaFileNode || !(new File(oldFastaFileNode.getFastaFilePath()).exists())) {
                GenbankFile gbFile = new GenbankFile(genbankFile.getAbsolutePath());
                logger.debug("Getting the sequence for Genome Project: " + gpName);
                String tmpSequence = gbFile.getFastaFormattedSequence().toUpperCase();
                if (null != tmpSequence && tmpSequence.length() >= 1000) {
                    logger.debug("Output: (" + tmpSequence.length() + ")\n" + tmpSequence.substring(0, 1000));
                }
                else {
                    logger.debug("\n\nWARNING: Blast-FRV sequence for " + gpName + " does not exist or is less than 1000 bps!!!\n\n");
                }

                // Make a FASTA file for blasting
                oldFastaFileNode = new FastaFileNode(task.getOwner(),
                        null/*Task*/, genbankFile.getName(), genbankFile.getName(),
                        Node.VISIBILITY_INACTIVE, FastaFileNode.NUCLEOTIDE, 1, null);
                oldFastaFileNode.setLength((long) tmpSequence.length());
                oldFastaFileNode = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(oldFastaFileNode);
                FileUtil.ensureDirExists(oldFastaFileNode.getDirectoryPath());
                oldBlastTask.setParameter(BlastTask.PARAM_query, oldFastaFileNode.getObjectId().toString());
                // Verify the save
                if (null == oldFastaFileNode || null == oldFastaFileNode.getObjectId()) {
                    logger.debug("The FastaFileNode was not properly saved! Ensure computeserver.ejb.service property is correct!");
                }
                File ffnDir = new File(oldFastaFileNode.getDirectoryPath());
                ffnDir.mkdirs();
                FileWriter fos = new FileWriter(oldFastaFileNode.getDirectoryPath() + File.separator + FastaFileNode.NUCLEOTIDE
                        + "." + FastaFileNode.TAG_FASTA);
                fos.append(">").append(genbankFile.getName()).append("\n");
                fos.append(tmpSequence).append("\n");
                fos.close();
            }

            // STEP 1: Work on the blast side of things
            // Run BlastN using the old query node and new subject db's
            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setParameter(BlastNTask.PARAM_query, oldFastaFileNode.getObjectId().toString());
            // todo Need an explicit check for all the db node's existence in db and filestore!!!!!!!!!!!!!!!
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(finalBlastDBCommaList));
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

            blastNTask.setOwner(tmpOwner.getName());
            blastNTask.setParentTaskId(task.getObjectId());
            blastNTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            blastNTask = (BlastNTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(blastNTask);
            // SUBMITTING BLAST JOB
            EJBFactory.getRemoteComputeBean().submitJob("FRVBlast", blastNTask.getObjectId());
            String status = waitAndVerifyCompletion(blastNTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                logger.error("\n\n\nERROR: the blast job has not actually completed!\nStatus is " + status);
                throw new ServiceException("Error running the GenomeProjectBlastFrvUpdateService FRVBlast");
            }

            // STEP 2: Recruit data from the blast results
            BlastResultFileNode blastOutputNode = EJBFactory.getRemoteComputeBean().getBlastResultFileNodeByTaskId(blastNTask.getObjectId());
            HashSet<BlastResultFileNode> rvRtInputNodes = new HashSet<BlastResultFileNode>();
            rvRtInputNodes.add(blastOutputNode);
            RecruitmentViewerRecruitmentTask recruitmentTask = new RecruitmentViewerRecruitmentTask(gpFileNode.getObjectId().toString(),
                    genbankFile.getName(),
                    rvRtInputNodes, tmpOwner.getName(), new ArrayList(), gpImportTask.getTaskParameterSet(),
                    oldRecruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.SUBJECT),
                    oldRecruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.QUERY),
                    Long.valueOf(oldRecruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.GENOME_SIZE)),
                    oldRecruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.GI_NUMBER));
            recruitmentTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            recruitmentTask.setParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS, task.getParameter(GenomeProjectBlastFrvUpdateTask.NEW_BLASTABLE_DATABASE_NODES));
            recruitmentTask.setParentTaskId(task.getObjectId());
            recruitmentTask = (RecruitmentViewerRecruitmentTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(recruitmentTask);
            // SUBMITTING THE RECRUITMENT JOB
            EJBFactory.getRemoteComputeBean().submitJob("FrvDataRecruitment", recruitmentTask.getObjectId());

            // STEP 3: Append the new combined*.hits file to the end of the old combined*.hits file
            // Keep a copy of the original recruitment data for this node/organism
            File originalCombinedHitsFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME);
            File originalBlastCombinedFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.BLAST_COMBINED_FILENAME);
            File backupOriginalCombinedHitsFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME + "." + oldRecruitmentTask.getObjectId());
            File backupOriginalBlastCombinedFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.BLAST_COMBINED_FILENAME + "." + oldRecruitmentTask.getObjectId());
            File newCopiedCombinedHitsFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME + "." + recruitmentTask.getObjectId());
            File newCopiedBlastCombinedFile = new File(oldRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.BLAST_COMBINED_FILENAME + "." + recruitmentTask.getObjectId());
            if (!backupOriginalCombinedHitsFile.exists()) {
                FileUtil.copyFile(originalCombinedHitsFile, backupOriginalCombinedHitsFile);
            }
            if (!backupOriginalBlastCombinedFile.exists()) {
                FileUtil.copyFile(originalBlastCombinedFile, backupOriginalBlastCombinedFile);
            }
            // Copy the new combined*.hits file into the old dir - Get Fresh data as the object transactions could be expired
            RecruitmentFileNode newRecruitmentFileNode = (RecruitmentFileNode) EJBFactory.getRemoteComputeBean().getResultNodeByTaskId(recruitmentTask.getObjectId());
            FileUtil.copyFile(new File(newRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME),
                    newCopiedCombinedHitsFile);
            FileUtil.copyFile(new File(newRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.BLAST_COMBINED_FILENAME),
                    newCopiedBlastCombinedFile);
            logger.debug("Copying and Appending " + newRecruitmentFileNode.getDirectoryPath() + File.separator + RecruitmentFileNode.COMBINED_FILENAME + " to " + originalCombinedHitsFile.getAbsolutePath());
            FileUtil.appendOneFileToAnother(originalCombinedHitsFile, newCopiedCombinedHitsFile);

            // Update the blast db list - Get Fresh data as the object transactions could be expired
            oldBlastDBList.addAll(finalBlastDBCommaList);
            oldRecruitmentTask = (RecruitmentViewerRecruitmentTask) EJBFactory.getRemoteComputeBean().getTaskById(oldRecruitmentTaskId);
            oldRecruitmentTask.setParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS, Task.csvStringFromCollection(oldBlastDBList));
            EJBFactory.getRemoteComputeBean().saveOrUpdateTask(oldRecruitmentTask);

            // STEP 4: Filter the recruited results
            // Need to make sure all known samples are in the re-imaging attempt - Get Fresh data as the object transactions could be expired
            //RecruitmentViewerFilterDataTask oldFilterDataTask = (RecruitmentViewerFilterDataTask) EJBFactory.getRemoteComputeBean().getTaskById(Long.valueOf(oldFRVFilterTaskID));
            EJBFactory.getRemoteComputeBean().setTaskParameter(Long.valueOf(oldFRVFilterTaskID), RecruitmentViewerFilterDataTask.SAMPLE_LIST, Task.csvStringFromCollection(EJBFactory.getRemoteComputeBean().getAllSampleNamesAsList()));
            EJBFactory.getRemoteComputeBean().setTaskParameter(Long.valueOf(oldFRVFilterTaskID), Task.PARAM_project, task.getParameter(Task.PARAM_project));
            EJBFactory.getRemoteComputeBean().addEventToTask(Long.valueOf(oldFRVFilterTaskID), new Event("Combining data and resubmitting filter task", new Date(), Event.RESUBMIT_EVENT));
            // SUBMITTING THE DATA FILTER JOB
            EJBFactory.getRemoteComputeBean().submitJob("FrvResubmitImagesOnlyGrid", Long.valueOf(oldFRVFilterTaskID));
            logger.debug("\nCompleted Blast-FRV Update for " + gpName);
        }
        catch (Exception e) {
            logger.error("\n\n\nError updating the Blast-FRV and the recruitment data for task " + task.getObjectId() + "\nERROR:" + e.getMessage());
            throw new ServiceException(e);
        }
    }


    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        String[] statusTypeAndValue = EJBFactory.getRemoteComputeBean().getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = EJBFactory.getRemoteComputeBean().getTaskStatus(taskId);
        }
        logger.debug(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }


}
