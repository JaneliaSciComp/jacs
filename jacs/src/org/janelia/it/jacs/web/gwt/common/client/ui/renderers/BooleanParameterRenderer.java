
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.BooleanParameterVO;

/**
 * @author Michael Press
 */
public class BooleanParameterRenderer extends RadioParameterRenderer {
    public BooleanParameterRenderer(BooleanParameterVO param, String butonGroupName, Task task) {
        super(param, butonGroupName, task);
    }

    protected String[] getButtonLabels() {
        return new String[]{"True", "False"};
    }

    protected void setSelectedParameterValue(String selectedParameterValue) {
        boolean tmpBoolean = selectedParameterValue.toLowerCase().indexOf("true") >= 0;
        ((BooleanParameterVO) _param).setBooleanValue(tmpBoolean);
        setValueObject(_param); // updates task
    }
}
