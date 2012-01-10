
package org.janelia.it.jacs.model.tasks.annotation;

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
public class AnnotationSessionTask extends Task {
    transient public static final String TASK_NAME      = "annotSession";
    transient public static final String DISPLAY_NAME   = "Annotation Session";

    // Parameter Keys
    transient public static final String PARAM_sessionName = "session name";
    transient public static final String PARAM_annotationCategories = "annotation categories";
    transient public static final String PARAM_annotationTargets     = "annotation targets";
    transient public static final String PARAM_completedTargets     = "completed targets";

    public AnnotationSessionTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public AnnotationSessionTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_sessionName, "");
        setParameter(PARAM_annotationCategories, "");
        setParameter(PARAM_annotationTargets, "");
        setParameter(PARAM_completedTargets, "");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_sessionName)
        		||key.equals(PARAM_annotationCategories)
        		||key.equals(PARAM_annotationTargets)
        		||key.equals(PARAM_completedTargets)) {
            return new TextParameterVO(value);
        }

        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}