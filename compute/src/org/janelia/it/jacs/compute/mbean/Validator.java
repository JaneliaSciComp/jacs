package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Beginnings of a validation launcher.
 *
 * Created by fosterl on 6/17/14.
 */
public class Validator implements ValidatorMBean {
    private Logger log = Logger.getLogger(Validator.class);
    @Override
    public void runValidations(String user, Long guid, String label, Boolean nodebug) {
        runChildValidations(null, user, guid, label, nodebug);
    }

    /** 
    * This has a different name from runValidations() above, to avoid confusion at process-file.
     * its "parentTaskGuid", however, may be null, in which case, the created task is not a child of any other task.
     */
    public void runChildValidations(Long parentTaskGuid, String user, Long guid, String label, Boolean nodebug) {
        try {
            String processName = "ValidationServicePipeline";
            String displayName = "Sample Content Validation";

            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("guid", guid == null ? null : guid.toString(), null));
            taskParameters.add(new TaskParameter("label", label, null));
            taskParameters.add(new TaskParameter("nodebug", nodebug.toString(), null));

            // Create the task and run it.
            GenericTask task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, processName, displayName);

            if ( parentTaskGuid != null ) {
                task.setParentTaskId( parentTaskGuid );
            }
            saveAndRunTask(task);

        } catch ( Exception ex ) {
            log.error("Failed to complete validation", ex);
        }
    }

    // Borrowed from SampleDataManager
    private void saveAndRunTask(GenericTask task) throws Exception {
        task = (GenericTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
        EJBFactory.getLocalComputeBean().submitJob(task.getTaskName(), task.getObjectId());
    }

}
