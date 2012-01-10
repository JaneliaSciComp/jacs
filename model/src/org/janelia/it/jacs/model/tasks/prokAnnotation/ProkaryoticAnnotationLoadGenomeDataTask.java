
package org.janelia.it.jacs.model.tasks.prokAnnotation;

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
 * Date: Dec 15, 2008
 * Time: 3:03:13 PM
 */
public class ProkaryoticAnnotationLoadGenomeDataTask extends Task {
    transient public static final String TASK_NAME = "prokAnnotationLoadGenomeDataTask";
    transient public static final String DISPLAY_NAME = "Prokaryotic Annotation Pipeline Load Genome Data";

    // Parameter Keys
    transient public static final String PARAM_username = "username";
    transient public static final String PARAM_sybasePassword = "Sybase password";
    transient public static final String PARAM_targetDirectory = "targetDirectory";
    // The ftpSource dir must contain the location; ie ftp://anonymous:anonymous@ftp.ncbi.nih.gov/genomes/Bacteria/
    transient public static final String PARAM_ftpSourceDirectory = "ftpSourceDirectory";

    public ProkaryoticAnnotationLoadGenomeDataTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_username, "");
        setParameter(PARAM_sybasePassword, "");
        setParameter(PARAM_targetDirectory, "");
        setParameter(PARAM_ftpSourceDirectory, "");
        this.taskName = TASK_NAME;
    }

    public ProkaryoticAnnotationLoadGenomeDataTask() {
        super();
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_targetDirectory) || key.equals(PARAM_ftpSourceDirectory) || key.equals(PARAM_username)) {
            return new TextParameterVO(value, 500);
        }
        if (key.equals(PARAM_sybasePassword)) {
            return new TextParameterVO("**Hidden**", 500);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}