package org.janelia.it.jacs.model.tasks.fly;

import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * Discover files in a set of input directories and create corresponding entities in the database.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FlyScreenPatternAnnotationTask extends Task {

    transient public static final String TASK_NAME = "flyScreenPatternAnnotation";
    transient public static final String DISPLAY_NAME = "Fly Screen Pattern Annotation";
    // Sample input file

    // Parameter Keys
    transient public static final String PARAM_topLevelFolderName = "top level folder name";
    transient public static final String PARAM_refresh = "refresh";

    // Default values - default overrides

    public FlyScreenPatternAnnotationTask(Set<Node> inputNodes, String owner, List<Event> events,
    		Set<TaskParameter> taskParameterSet, String topLevelFolderName, Boolean refresh) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
        setParameter(PARAM_topLevelFolderName, topLevelFolderName);
        setParameter(PARAM_refresh, refresh==null?"false":refresh.toString());
    }

    public FlyScreenPatternAnnotationTask() {
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_topLevelFolderName, "");
        setParameter(PARAM_refresh, "false");
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_topLevelFolderName)) {
            return new TextParameterVO(value, 4000);
        }
        if (key.equals(PARAM_refresh)) {
            return new BooleanParameterVO(Boolean.parseBoolean(value));
        }
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
