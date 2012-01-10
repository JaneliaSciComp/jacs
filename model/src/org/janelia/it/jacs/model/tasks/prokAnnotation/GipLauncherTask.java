
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
public class GipLauncherTask extends ProkPipelineBaseTask {
    transient public static final String TASK_NAME = "gipLauncherTask";
    transient public static final String DISPLAY_NAME = "Gip Launcher";
    transient public static final String ASSEMBLY_ISCURRENT = "ISCURRENT";
    transient public static final String GENE_TYPE_LOCUS = "locus";
    transient public static final String GENE_TYPE_FEAT_NAME = "feat_name";

    // Parameters
    transient public static final String PARAM_ASSEMBLY_ID = "assemblyId";
    transient public static final String PARAM_GENE_TYPE = "geneType";


    public GipLauncherTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    public GipLauncherTask() {
        super();
        setDefaultValues();
    }

    protected void setDefaultValues() {
        super.setDefaultValues();
        this.taskName = TASK_NAME;
        setParameter(PARAM_ASSEMBLY_ID, null);
        setParameter(PARAM_GENE_TYPE, null);
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_ASSEMBLY_ID) || key.equals(PARAM_GENE_TYPE)) {
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