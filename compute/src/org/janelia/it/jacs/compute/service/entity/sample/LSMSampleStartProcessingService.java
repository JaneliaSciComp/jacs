package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Updates the status of a sample.
 */
public class LSMSampleStartProcessingService extends AbstractEntityService {
    private static final String SAMPLE_PROCESSING_JOBNAME = "GSPS_CompleteSamplePipeline";

    public void execute() throws Exception {
        List<Long> sampleIds = (List<Long>)processData.getItem("SAMPLE_ID");
        logger.info("Creating ProcessSample task for " + sampleIds.size() + " samples.");
        DomainDAL domainDAL = DomainDAL.getInstance();

        // ASSUME-FOR-NOW: owner key is to be used to find the domain objects; key is for data owner.
        List<Sample> samples = domainDAL.getDomainObjects(ownerKey, Sample.class.getSimpleName(), sampleIds);
        for (Sample sample: samples) {
            Task processSampleTask = createTask(sample);
            logger.info("Dispatch sample processing task " + processSampleTask.getObjectId());
            computeBean.dispatchJob(SAMPLE_PROCESSING_JOBNAME, processSampleTask.getObjectId());
        }
    }

    private Task createTask(Sample sample) throws DaoException {
        HashSet<TaskParameter> taskParameters = new HashSet<>();
        taskParameters.add(new TaskParameter("sample entity id", sample.getId().toString(), null));
        Boolean reusePipelineRuns = processData.getBoolean("REUSE_PIPELINE_RUNS");
        Boolean reuseSummary = processData.getBoolean("REUSE_SUMMARY");
        Boolean reuseProcessing = processData.getBoolean("REUSE_PROCESSING");
        Boolean reusePost = processData.getBoolean("REUSE_POST");
        Boolean reuseAlignment = processData.getBoolean("REUSE_ALIGNMENT");
        String runObjectives = processData.getString("RUN_OBJECTIVES");
        if (reusePipelineRuns) {
            taskParameters.add(new TaskParameter("reuse pipeline runs", reusePipelineRuns.toString(), null));
        }
        if (reuseSummary!=null) {
            taskParameters.add(new TaskParameter("reuse summary", reuseSummary.toString(), null));
        }
        if (reuseProcessing!=null) {
            taskParameters.add(new TaskParameter("reuse processing", reuseProcessing.toString(), null));
        }
        if (reusePost!=null) {
            taskParameters.add(new TaskParameter("reuse post", reusePost.toString(), null));
        }
        if (reuseAlignment!=null) {
            taskParameters.add(new TaskParameter("reuse alignment", reuseAlignment.toString(), null));
        }
        if (!StringUtils.isBlank(runObjectives)) {
            taskParameters.add(new TaskParameter("run objectives", runObjectives, null));
        }
        GenericTask processSampleTask = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), new ArrayList<Event>(),
                taskParameters, SAMPLE_PROCESSING_JOBNAME, SAMPLE_PROCESSING_JOBNAME);
        processSampleTask.setParentTaskId(task.getObjectId());
        return computeBean.saveOrUpdateTask(processSampleTask);
    }
}
