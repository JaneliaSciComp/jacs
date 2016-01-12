package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;

import java.util.Date;

/**
 * Updates the status of a sample.
 */
public class LSMSampleProcessingStatusService extends AbstractEntityService {

    public void execute() throws Exception {
        String sampleId = processData.getString("SAMPLE_ENTITY_ID");
        String sampleStatus = processData.getString("PROCESSING_STATUS");

        Entity sampleEntity = entityBean.getEntityById(sampleId);
        entityBean.setOrUpdateValue(sampleEntity.getId(), EntityConstants.ATTRIBUTE_STATUS, sampleStatus);
        Task currentTask = computeBean.getTaskById(task.getObjectId());
        currentTask.addEvent(new Event("Update status for " + sampleId + " to " + sampleStatus,
                new Date(),
                Event.RUNNING_EVENT));
        task = computeBean.saveOrUpdateTask(currentTask);
        processData.putItem(IProcessData.TASK, task); // update the task
    }

}
