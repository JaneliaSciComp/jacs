
package org.janelia.it.jacs.compute.service.recruitment;

import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentDataFastaBuilderTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.shared.fasta.FASTAFileTokenizer;
import org.janelia.it.jacs.shared.node.FastaUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Todd Safford
 */
public class UserBlastFrvGridService implements IService {

    private Task task;

    public void execute(IProcessData processData) throws ServiceException {
        // Get the values from the task
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();

        try {
            // Prep for execution
            ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = ProcessDataHelper.getTask(processData);
            String sessionName = ProcessDataHelper.getSessionRelativePath(processData);

            // STEP 1: Work on the blast side of things
            // Get the user sequence
            Set<Node> inputNodes = task.getInputNodes();
            Node inputNode;
            if (null != inputNodes && null != inputNodes.iterator() && inputNodes.iterator().hasNext()) {
                inputNode = inputNodes.iterator().next();
            }
            else {
                throw new ServiceException("Do not have user sequence for Blast-Frv.  Task=" + task.getObjectId());
            }

            // Grab the input FASTA.
            StringBuffer tmpSequence = new StringBuffer();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i <= 1000; i++) {
                builder.append('N');
            }
            String gaps = builder.toString();
            if (inputNode instanceof FastaFileNode) {
                FastaFileNode tmpNode = (FastaFileNode) inputNode;
                if (!SequenceType.NUCLEOTIDE.equalsIgnoreCase(tmpNode.getSequenceType())) {
                    throw new ServiceException("Do not have valid user sequence type for Blast-Frv.  Task=" + task.getObjectId());
                }
                FileInputStream fis = new FileInputStream(new File(tmpNode.getFastaFilePath()));
                FASTAFileTokenizer tok = new FASTAFileTokenizer(fis.getChannel());
                // Prime the pump
                tok.nextFASTAEntry(true);
                String tmpEntrySeq = tok.getFastaEntry();
                long numEntries = 0;
                long seqCount = 0;
                while (null != tmpEntrySeq && !"".equals(tmpEntrySeq)) {
                    // Ignore the defline
                    String sequenceText = tmpEntrySeq.substring(tmpEntrySeq.indexOf("\n"));
                    // Get rid of all the spaces.
                    sequenceText = sequenceText.replace("\n", "");
                    sequenceText = sequenceText.replace(" ", "");
                    sequenceText = sequenceText.trim();

                    tmpSequence.append(sequenceText);
                    numEntries++;
                    seqCount += sequenceText.length();
                    tok.nextFASTAEntry(true);
                    tmpEntrySeq = tok.getFastaEntry();
                    if (null != tmpEntrySeq && !"".equals(tmpEntrySeq)) {
                        tmpSequence.append(gaps);
                    }
                }
                System.out.println("FastaFileNode: Parsed " + numEntries + " entries for a total sequence count of " + seqCount + ":" + tmpSequence.length());
            }
            // Format the string; includes the defline
            // Actually, defline really means accession.
            // NOTE: FRV code assumes a single string on the defline!!!!
            String finalSeq = FastaUtil.formatFasta("FRVBlast", tmpSequence.toString(), 60);

            // Make a FASTA file for blasting
            FastaFileNode ffn = new FastaFileNode(task.getOwner(),
                    null/*Task*/, inputNode.getName(), inputNode.getName(),
                    Node.VISIBILITY_PRIVATE_DEPRECATED, FastaFileNode.NUCLEOTIDE, 1, sessionName);
            ffn.setLength((long) tmpSequence.length());
            ffn = (FastaFileNode) computeBean.saveOrUpdateNode(ffn);
            // Verify the save
            if (null == ffn || null == ffn.getObjectId()) {
                System.out.println("The FastaFileNode was not properly saved! Ensure computeserver.ejb.service property is correct!");
            }
            File ffnDir = new File(ffn.getDirectoryPath());
            boolean dirsCreated = ffnDir.mkdirs();
            if (!dirsCreated){
                throw new ServiceException("Unable to create dirs for the UserBlastFrvGridService");
            }
            FileWriter fos = new FileWriter(ffn.getDirectoryPath() + File.separator + FastaFileNode.NUCLEOTIDE
                    + "." + FastaFileNode.TAG_FASTA);
            fos.append(finalSeq);
            fos.close();

            // Run BlastN using the new query node and All Metagenomic Reads subject db
            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            blastNTask.setJobName("FRV Blast of " + ffn.getName());
            blastNTask.setParameter(BlastNTask.PARAM_query, ffn.getObjectId().toString());
            blastNTask.setParentTaskId(task.getObjectId());
            // todo Need an explicit check for this node's existence in db and filestore!!!!!!!!!!!!!!!
            // Get the id for the node, name="All Metagenomic Sequence Reads (N)" subject db "1054893807616655712"
            // todo Want to be able to set blast database as a parameter!
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, "1054893807616655712");
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
            blastNTask = (BlastNTask) computeBean.saveOrUpdateTask(blastNTask);
            // SUBMITTING BLAST JOB - Async operation so wait for task to complete
            computeBean.saveEvent(task.getObjectId(), Event.RUNNING_EVENT, Event.RUNNING_EVENT, new Date());
            computeBean.submitJob("FRVBlast", blastNTask.getObjectId());
            String status = waitAndVerifyCompletion(blastNTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the blast job has not actually completed!\nStatus is " + status);
                throw new ServiceException("Error running the UserBlastFrvGridService FRVBlast");
            }

            // STEP 2: Recruit data from the blast results
            BlastResultFileNode blastOutputNode = computeBean.getBlastResultFileNodeByTaskId(blastNTask.getObjectId());
            HashSet<BlastResultFileNode> rvRtInputNodes = new HashSet<BlastResultFileNode>();
            rvRtInputNodes.add(blastOutputNode);
            RecruitmentViewerRecruitmentTask recruitmentTask = new RecruitmentViewerRecruitmentTask(null,
                    null,
                    rvRtInputNodes, task.getOwner(), new ArrayList(), null, "All Metagenomic Sequence Reads (N)",
                    inputNode.getName(), (long) tmpSequence.length(), null);
            recruitmentTask.setParameter(RecruitmentViewerTask.BLAST_DATABASE_IDS, "1054893807616655712");
            recruitmentTask.setParentTaskId(task.getObjectId());
            recruitmentTask = (RecruitmentViewerRecruitmentTask) computeBean.saveOrUpdateTask(recruitmentTask);
            // SUBMITTING THE RECRUITMENT JOB - Synchronous operation so no need for wait() method
            computeBean.saveEvent(task.getObjectId(), Event.RECRUITING_EVENT, Event.RECRUITING_EVENT, new Date());
            computeBean.submitJob("FrvDataRecruitment", recruitmentTask.getObjectId());

            // STEP 3: Filter the recruited results
            RecruitmentFileNode recruitmentFileNode = (RecruitmentFileNode) computeBean.getResultNodeByTaskId(recruitmentTask.getObjectId());
            HashSet<RecruitmentFileNode> filterInputNodes = new HashSet<RecruitmentFileNode>();
            filterInputNodes.add(recruitmentFileNode);
            RecruitmentViewerFilterDataTask filterDataTask = new RecruitmentViewerFilterDataTask(filterInputNodes,
                    task.getOwner(), new ArrayList(),
                    recruitmentTask.getTaskParameterSet(), recruitmentTask.getParameter(RecruitmentViewerTask.SUBJECT),
                    recruitmentTask.getParameter(RecruitmentViewerTask.QUERY), 0l,
                    50, 100, 0.0,
                    Double.parseDouble(recruitmentTask.getParameter(RecruitmentViewerRecruitmentTask.GENOME_SIZE)),
                    computeBean.getAllSampleNamesAsList(),
                    RecruitmentViewerFilterDataTask.INITIAL_MATE_BITS, null,
                    null,
                    RecruitmentViewerFilterDataTask.COLORIZATION_SAMPLE);
            filterDataTask.setParentTaskId(task.getObjectId());
            filterDataTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            filterDataTask = (RecruitmentViewerFilterDataTask) computeBean.saveOrUpdateTask(filterDataTask);
            // SUBMITTING THE DATA FILTER JOB - Async operation but just leave anyway after submission
            computeBean.saveEvent(task.getObjectId(), Event.GENERATING_IMAGES_EVENT, Event.GENERATING_IMAGES_EVENT, new Date());
            computeBean.submitJob("FrvNovelGrid", filterDataTask.getObjectId());
            String imageStatus = waitAndVerifyCompletion(filterDataTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(imageStatus)) {
                System.out.println("\n\n\nERROR: the filter job has not actually completed!\nStatus is " + status);
                throw new ServiceException("Error running the UserBlastFrvGridService FrvNovelGrid");
            }

            // STEP 4: Building the fasta file - this speeds up export of reads
            RecruitmentResultFileNode rrfn = (RecruitmentResultFileNode) computeBean.getResultNodeByTaskId(filterDataTask.getObjectId());
            RecruitmentDataFastaBuilderTask fastaTask = new RecruitmentDataFastaBuilderTask(rrfn.getObjectId().toString());
            fastaTask.setOwner(task.getOwner());
            fastaTask = (RecruitmentDataFastaBuilderTask) computeBean.saveOrUpdateTask(fastaTask);
            computeBean.saveEvent(task.getObjectId(), Event.FASTA_GENERATION_EVENT, Event.FASTA_GENERATION_EVENT, new Date());
            computeBean.submitJob("FrvDataFastaNonGrid", fastaTask.getObjectId());

        }
        catch (Exception e) {
            System.out.println("\n\n\nError generating the FRV data for user " + task.getOwner() + ", task=" + task.getObjectId() + "\nERROR:" + e.getMessage());
            // Try to record the error
            try {
                computeBean.saveEvent(task.getObjectId(), Event.ERROR_EVENT, "Error executing the FRV pipeline", new Date());
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
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
        System.out.println(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

}