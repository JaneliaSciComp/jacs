
package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
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
 * Date: Oct 3, 2008
 * Time: 10:50:49 AM
 */
public class CreateRecruitmentBlastDatabaseTask extends CreateBlastDatabaseTask {
    public static final String DISPLAY_NAME = "Create Recruitment Blast Database Task";
    public static final String PARAM_SAMPLE_NAME = "sampleName";

    public CreateRecruitmentBlastDatabaseTask() {
        super();
        taskName = "Create Recruitment Blast Database";
    }

    public CreateRecruitmentBlastDatabaseTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                                   String blastDBName, String blastDBDescription, String fastaNodeId, String sampleName) {
        super(inputNodes, owner, events, taskParameterSet, blastDBName, blastDBDescription, fastaNodeId);
        setParameter(PARAM_SAMPLE_NAME, sampleName);
        this.taskName = DISPLAY_NAME;
    }

    @Override
    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) {return null;}
        String value = getParameter(key);
        if (value == null) {return null;}
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null) {return pvo;}
        if (PARAM_SAMPLE_NAME.equals(key)){
            return new TextParameterVO(value);
        }
        // No match
        return null;
    }
}