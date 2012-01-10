
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class ProkaryoticAnnotationServiceLoadGenomeDataTask extends Task {
    transient public static final String TASK_NAME = "prokAnnotationServiceLoadGenomeDataTask";
    transient public static final String DISPLAY_NAME = "Prokaryotic Annotation Service Load Genome Data";

    // Parameter Keys
    transient public static final String PARAM_username = "username";
    transient public static final String PARAM_sybasePassword = "Sybase password";
    transient public static final String PARAM_targetDirectory = "targetDirectory";
    // The source dir must contain the location of the files for the service
    transient public static final String PARAM_sourceDirectory = "sourceDirectory";
    transient public static final String PARAM_dateString = "dateString";

    public ProkaryoticAnnotationServiceLoadGenomeDataTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_username, "");
        setParameter(PARAM_sybasePassword, "");
        setParameter(PARAM_targetDirectory, "");
        setParameter(PARAM_sourceDirectory, "");
        setParameter(PARAM_dateString, "");
        this.taskName = TASK_NAME;
    }

    public ProkaryoticAnnotationServiceLoadGenomeDataTask() {
        super();
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_targetDirectory) || key.equals(PARAM_sourceDirectory) || key.equals(PARAM_username) ||
                key.equals(PARAM_dateString)) {
            return new TextParameterVO(value, 500);
        }
        if (key.equals(PARAM_sybasePassword)) {
            return new TextParameterVO("**Hidden**", 500);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}