package org.janelia.it.jacs.web.gwt.tic.client;

import org.janelia.it.jacs.model.tasks.tic.TicTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford, naxelrod
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class TICTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                TicTask.PARAM_inputFile.equalsIgnoreCase(parameterKeyName));
    }
}