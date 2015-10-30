
package org.janelia.it.jacs.model.tasks.recruitment;

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
 * Date: Jul 17, 2008
 * Time: 10:35:36 AM
 */
public class RecruitmentSamplingBlastDatabaseBuilderTask extends Task {
    public static final String DISPLAY_NAME = "Create FRV Sampling Database Task";
    public static final String PARAM_ORIGINAL_BLAST_DB_NODE_IDS = "originalBlastDBNodeIds";
    public static final String PARAM_SAMPLING_DB_NAME = "samplingDbName";
    public static final String PARAM_SAMPLING_DB_DESCRIPTION = "samplingDBDescription";


    public RecruitmentSamplingBlastDatabaseBuilderTask() {
        super();
        taskName = "Create FRV Sampling Blast Database";
    }

    public RecruitmentSamplingBlastDatabaseBuilderTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                                       String originalBlastDBNodeId, String samplingDbName, String samplingDbDescription) {
        super(inputNodes, owner, events, taskParameterSet);
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_ORIGINAL_BLAST_DB_NODE_IDS, originalBlastDBNodeId);
        setParameter(PARAM_SAMPLING_DB_NAME, samplingDbName);
        setParameter(PARAM_SAMPLING_DB_DESCRIPTION, samplingDbDescription);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_ORIGINAL_BLAST_DB_NODE_IDS) || key.equals(PARAM_SAMPLING_DB_NAME) ||
                key.equals(PARAM_SAMPLING_DB_DESCRIPTION)) {
            return new TextParameterVO(value, 400);
        }
        // no match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getOriginalBlastDbNodeIds() {
        return getParameter(PARAM_ORIGINAL_BLAST_DB_NODE_IDS);
    }

    public String getSamplingDbName() {
        return getParameter(PARAM_SAMPLING_DB_NAME);
    }

    public String getSamplingDbDescription() {
        return getParameter(PARAM_SAMPLING_DB_DESCRIPTION);
    }
}