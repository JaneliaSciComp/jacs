
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
 * Date: Mar 4, 2010
 * Time: 3:37:20 PM
 */
public class PressDbTask extends Task {
    transient public static final String TASK_NAME = "pressDBTask";
    transient public static final String DISPLAY_NAME = "Press DB";
    transient public static final String SUFFIX_1CON = ".1con";
    transient public static final String SUFFIX_SEQ = ".seq";

    // Parameter Keys
    transient public static final String PARAM_FILE_SUFFIX = "fileSuffix";

    public PressDbTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public PressDbTask() {
        super();
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_FILE_SUFFIX, null);
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_FILE_SUFFIX)) {
            return new TextParameterVO(value);
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
