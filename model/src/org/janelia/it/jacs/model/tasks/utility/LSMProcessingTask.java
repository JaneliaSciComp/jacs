
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class LSMProcessingTask extends Task {

    public static final String DISPLAY_NAME = "LSM Processing Task";
    public static final String PARAM_LSM_PATHS = "lsmPaths";

    public LSMProcessingTask() {
        super();
    }

    public LSMProcessingTask(String owner,
                             List<String> lsmPaths) {
        super(new HashSet<Node>(), owner, new ArrayList<Event>(), new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setLsmPaths(lsmPaths);
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

    @XmlElement
    public List<String> getLsmPaths() { return listOfStringsFromCsvString(getParameter(PARAM_LSM_PATHS)); }

    public void setLsmPaths(List<String> lsmPaths) {
        setParameter(PARAM_LSM_PATHS, csvStringFromCollection(lsmPaths));
    }
}