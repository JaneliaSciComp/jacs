
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.HashSet;
import java.util.List;

/**
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class JFSExportTask extends Task {

    public static final String DISPLAY_NAME = "JFS Export Task";
    public static final String PARAM_SOURCE = "sourcePath";

    public JFSExportTask() {
        super();
    }

    public JFSExportTask(String owner,
                         List<Event> events,
                         String sourcePath) {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE, sourcePath);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getSourcePath() {
        return getParameter(PARAM_SOURCE);
    }

}