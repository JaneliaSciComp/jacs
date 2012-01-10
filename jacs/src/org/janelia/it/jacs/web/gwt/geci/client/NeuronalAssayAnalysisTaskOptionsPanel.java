
package org.janelia.it.jacs.web.gwt.geci.client;

import org.janelia.it.jacs.model.tasks.geci.NeuronalAssayAnalysisTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford, naxelrod
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class NeuronalAssayAnalysisTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                NeuronalAssayAnalysisTask.PARAM_inputFile.equalsIgnoreCase(parameterKeyName));
    }
}