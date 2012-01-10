
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 2, 2010
 * Time: 3:22:33 PM
 */
public class UploadFastqDirectoryTask extends Task {

    public static final String DISPLAY_NAME = "Upload Fastq Directory Task";
    public static final String PARAM_SOURCE_DIR = "sourceDir";
    public static final String PARAM_MATE_MEAN_INNER_DISTANCE = "mateMeanInnerDistance";
    public static final String PARAM_NODE_NAME = "nodeName";

    public UploadFastqDirectoryTask() {
        super();
        this.taskName = DISPLAY_NAME;
    }

    public UploadFastqDirectoryTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                    Long mateMeanInnerDistance, String pathToSourceDirectory, String nodeName) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE_DIR, pathToSourceDirectory);
        setParameter(PARAM_MATE_MEAN_INNER_DISTANCE, "" + mateMeanInnerDistance);
        setParameter(PARAM_NODE_NAME, nodeName);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE_DIR)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_MATE_MEAN_INNER_DISTANCE)) {
            if (value.trim().length()==0) {
                return null;
            } else {
                return new LongParameterVO(0L, 10000000L, new Long(value.trim()));
            }
        }
        if (key.equals(PARAM_NODE_NAME)) {
            return new TextParameterVO(value);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getPathToSourceDirectory() {
        return getParameter(PARAM_SOURCE_DIR);
    }

    public Long getMateMeanInnerDistace() {
        String mmid=getParameter(PARAM_MATE_MEAN_INNER_DISTANCE);
        if (mmid==null || mmid.trim().length()==0) {
            return null;
        } else {
            return new Long(mmid.trim());
        }
    }

    public String getNodeName() {
        return getParameter(PARAM_NODE_NAME);
    }
}
