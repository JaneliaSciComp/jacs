
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 4, 2010
 * Time: 1:49:26 PM
 */
public class ShortOrfTrimTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "shortOrfTrimTask";
    transient public static final String DISPLAY_NAME = "Short ORF Trim";

    // Parameters
    transient public static final String PARAM_REMOVE_BLACKLISTED_ORFS = "removeBlacklistedOrfs";

    public ShortOrfTrimTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public ShortOrfTrimTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        setParameter(PARAM_REMOVE_BLACKLISTED_ORFS, null);
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_REMOVE_BLACKLISTED_ORFS)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
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