
package org.janelia.it.jacs.web.gwt.blast.client.panel;

import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class BlastTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(BlastTask.PARAM_query.equalsIgnoreCase(parameterKeyName) ||
                BlastTask.PARAM_subjectDatabases.equalsIgnoreCase(parameterKeyName) ||
                BlastTask.PARAM_believeDefline.equalsIgnoreCase(parameterKeyName));
    }
}
