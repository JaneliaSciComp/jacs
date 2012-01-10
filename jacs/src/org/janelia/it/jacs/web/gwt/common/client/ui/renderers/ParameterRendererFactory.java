
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.util.List;

/**
 * @author Michael Press
 */
public class ParameterRendererFactory {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.renderers.ParameterRendererFactory");

    public static ParameterRenderer getParameterRenderer(String keyName, ParameterVO param, Task task) {
        try {
            if (param == null) {
                _logger.error("Param passed in is NULL.");
                throw new Exception("ParameterVO may not be null");
            }
            if (null == param.getType()) {
                _logger.error("A parameter type is NULL!!! value=" + param.getStringValue());
            }
            if (param.getType().equalsIgnoreCase(BooleanParameterVO.PARAM_BOOLEAN)) {
                return new BooleanParameterRenderer((BooleanParameterVO) param, keyName, task);
            }
            else if (param.getType().equalsIgnoreCase(DoubleParameterVO.PARAM_DOUBLE)) {
                return new DoubleParameterRenderer((DoubleParameterVO) param, keyName, task);
            }
            else if (param.getType().equalsIgnoreCase(LongParameterVO.PARAM_LONG)) {
                return new LongParameterRenderer((LongParameterVO) param, keyName, task);
            }
            else if (param.getType().equalsIgnoreCase(TextParameterVO.PARAM_TEXT)) {
                return new TextParameterRenderer((TextParameterVO) param, keyName, task);
            }
            else if (param.getType().equalsIgnoreCase(SingleSelectVO.PARAM_SINGLE_SELECT)) {
                SingleSelectVO ssvo = ((SingleSelectVO) param);
                SingleSelectParameterRenderer tmpRenderer = new SingleSelectParameterRenderer((SingleSelectVO) param, keyName, task);
                List tmpList = ssvo.getPotentialChoices();
                int selectionIndex = tmpList.indexOf(ssvo.getActualUserChoice());
                tmpRenderer.setSelectedItem(selectionIndex);
                return tmpRenderer;
            }
            else if (param.getType().equalsIgnoreCase(MultiSelectVO.PARAM_MULTI_SELECT)) {
                return new MultiSelectParameterRenderer((MultiSelectVO) param, keyName, task);
            }
            else {
                return new UnknownParameterRenderer(param, keyName, task);
            }
        }
        catch (Throwable e) {
            _logger.error("Error obtaining the render for param");
            return null;
        }
    }
}
