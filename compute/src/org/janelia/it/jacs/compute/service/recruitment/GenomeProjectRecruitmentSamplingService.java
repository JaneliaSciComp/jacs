
package org.janelia.it.jacs.compute.service.recruitment;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.recruitment.GenomeProjectRecruitmentSamplingTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentSamplingBlastDatabaseBuilderTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentSamplingDatabaseFileNode;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Feb 25, 2008
 * Time: 2:45:37 PM
 */
public class GenomeProjectRecruitmentSamplingService implements IService {
    private GenomeProjectRecruitmentSamplingTask task;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            // Prep for execution
            ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (GenomeProjectRecruitmentSamplingTask) ProcessDataHelper.getTask(processData);

            // NOTE: Anything obtained from ComputeBean will not be able to get lazy loaded atts!!!!!
            String blastDBCommaList = task.getParameter(GenomeProjectRecruitmentSamplingTask.BLASTABLE_DATABASE_NODES);
            List<String> newBlastDBList = Task.listOfStringsFromCsvString(blastDBCommaList);

            // Make sure the db nodes are sampling blast db's
            RecruitmentSamplingBlastDatabaseBuilderTask builderTask = new RecruitmentSamplingBlastDatabaseBuilderTask(null,
                    task.getOwner(), null, null, Task.csvStringFromCollection(newBlastDBList), task.getJobName(), task.getJobName());
            builderTask.setJobName(task.getJobName());
            builderTask.setParentTaskId(task.getObjectId());
            builderTask = (RecruitmentSamplingBlastDatabaseBuilderTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(builderTask);
            EJBFactory.getLocalComputeBean().submitJob("FRVSamplingBlastDatabaseBuilder", builderTask.getObjectId());
            String builderStatus = waitAndVerifyCompletion(builderTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(builderStatus)) {
                System.out.println("\n\n\nERROR: the blast sampling db builder job has not actually completed!\nStatus is " + builderStatus);
                throw new ServiceException("FRVSamplingBlastDatabaseBuilder did not complete successfully.");
            }
            // Now replace the original regular db list with the sampling db id
            RecruitmentSamplingDatabaseFileNode samplingNode = (RecruitmentSamplingDatabaseFileNode) EJBFactory.getLocalComputeBean().getResultNodeByTaskId(builderTask.getObjectId());
            newBlastDBList.clear();
            newBlastDBList.add(samplingNode.getObjectId().toString());

            // FASTA file node for ALL NCBI complete genomes
            FastaFileNode fastaFileNode = (FastaFileNode) EJBFactory.getRemoteComputeBean().
                    getNodeById(SystemConfigurationProperties.getLong("Recruitment.GenomeProjectFastaFileNode"));
            Subject tmpOwner = EJBFactory.getRemoteComputeBean().getSubjectByNameOrKey(task.getOwner());

            // STEP 1: Work on the blast side of things
            // Run BlastN using the old query node and new subject db's
            BlastNTask blastNTask = new BlastNTask();
            blastNTask.setParameter(BlastNTask.PARAM_query, fastaFileNode.getObjectId().toString());
            // todo Need an explicit check for all the db node's existence in db and filestore!!!!!!!!!!!!!!!
            blastNTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(newBlastDBList));
            blastNTask.setParameter(BlastNTask.PARAM_databaseAlignments, "10000");
            blastNTask.setParameter(BlastNTask.PARAM_lowerCaseFiltering, "true");
            blastNTask.setParameter(BlastNTask.PARAM_evalue, "-4");
            blastNTask.setParameter(BlastNTask.PARAM_mismatchPenalty, "-5");
            // NOTE: The databaseSize is calculated on-the-fly and not on any number passed to the task
            //blastNTask.setParameter(BlastNTask.PARAM_databaseSize, "3000000000");
            blastNTask.setParameter(BlastNTask.PARAM_databaseDescriptions, "5");
            blastNTask.setParameter(BlastNTask.PARAM_gappedAlignmentDropoff, "30");
            blastNTask.setParameter(BlastNTask.PARAM_matchReward, "4");
            blastNTask.setParameter(BlastNTask.PARAM_filter, "m L");

            blastNTask.setOwner(tmpOwner.getName());
            blastNTask.setParentTaskId(task.getObjectId());
            blastNTask.setParameter(Task.PARAM_project, task.getParameter(Task.PARAM_project));
            blastNTask = (BlastNTask) EJBFactory.getRemoteComputeBean().saveOrUpdateTask(blastNTask);
            // SUBMITTING BLAST SAMPLING JOB
            EJBFactory.getRemoteComputeBean().saveEvent(task.getObjectId(), "Running Blast", "Running Blast", new Date());
            EJBFactory.getRemoteComputeBean().submitJob("FRVSamplingBlast", blastNTask.getObjectId());
            String status = waitAndVerifyCompletion(blastNTask.getObjectId());
            if (!Event.COMPLETED_EVENT.equals(status)) {
                System.out.println("\n\n\nERROR: the blast job has not actually completed!\nStatus is " + status);
                throw new ServiceException("FRVSamplingBlast did not complete successfully.");
            }
            // The next steps use the blast results and expect them as the input nodes.
            BlastResultFileNode blastOutputNode = EJBFactory.getRemoteComputeBean().getBlastResultFileNodeByTaskId(blastNTask.getObjectId());
            processData.putItem(ProcessDataConstants.INPUT_FILE_NODE_ID, blastOutputNode.getObjectId());
        }
        catch (Exception e) {
            System.out.println("\n\n\nError updating the Blast-FRV and the recruitment data for task " + task.getObjectId() + "\nERROR:" + e.getMessage());
            throw new ServiceException(e);
        }
    }


    private String waitAndVerifyCompletion(Long taskId) throws Exception {
        String[] statusTypeAndValue = EJBFactory.getRemoteComputeBean().getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = EJBFactory.getRemoteComputeBean().getTaskStatus(taskId);
        }
        System.out.println(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }


}