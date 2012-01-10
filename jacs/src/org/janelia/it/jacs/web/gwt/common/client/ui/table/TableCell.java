
package org.janelia.it.jacs.web.gwt.common.client.ui.table;

import com.google.gwt.user.client.ui.Widget;

/**
 * Defines one cell in a (Sortable)Table.  The cell must have a Comparable value, and may optionally have a Widget
 * that is used for display.  The Comparable is used for sorting on the cell's column.
 *
 * @author Michael Press
 */
public class TableCell {
    public static final int NOT_SET = -1;

    Comparable _value;
    Widget _widget = null;
    int row = NOT_SET;
    int col = NOT_SET;

    public TableCell(Comparable value) {
        _value = value;
    }

    public TableCell(Comparable value, Widget widget) {
        _value = value;
        _widget = widget;
    }

    public Comparable getValue() {
        return _value;
    }

    public void setValue(Comparable value) {
        _value = value;
    }

    public Widget getWidget() {
        return _widget;
    }

    public void setWidget(Widget widget) {
        _widget = widget;
    }

    public boolean isEmpty() {
        //return getValue() == null || getWidget()==null || getValue().toString() == null || getValue().toString().trim().equals("") || getValue().toString().equals("&nbsp;");
        return getValue() == null || getValue().toString() == null || getValue().toString().trim().equals("") || getValue().toString().equals("&nbsp;");
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("value=").append(_value);
        buff.append("_widget=").append((_widget == null ? "null" : _widget.toString()));
        return buff.toString();
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
