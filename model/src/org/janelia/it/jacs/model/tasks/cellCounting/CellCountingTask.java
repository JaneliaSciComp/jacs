package org.janelia.it.jacs.model.tasks.cellCounting;

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
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 7/6/12
 * Time: 3:26 PM
 */
public class CellCountingTask extends Task {
    transient public static final String TASK_NAME              = "cellCounting";
    transient public static final String DISPLAY_NAME           = "Cell Counting";
    transient public static final String PARAM_planInformation  = "planInformation";
    transient public static final String PARAM_inputFilePath    = "inputFilePath";
    transient public static final String PARAM_resultsFolderName= "resultsFolderName";

    public static final String DEFAULT_PLAN =
            "-ist 60 -nt 40 -cst 110 -dc 3 -ec 6 -mnr 90\n" +
            "-ist 50 -nt 35 -cst 90 -dc 3 -ec 5 -mnr 80\n" +
            "-ist 45 -nt 30 -cst 80 -dc 3 -ec 4 -mnr 70\n" +
            "-ist 40 -nt 25 -cst 80 -dc 3 -ec 4 -mnr 60\n" +
            "-ist 30 -nt 25 -cst 80 -dc 3 -ec 4 -mnr 50\n" +
            "-ist 25 -nt 25 -cst 75 -dc 3 -ec 3 -mnr 50\n" +
            "-ist 25 -nt 25 -cst 70 -dc 3 -ec 3 -mnr 50\n";


    public CellCountingTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet,
                            String inputFilePath, String resultsFolderName, String planInformation) {
        super(inputNodes, owner, events, taskParameterSet);
        setTaskName(DISPLAY_NAME+" Task");
        setParameter(PARAM_inputFilePath, inputFilePath);
        setParameter(PARAM_resultsFolderName, resultsFolderName);
        setParameter(PARAM_planInformation, planInformation);
    }

    public CellCountingTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (key.equals(PARAM_resultsFolderName) || key.equals(PARAM_inputFilePath) || key.equals(PARAM_planInformation)) {
            return new TextParameterVO(value);
        }
        if (value == null)
            return null;
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return true;
    }

}
