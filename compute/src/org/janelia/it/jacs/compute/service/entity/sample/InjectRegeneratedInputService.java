package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;

/**
 * Take the regenerated input created by the given task and inject it into the given AlignedInputFile.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InjectRegeneratedInputService extends AbstractEntityService {
	
    public void execute() throws Exception {

        AlignmentInputFile alignmentInputFile = (AlignmentInputFile)data.getRequiredItem("ALIGNMENT_INPUT");
        Long taskId = data.getRequiredItemAsLong("TASK_ID");
        Task task = computeBean.getTaskWithMessages(taskId);
        
        if (task.getMessages().isEmpty()) {
            throw new IllegalStateException("No output message found for task: "+taskId);
        }
        
        for(TaskMessage message : task.getMessages()) {
            String filepath = message.getMessage();
            // TODO: This is real hack-y. We should have a better way of communicating the task output. 
            if (filepath.startsWith("/")) {
                alignmentInputFile.setFilepath(filepath);
                contextLogger.info("Updated input filepath for: "+alignmentInputFile);
                break;
            }
        }
    }
}
