/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
