
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
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
public class BZipTestTask extends Task {

    public static final String DISPLAY_NAME = "BZip Test Task";
    public static final String PARAM_SOURCE = "sourcePath";
    public static final String PARAM_MODE = "mode";
    public static final String MODE_COMPRESS = "compress";
    public static final String MODE_DECOMPRESS = "decompress";

    public BZipTestTask() {
        super();
    }

    public BZipTestTask(String owner,
                        List<Event> events,
                        String sourcePath,
                        String mode) {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE, sourcePath);
        if (MODE_COMPRESS.equals(mode) || MODE_DECOMPRESS.equals(mode)) {
            setParameter(PARAM_MODE, mode);
        }
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE) || key.equals(PARAM_MODE)) {
            return new TextParameterVO(value, 400);
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

    public String getMode() {
        return getParameter(PARAM_MODE);
    }

}