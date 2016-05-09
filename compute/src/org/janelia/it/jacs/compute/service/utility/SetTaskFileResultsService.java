package org.janelia.it.jacs.compute.service.utility;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.entity.sample.AnatomicalArea;
import org.janelia.it.jacs.model.tasks.TaskMessage;
import org.janelia.it.jacs.model.user_data.FileNode;

/**
 * Sets the result filepaths as a message on the current Task object.
 * 
 * Input variables:
 *   SAMPLE_AREAS - Sample areas
 *   or
 *   RESULT_FILE_NODE - The result node containing the result files.
 *   OUTPUT_EXTENSION - The extension to match. If null then the root path will be set. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SetTaskFileResultsService extends AbstractDomainService {

	public static final String RESULT_TOKEN = "RESULT!";
	
	protected Logger logger = Logger.getLogger(SetTaskFileResultsService.class);
	
    public void execute() throws Exception {
        
        List<AnatomicalArea> sampleAreas = (List<AnatomicalArea>) processData.getItem("SAMPLE_AREAS");
        if (sampleAreas != null) {
        	StringBuilder sb = new StringBuilder();
            for(AnatomicalArea sampleArea : sampleAreas) {
            	if (sb.length()>0) sb.append(",");
            	sb.append(sampleArea.getName());
            	sb.append("=");
                sb.append(sampleArea.getStitchedFilename());
            }
            setResultMessage(RESULT_TOKEN+sb.toString());
            return;
        }
        
        // TODO: this other path should also use RESULT_TOKEN 
        // Better yet, we should create a formal mechanism for passing results between tasks
        
        FileNode finalOutputNode = (FileNode)data.getRequiredItem("RESULT_FILE_NODE");
        
        final String extension = data.getItemAsString("OUTPUT_EXTENSION");
        if (extension==null) {
            setResultMessage(finalOutputNode.getDirectoryPath());
            return;
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
            
            setResultMessage(message.toString());
        }
        else {
            throw new MissingDataException("Output files not found");
        }
    }
    
    private void setResultMessage(String message) throws Exception {
    	contextLogger.info("Setting result message: "+message);
        Set<TaskMessage> messages = new HashSet<TaskMessage>();
        messages.add(new TaskMessage(null, message));
        computeBean.saveTaskMessages(task.getObjectId(), messages);
    }
    
}
