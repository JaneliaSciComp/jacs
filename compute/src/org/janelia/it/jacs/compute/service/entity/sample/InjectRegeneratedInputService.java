package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.List;

import org.janelia.it.jacs.compute.service.align.AlignmentInputFile;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.utility.SetTaskFileResultsService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskMessage;

/**
 * Take the regenerated input created by the given task and inject it into the given AlignedInputFile.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class InjectRegeneratedInputService extends AbstractEntityService {
	
    public void execute() throws Exception {

    	List<AlignmentInputFile> alignmentInputFiles = (List<AlignmentInputFile>)data.getRequiredItem("ALIGNMENT_INPUTS");
        Long taskId = data.getRequiredItemAsLong("TASK_ID");
        Task task = computeBean.getTaskWithMessages(taskId);
        
        if (task.getMessages().isEmpty()) {
            throw new IllegalStateException("No output message found for task: "+taskId);
        }
        
        for(TaskMessage message : task.getMessages()) {
            String text = message.getMessage();
            // TODO: This is real hack-y. We should have a better way of communicating the task output. 
            if (text.startsWith(SetTaskFileResultsService.RESULT_TOKEN)) {

                contextLogger.info("Got result message: "+text);
            	for(String result : Task.listOfStringsFromCsvString(text.replace(SetTaskFileResultsService.RESULT_TOKEN, ""))) {
            		String[] resultArr = result.split(":");
            		String objective = resultArr[0];
            		String area = resultArr[1];
            		String filepath = resultArr[2];
                	for(AlignmentInputFile alignmentInputFile : alignmentInputFiles) {
                		if (objective.equals(alignmentInputFile.getObjective()) && area.equals(alignmentInputFile.getArea())) {
                            alignmentInputFile.setFilepath(filepath);
                            contextLogger.info("Updated input filepath for: "+alignmentInputFile);
                		}
                	}
            	}
            }
        }
        
		for (AlignmentInputFile alignmentInputFile : alignmentInputFiles) {
			if (alignmentInputFile.getFilepath().endsWith("h5j")) {
				contextLogger.warn("Path for " + alignmentInputFile.getArea() + " was not regenerated: " + alignmentInputFile.getFilepath());
			}
		}

    }
}
