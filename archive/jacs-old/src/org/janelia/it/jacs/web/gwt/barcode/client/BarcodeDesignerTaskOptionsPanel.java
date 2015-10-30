
package org.janelia.it.jacs.web.gwt.barcode.client;

import org.janelia.it.jacs.model.tasks.barcodeDesigner.BarcodeDesignerTask;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.web.gwt.common.client.panel.user.TaskOptionsPanel;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 24, 2008
 * Time: 4:34:47 PM
 */
public class BarcodeDesignerTaskOptionsPanel extends TaskOptionsPanel {
    protected boolean displayParameter(String parameterKeyName, ParameterVO tmpParam) {
        return super.displayParameter(parameterKeyName, tmpParam) && !(
                BarcodeDesignerTask.PARAM_ampliconsFile.equalsIgnoreCase(parameterKeyName) ||
                        BarcodeDesignerTask.PARAM_primerFile.equalsIgnoreCase(parameterKeyName));
    }
}