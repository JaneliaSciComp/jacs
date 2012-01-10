
package org.janelia.it.jacs.model.tasks.prokAnnotation;

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
 * Date: Mar 4, 2010
 * Time: 1:49:26 PM
 */
public class LoadGopherContigsTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "loadGopherContigsTask";
    transient public static final String DISPLAY_NAME = "Load Gopher Contigs";

    // Parameters
    transient public static final String PARAM_CONTIG_FILE_PATH = "contigFilePath";

    public LoadGopherContigsTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public LoadGopherContigsTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        setParameter(PARAM_CONTIG_FILE_PATH, null);
        this.taskName = TASK_NAME;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_CONTIG_FILE_PATH)) {
            return new TextParameterVO(value);
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