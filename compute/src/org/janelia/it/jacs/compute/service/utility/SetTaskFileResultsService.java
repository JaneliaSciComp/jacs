package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Sets the result filepaths as a message on the current Task object.
 * 
 * Input variables:
 *   RESULT_FILE_NODE - The result node containing the result tif files
 *   OUTPUT_EXTENSION - The extension to match 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetTaskFileResultsService extends AbstractEntityService {

	protected Logger logger = Logger.getLogger(SetTaskFileResultsService.class);
	
    public void execute() throws Exception {
        
        FileNode finalOutputNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
        if (finalOutputNode==null) {
            throw new IllegalArgumentException("RESULT_FILE_NODE cannot be null");
        }
        
        final String extension = (String)processData.getItem("OUTPUT_EXTENSION");
        if (extension==null) {
            throw new IllegalArgumentException("OUTPUT_EXTENSION cannot be null");
        }
        
        File outputDir = new File(finalOutputNode.getDirectoryPath());
        File[] outputFiles = outputDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(extension);
            }
        });

        if (outputFiles!=null && outputFiles.length>0) {
            StringBuffer message = new StringBuffer(outputDir.getAbsolutePath());
            message.append(":");
            
            int i = 0;
            for(File file : outputFiles) {
                if (i++>0) message.append(",");
                message.append(file.getName());
            }
        
            Set<TaskMessage> messages = new HashSet<TaskMessage>();
            messages.add(new TaskMessage(null, message.toString()));
            computeBean.saveTaskMessages(task.getObjectId(), messages);
        }
        else {
            throw new MissingDataException("Output files not found");
        }
    }
}
