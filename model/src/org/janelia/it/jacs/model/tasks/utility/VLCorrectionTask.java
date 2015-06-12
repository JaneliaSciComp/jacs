
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.HashSet;
import java.util.List;

/**
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class VLCorrectionTask extends Task {

    public static final String DISPLAY_NAME = "Visually Lossless Correction Task";
    public static final String PARAM_SOURCE = "sourcePath";
    public static final String PARAM_TARGET_OWNER = "targetOwner";
    public static final String PARAM_DEBUG = "debug";

    public VLCorrectionTask() {
        super();
    }

    public VLCorrectionTask(String owner,
                            List<Event> events,
                            String sourcePath,
                            String targetOwner,
                            boolean debug) {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE, sourcePath);
        setParameter(PARAM_DEBUG, Boolean.toString(debug));
        setParameter(PARAM_TARGET_OWNER, targetOwner);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE) || key.equals(PARAM_TARGET_OWNER)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_DEBUG)) {
            return new BooleanParameterVO(Boolean.valueOf(value));
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getSourcePath() {
        return getParameter(PARAM_SOURCE);
    }

    public String getDebug() {
        return getParameter(PARAM_DEBUG);
    }

    public String getTargetOwner() { return getParameter(PARAM_TARGET_OWNER);}
}