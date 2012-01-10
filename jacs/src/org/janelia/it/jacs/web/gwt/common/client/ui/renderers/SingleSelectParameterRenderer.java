
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.SingleSelectVO;

/**
 * @author Michael Press
 */
public class SingleSelectParameterRenderer extends ListParameterRenderer {
    public SingleSelectParameterRenderer(SingleSelectVO param, String key, Task task) {
        super(param, key, task);
    }

    protected SingleSelectVO getSingleSelectValueObject() {
        return (SingleSelectVO) getValueObject();
    }

    protected String[] getListItems() {
        return getListItems(getSingleSelectValueObject().getPotentialChoices());
    }

    protected void setSelectedItems(String[] selectedItems) {
        ((SingleSelectVO) _param).setActualUserChoice(selectedItems[0]);
        setValueObject(_param); // updates task
    }


}
