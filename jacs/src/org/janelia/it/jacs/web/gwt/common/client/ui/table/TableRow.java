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

import java.util.ArrayList;
import java.util.List;

/*
 * RowData defines one row in a Sortable Table
 */
public class TableRow implements Comparable {
    List<TableCell> _values = new ArrayList<TableCell>(); // Maintains the list of the TableCells in the table
    int _sortCol = 0; // Keeps the current column index being sorted
    Object _rowObject; // row-specific payload for id or other purpose - alternative to storing hidden value in column

    public void setValue(int index, TableCell tableCell) {
        _values.set(ensureSize(index), tableCell);
    }

    private int ensureSize(int index) {
        if (index >= _values.size())
            addNullColumns(index);
        return index;
    }

    public Comparable getColumnValue(int index) {
        return getTableCell(index).getValue();
    }

    public Widget getColumnWidget(int index) {
        return getTableCell(index).getWidget();
    }

    public TableCell getTableCell(int index) {
        return _values.get(index);
    }

    public List getTableCells() {
        return _values;
    }

    public int getSortCol() {
        return _sortCol;
    }

    public void setSortCol(int sortCol) {
        _sortCol = sortCol;
    }

    /*
     * Implementation of interface Comparable. Returns the compare result to another Comparable.  If both are non-null,
     * then invokes compareTo() on the Comparable found in the sort-column of this row.
     * 
     * @return 1 if Comparable in sort-column is greater than Comparable in sort-column of the other RowData object,
     * -1 if the other is greater, or 0 if equal.
     */
    public int compareTo(Object other) {
        // If other is null, return 1
        TableRow otherRow = (TableRow) other;
        if (otherRow == null || otherRow.getColumnValue(_sortCol) == null)
            return 1;

        // If this is null, return -1
        if (getColumnValue(_sortCol) == null)
            return -1;

        // Else invoke compareTo() on the Comparable in the sort-column cell
        return getColumnValue(_sortCol).compareTo(otherRow.getColumnValue(_sortCol));
    }

    /*
     * Adds the Null columns in the table row
     */
    private void addNullColumns(int index) {
        for (int nullIndex = _values.size(); nullIndex <= index; nullIndex++)
            _values.add(null);
    }

    public int getNumColumns() {
        return _values.size();  //To change body of created methods use File | Settings | File Templates.
    }


    public Object getRowObject() {
        return _rowObject;
    }

    public void setRowObject(Object rowObject) {
        _rowObject = rowObject;
    }
}
