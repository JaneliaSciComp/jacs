
package org.janelia.it.jacs.web.gwt.neuronSeparator.client;

import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class NeuronSeparatorTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList.equalsIgnoreCase(parameterKeyName));
    }
}