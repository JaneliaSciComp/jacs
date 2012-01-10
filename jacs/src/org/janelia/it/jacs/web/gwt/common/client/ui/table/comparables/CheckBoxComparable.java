
package org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables;

import com.google.gwt.user.client.ui.CheckBox;

/**
 * Wraps a checkbox in a Comparable so a column of Checkboxes can be sorted
 *
 * @author Michael Press
 */
public class CheckBoxComparable implements Comparable {
    private CheckBox _checkbox;

    public CheckBoxComparable(CheckBox checkbox) {
        super();
        _checkbox = checkbox;
    }

    public boolean isChecked() {
        return _checkbox.getValue();
    }

    public int compareTo(Object o) {
        if (o == null)
            return 1;

        CheckBoxComparable other = (CheckBoxComparable) o;

        if (isChecked() == other.isChecked())
            return 0;
        else
            return (isChecked() ? 1 : -1);
    }
}
