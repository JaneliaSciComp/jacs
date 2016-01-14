
package org.janelia.it.jacs.model.tasks.utility;

import com.google.common.collect.ImmutableSet;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This action is used for processing a list of LSM files.
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
@XmlRootElement(name = "lsmProcessing")
@XmlAccessorType(XmlAccessType.NONE)
public class LSMProcessingTask extends Task {

    public static final String TASK_NAME = "lsmprocessing";
    public static final String DEFAULT_JOBNAME = "LSMInitProcessing";
    public static final String DISPLAY_NAME = "LSM Processing Task";
    public static final String PARAM_LSM_NAMES = "lsm names";
    public static final String PARAM_REUSE_PIPELINE_RUNS = "reuse pipeline runs";
    public static final String PARAM_REUSE_SUMMARY = "reuse summary";
    public static final String PARAM_REUSE_PROCESSING = "reuse processing";
    public static final String PARAM_REUSE_ALIGNMENT = "reuse alignment";
    public static final String PARAM_REUSE_POST = "reuse post";
    public static final String PARAM_RUN_OBJECTIVES = "run objectives";


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
        setJobName(DEFAULT_JOBNAME);
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
        setParameter(PARAM_LSM_NAMES, csvStringFromCollection(ImmutableSet.copyOf(lsmNames)));
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

    @XmlElement
    public Boolean getReusePost() {
        return getParameterAsBoolean(PARAM_REUSE_POST);
    }

    public void setReusePost(Boolean val) {
        setParameterAsBoolean(PARAM_REUSE_POST, val);
    }

    @XmlElement
    public String getRunObjectives() {
        return getParameter(PARAM_RUN_OBJECTIVES);
    }

    public void setRunObjectives(String val) {
        setParameter(PARAM_RUN_OBJECTIVES, val);
    }
}
