package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.validation.ValidationTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.util.ArrayList;
import java.util.HashSet;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Beginnings of a validation launcher.
 *
 * Created by fosterl on 6/17/14.
 */
@Singleton
@Startup
@Remote(ValidatorMBean.class)
public class Validator extends AbstractComponentMBean implements ValidatorMBean {
    private Logger log = Logger.getLogger(Validator.class);
    public transient static final String PROCESS_NAME = "ValidationServicePipeline";
    public transient static final String DEFAULT_DISPLAY_NAME = "Sample Content Validation";

    public Validator() {
        super("jacs");
    }

    @Override
    public void runValidations(String user, Long guid, String label, String types, Boolean nodebug) {
        runChildValidations(null, user, guid, label, types, nodebug);
    }

    /** 
    * This has a different name from runValidations() above, to avoid confusion at process-file.
     * its "parentTaskGuid", however, may be null, in which case, the created task is not a child of any other task.
     */
    public void runChildValidations(Long parentTaskGuid, String user, Long guid, String label, String types, Boolean nodebug) {
        try {

            HashSet<TaskParameter> taskParameters = new HashSet<>();
            taskParameters.add(new TaskParameter("guid", guid == null ? null : guid.toString(), null));
            taskParameters.add(new TaskParameter("label", label, null));
            taskParameters.add(new TaskParameter("types", types, null));
            taskParameters.add(new TaskParameter("nodebug", nodebug.toString(), null));

            String displayName = DEFAULT_DISPLAY_NAME;
            if ( label != null ) {
                displayName = label
                        .replaceAll("'", "_")
                        .replaceAll("\"", "_")
                        .replaceAll("/", "_")
                        .replaceAll("\\\\", "_")
                        .replaceAll(":", "_")
                        .replaceAll(";", "_")
                        .replaceAll(",", "_")
                        .replaceAll(" ", "_")
                ;
            }

            // Create the task and run it.
            ValidationTask task = new ValidationTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, PROCESS_NAME, displayName);

            if ( parentTaskGuid != null ) {
                task.setParentTaskId( parentTaskGuid );
            }
            saveAndRunTask(task);

        } catch ( Exception ex ) {
            log.error("Failed to complete validation", ex);
        }
    }

    // Borrowed from SampleDataManager
    private void saveAndRunTask(ValidationTask task) throws Exception {
        task = (ValidationTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
        EJBFactory.getLocalComputeBean().submitJob(task.getTaskName(), task.getObjectId());
    }

}
