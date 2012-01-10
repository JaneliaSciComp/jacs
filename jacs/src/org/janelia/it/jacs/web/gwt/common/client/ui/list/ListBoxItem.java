
package org.janelia.it.jacs.web.gwt.common.client.ui.list;

/**
 * @author Michael Press
 */
public class ListBoxItem {
    private String _displayValue;
    private String _value;

    public ListBoxItem(String displayValue, String value) {
        _displayValue = displayValue;
        _value = value;
    }

    public String getDisplayValue() {
        return _displayValue;
    }

    public String getValue() {
        return _value;
    }
}
