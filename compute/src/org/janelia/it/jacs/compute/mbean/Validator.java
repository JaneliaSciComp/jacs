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
    public void runValidations(String user, Long guid, Boolean nodebug) {
        try {
            String processName = "ValidationServicePipeline";
            String displayName = "Sample Content Validation";
            //NO PARAMS YET
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("guid", guid.toString(), null));
            taskParameters.add(new TaskParameter("nodebug", nodebug.toString(), null));
            saveAndRunTask(user, processName, displayName, taskParameters);
        } catch ( Exception ex ) {
            log.error("Failed to complete validation", ex);
        }
    }

    // Borrowed from SampleDataManager
    private void saveAndRunTask(String user, String processName, String displayName, HashSet<TaskParameter> parameters) throws Exception {
        GenericTask task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                parameters, processName, displayName);
        saveAndRunTask(task);
    }

    // Borrowed from SampleDataManager
    private void saveAndRunTask(GenericTask task) throws Exception {
        task = (GenericTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
        EJBFactory.getLocalComputeBean().submitJob(task.getTaskName(), task.getObjectId());
    }

}
