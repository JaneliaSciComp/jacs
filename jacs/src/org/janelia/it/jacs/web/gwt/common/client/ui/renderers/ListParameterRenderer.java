
package org.janelia.it.jacs.web.gwt.common.client.ui.renderers;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.List;

/**
 * @author Michael Press
 */
public abstract class ListParameterRenderer extends ParameterRenderer {
    private ListBox _listBox;
    private MyChangeListener changeListener = new MyChangeListener();

    public ListParameterRenderer(ParameterVO param, String key, Task task) {
        super(param, key, task, false);
        realize();
    }

    protected Widget createPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        _listBox = new ListBox();
        String[] items = getListItems();
        for (int i = 0; items != null && i < items.length; i++) {
            _listBox.addItem(items[i]);
        }

        _listBox.addChangeListener(changeListener);
        panel.add(_listBox);
        return panel;
    }

    protected abstract String[] getListItems();

    protected abstract void setSelectedItems(String[] selectedItems);

    /**
     * Convenience method to get String values from a List
     *
     * @param values the items which will be rendered in the list
     * @return a string array of the items
     */
    protected String[] getListItems(List<String> values) {
        String[] vals = new String[values.size()];
        for (int i = 0; i < values.size(); i++)
            vals[i] = (String) values.get(i);

        return vals;
    }

    protected void setSelectedItem(int targetListItem) {
        _listBox.setSelectedIndex(targetListItem);
    }

    public class MyChangeListener implements ChangeListener {
        public void onChange(Widget widget) {
            String[] tmpItems = new String[0];
            tmpItems[0] = _listBox.getItemText(_listBox.getSelectedIndex());
            setSelectedItems(tmpItems);
        }
    }
}
