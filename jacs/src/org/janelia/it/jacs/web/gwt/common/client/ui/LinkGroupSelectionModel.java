
package org.janelia.it.jacs.web.gwt.common.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Dec 1, 2006
 * Time: 6:14:36 PM
 */
public class LinkGroupSelectionModel {
    private String _selectedValue;
    private List<SelectionListener> _listeners = new ArrayList<SelectionListener>();

    public void setSelectedValue(String selectedValue) {
        _selectedValue = selectedValue;
        notify(true, selectedValue);
    }

    // NOTE: synchronized is used here as an advisory that there could be conflict
    // if this class were used outside of GWT, rather than for any functional reason.
    // GWT does not have multithreading, and does not support real synchronization.
    public void addSelectionListener(SelectionListener listener) {
        _listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        if (_listeners.contains(listener))
            _listeners.remove(listener);
    }

    private void notify(boolean onSelect, String value) {
        // Notify listeners
        for (SelectionListener _listener : _listeners) {
            if (onSelect)
                _listener.onSelect(value);
            else
                _listener.onUnSelect(value);

        }
    }

    public String getSelectedValue() {
        return _selectedValue;
    }
}
