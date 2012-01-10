
package org.janelia.it.jacs.model.tasks.rnaSeq;

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
 * User: smurphy
 * Date: Aug 18, 2010
 * Time: 5:37:49 PM
 */
public class UploadRnaSeqReferenceGenomeTask extends Task {

    public static final String DISPLAY_NAME = "Upload RnaSeq Reference Genome Task";
    public static final String PARAM_SOURCE_FILE = "sourceFile";
    public static final String PARAM_NODE_NAME = "nodeName";

    public UploadRnaSeqReferenceGenomeTask() {
        super();
        this.taskName = DISPLAY_NAME;
    }

    public UploadRnaSeqReferenceGenomeTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                          String pathToFile) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_SOURCE_FILE, pathToFile);
        setParameter(PARAM_NODE_NAME, "");
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_SOURCE_FILE)) {
            return new TextParameterVO(value, 400);
        }
        if (key.equals(PARAM_NODE_NAME)) {
            return new TextParameterVO(value, 400);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getPathToOriginalFile() {
        return getParameter(PARAM_SOURCE_FILE);
    }

    public String getNodeName() {
        return getParameter(PARAM_NODE_NAME);
    }

}
