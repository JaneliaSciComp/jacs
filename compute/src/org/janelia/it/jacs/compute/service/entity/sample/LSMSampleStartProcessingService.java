package org.janelia.it.jacs.compute.service.entity.sample;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.tasks.utility.LSMProcessingTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Updates the status of a sample.
 */
public class LSMSampleStartProcessingService extends AbstractEntityService {
    private static final String SAMPLE_PROCESSING_JOBNAME = "GSPS_CompleteSamplePipeline";

    public void execute() throws Exception {
        String datasetIdWithsampleId = processData.getString("SAMPLE_DATASET_ID_WITH_ENTITY_ID");
        String sampleId = Iterables.get(Splitter.on(':').split(datasetIdWithsampleId),1);
        Entity sampleEntity = entityBean.getEntityById(sampleId);
        Task processSampleTask = createTask(sampleEntity);
        computeBean.dispatchJob(SAMPLE_PROCESSING_JOBNAME, processSampleTask.getObjectId());
    }

    private Task createTask(Entity sample) throws DaoException {
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
