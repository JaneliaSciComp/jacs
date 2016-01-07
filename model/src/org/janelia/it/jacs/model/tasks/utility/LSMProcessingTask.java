
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

    public static final String TASK_NAME = "lsmprocessing";
    public static final String DISPLAY_NAME = "LSM Processing Task";
    public static final String PARAM_LSM_NAMES = "lsm names";
    public static final String PARAM_REUSE_PIPELINE_RUNS = "reuse pipeline runs";
    public static final String PARAM_REUSE_SUMMARY = "reuse summary";
    public static final String PARAM_REUSE_PROCESSING = "reuse processing";
    public static final String PARAM_REUSE_ALIGNMENT = "reuse alignment";

    public LSMProcessingTask() {
        super();
        setDefaultValues();
    }

    public LSMProcessingTask(String owner, List<String> lsmNames) {
        super(new HashSet<Node>(), owner, new ArrayList<Event>(), new HashSet<TaskParameter>());
        setDefaultValues();
        setLsmNames(lsmNames);
    }

    private void setDefaultValues() {
        setParameter(PARAM_REUSE_PIPELINE_RUNS, "true");
        setParameter(PARAM_REUSE_SUMMARY, "true");
        setParameter(PARAM_REUSE_PROCESSING, "true");
        setParameter(PARAM_REUSE_ALIGNMENT, "true");
        setTaskName(TASK_NAME);
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
    public List<String> getLsmNames() { return listOfStringsFromCsvString(getParameter(PARAM_LSM_NAMES)); }

    public void setLsmNames(List<String> lsmNames) {
        setParameter(PARAM_LSM_NAMES, csvStringFromCollection(lsmNames));
    }

    @XmlElement
    public Boolean getReusePipelineRuns() {
        return getParameterAsBoolean(PARAM_REUSE_PIPELINE_RUNS);
    }

    public void setReusePipelineRuns(Boolean val) {
        setParameterAsBoolean(PARAM_REUSE_PIPELINE_RUNS, val);
    }

    @XmlElement
    public Boolean getReuseSummary() {
        return getParameterAsBoolean(PARAM_REUSE_SUMMARY);
    }

    public void setReuseSummary(Boolean val) {
        setParameterAsBoolean(PARAM_REUSE_SUMMARY, val);
    }

    @XmlElement
    public Boolean getReuseProcessing() {
        return getParameterAsBoolean(PARAM_REUSE_PROCESSING);
    }

    public void setReuseProcessing(Boolean val) {
        setParameterAsBoolean(PARAM_REUSE_PROCESSING, val);
    }

    @XmlElement
    public Boolean getReuseAlignment() {
        return getParameterAsBoolean(PARAM_REUSE_ALIGNMENT);
    }

    public void setReuseAlignment(Boolean val) {
        setParameterAsBoolean(PARAM_REUSE_ALIGNMENT, val);
    }
}
