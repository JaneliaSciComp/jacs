
package org.janelia.it.jacs.web.gwt.reversePsiBlast.client;

import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class ReversePsiBlastTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                ReversePsiBlastTask.PARAM_query_node_id.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_subjectDatabases.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_alignmentViewOptions.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_databaseSize.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_outputFileName.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_querySequenceProtein.equalsIgnoreCase(parameterKeyName) ||
                        ReversePsiBlastTask.PARAM_loopParameter.equalsIgnoreCase(parameterKeyName));
    }
}