
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * @author Michael Press
 */
abstract public class ParameterRenderer extends Renderer {
    ParameterVO _param;
    String _key;
    Task _task;

    public ParameterRenderer(ParameterVO param, String key, Task task) {
        this(param, key, task, true);
    }

    public ParameterRenderer(ParameterVO param, String key, Task task, boolean realizeNow) {
        _param = param;
        _key = key;
        _task = task;
        if (realizeNow) {
            realize();
        }
    }

    public ParameterVO getValueObject() {
        return _param;
    }

    public void setValueObject(ParameterVO param) {
        _param = param;
//        Window.alert("Setting "+_key + " to " + param.getStringValue());
        _task.setParameter(_key, param.getStringValue());
    }

    abstract protected Widget createPanel();

    protected void realize() {
        add(createPanel());
    }
}
