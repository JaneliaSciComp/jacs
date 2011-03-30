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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

public class SortableTableController implements TableListener {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.Table");

    protected SortableTable _table;
    private static final String ROW_ANNOTATION = "row";
    private static final String COL_ANNOTATION = "col";
    private boolean _processHovers = true; // default

    public SortableTableController(SortableTable sortableTable) {
        super();
        _table = sortableTable;
    }

    /**
     * Implementation of TableListener interface;  called when user clicks on a cell.
     */
    public void onCellClicked(SourcesTableEvents sourcesTableEvents, int row, int col) {
        onSortRequest(row, col);
    }

    /**
     * Handle mouse events :<ol>
     * <li>Mouseover events cause a Hover action</li>
     * <li>Mouseout events cancel Hover actions</li>
     * <li>Clicks on a header cell cause a sort action<li>
     * <li>Clicks on body cells select that row</li>
     * <li>Double-clicks on body cells perform a double-click select of the row</li>
     */
    public void handleEvent(Event event, Element cell) {
        int row = getCellRow(cell);
        if (row < 0) return;
        int col = getCellCol(cell);
        if (DOM.eventGetType(event) == Event.ONMOUSEOVER)
            onHover(row);
        else if (DOM.eventGetType(event) == Event.ONMOUSEOUT)
            afterHover(row);
        else if (DOM.eventGetType(event) == Event.ONDBLCLICK)
            onDoubleClickSelectRequest(row);
        else if (DOM.eventGetType(event) == Event.ONCLICK) {
            if (row == SortableTable.HEADER_ROW)
                onSortRequest(row, col);
            else
                onSelectRequest(row, col);
        }

    }

    protected void onDoubleClickSelectRequest(int row) {
        _logger.debug("SortableTableController got double click");
        _table.selectRowWithDoubleClick(row);
    }

    /**
     * Selects the given row.  If the row is already selected, then the row is unselected.
     */
    protected void onSelectRequest(int row, int col) {
        // if click is on currently-selected row, unselect it
        if (_table.getSelectedRow() != null && _table.getSelectedRow().getRowIndex() == row)
            _table.unSelectRow(row);
        else
            _table.selectRow(row);
    }

    //TODO: move sort order and direction into here?
    /**
     * When the user requests a column sort, resort and redisplay the table
     */
    protected void onSortRequest(int row, int col) {
        if (row != SortableTable.HEADER_ROW || !_table.getCol(col).isSortable())
            return;

        _table.toggleSortDirection(col);
        _table.sort();
        _table.refresh();
    }

    /**
     * When a cell is hovered, add the hover style to all cells in the row
     */
    //TODO: ignore hover events if new row is the same 
    public void onHover(int row) {
        if (row == SortableTable.HEADER_ROW || !_processHovers)
            return;

        _table.addCellHoverStyle(row);
    }

    public void afterHover(int row) {
        if (row == SortableTable.HEADER_ROW || !_processHovers)
            return;

        _table.removeCellHoverStyle(row);
    }

    /**
     * Store row and col attributes on the cell for use on mouse events
     */
    public void annotateCell(int row, int col) {
        Element cell = _table.getCellFormatter().getElement(row, col);
        DOM.setElementProperty(cell, ROW_ANNOTATION, String.valueOf(row));
        DOM.setElementProperty(cell, COL_ANNOTATION, String.valueOf(col));
    }

    public int getCellRow(Element cell) {
        String row = DOM.getElementProperty(cell, ROW_ANNOTATION);
        if (row != null && row.length() > 0) {
            return Integer.parseInt(row);
        }
        else {
            return -1;
        }
    }

    public int getCellCol(Element cell) {
        String col = DOM.getElementProperty(cell, COL_ANNOTATION);
        if (col != null && col.length() > 0) {
            return Integer.parseInt(col);
        }
        else {
            return -1;
        }
    }

    /**
     * Allows hover processing to be temporarily disabled
     */
    public void setHoverHandling(boolean processHovers) {
        _processHovers = processHovers;
    }

    public SortableTable getSortableTable() {
        return _table;
    }
}
