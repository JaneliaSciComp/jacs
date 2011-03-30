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
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveRightAlignedLauncher;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.DoubleClickSelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverStyleSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.*;

/*
 * SortableTable is a FlexTable which provides client-side (in-browser) sorting.
 *
 * To sort correctly, the objects in each cell must be of type <code>Comparable</code> (i.e. implement the
 <code>Comparable</code> interface and implement compareTo() and toString()).
 *
 * @author Michael Press
 */
public class SortableTable extends FlexTable implements Cloneable {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable");

    public static final int HEADER_ROW = 0;

    private SortableColumn[] _defaultSortCols;
    private SortableColumn[] _sortCols;
    private List<TableColumn> _tableCols = new ArrayList<TableColumn>();   // List of TableColumn objects
    private List<TableRow> _tableRows = new ArrayList<TableRow>();   // List of RowData Objects
    private SortableTableController _controller;
    private List<TablePopulateListener> _populateListeners = new ArrayList<TablePopulateListener>();
    private ArrayList<TableSortListener> _sortListeners = new ArrayList<TableSortListener>();
    private List<TableClearListener> _clearListeners = new ArrayList<TableClearListener>();
    private ArrayList<SelectionListener> _selectionListeners = new ArrayList<SelectionListener>();
    private ArrayList<DoubleClickSelectionListener> _doubleClickSelectionListeners = new ArrayList<DoubleClickSelectionListener>();

    //TODO: get these via methods so subclasses can override
    private static final String DEFAULT_TABLE_HEADER_STYLE = "tableHeader";
    private static final String DEFAULT_NONSORTABLE_TABLE_HEADER_TEXT_STYLE = "tableHeaderText";
    private static final String DEFAULT_SORTABLE_TABLE_HEADER_TEXT_STYLE = "tableHeaderSortableText";
    private static final String DEFAULT_TABLE_HEADER_TEXT_HOVER_STYLE = "tableHeaderSortableTextHover";
    private static final String DEFAULT_TABLE_HEADER_SORT_IMAGE_STYLE = "tableHeaderSortImage";
    private static final String DEFAULT_CELL_STYLE = "tableCell";
    private static final String DEFAULT_CELL_TEXT_STYLE = "tableCellText";
    private static final String DEFAULT_CELL_SELECT_STYLE = "tableCellSelection";
    private static final String DEFAULT_CELL_POST_SELECT = "tableRowPostSelect";
    private static final String DEFAULT_EVEN_ROW_STYLE = "tableRowEven";
    private static final String DEFAULT_ODD_ROW_STYLE = "tableRowOdd";
    private static final String DEFAULT_TABLE_ROW_HOVER_STYLE = "tableRowHover";
    private static final String HEADER_POPUP_HOST_TEXT_STYLE = "tableHeaderPopupHostText";
    private static final String HEADER_POPUP_HOST_HOVER_STYLE = "tableHeaderPopupHostHover";

    private static final char TAB = '\t';
    private static final char COMMA = ',';
    private static final char EOL = 0x0A;

    private String _evenRowStyle = DEFAULT_EVEN_ROW_STYLE;
    private String _oddRowStyle = DEFAULT_ODD_ROW_STYLE;
    private String _cellStyle = DEFAULT_CELL_STYLE;

    private boolean _highlightSelect = false;
    private SelectedRow _selectedRow = null;

    public class SelectedRow {
        TableRow _rowObject;
        int _rowIndex;

        public SelectedRow(TableRow rowObject, int rowIndex) {
            _rowObject = rowObject;
            _rowIndex = rowIndex;
        }

        public TableRow getRowObject() {
            return _rowObject;
        }

        public int getRowIndex() {
            return _rowIndex;
        }
    }

    public SortableTable() {
        super();
        init();
    }

    private void init() {
        _controller = new SortableTableController(this);

        // notice hover events so we can highlight the current row, and clicks so we can select rows
        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK | Event.ONDBLCLICK);
        // notice clicks so we can sort
        addTableListener(_controller);

        setStyleName("table");
        setCellPadding(0);
        setCellSpacing(0);
    }

    public void setTableController(SortableTableController controller) {
        if (_controller != null) {
            removeTableListener(_controller);
        }
        _controller = controller;
        if (_controller != null) {
            addTableListener(_controller);       // notice clicks so we can sort
        }
    }

    /**
     * Delegates all browser events to the SortableTableController
     */
    public void onBrowserEvent(Event event) {
        if (_controller == null || event == null || getEventTargetCell(event) == null)
            return;

        _controller.handleEvent(event, getEventTargetCell(event));
    }

    private String getRowStyle(int row) {
        return (row % 2 == 0) ? getEvenRowStyle() : getOddRowStyle();
    }

    /*
     * Adds the Column Header to the table. Uses the row 0 to add the header names.
     * Renders the name and the asc/desc/blank gif to the column
     */
    public void addColumn(TableColumn tableColumn) {
        int col = _tableCols.size();

        _tableCols.add(tableColumn);
        setHeaderStyles(col);
        setHeaderWidget(col, tableColumn);
    }

    /**
     * Retrieve the object associated with a row from the table
     *
     * @param row a one based index in the table
     * @return the object associated with the specified row
     */
    public Object getRowData(int row) {
        TableRow tableRow = getRow(row - 1);
        return tableRow != null ? tableRow.getRowObject() : null;
    }

    /**
     * Returns the TableCell representing the cell at (row, col). Note that row is always >=1 since row 0 is the
     * header row. Callers should note that the table may get sorted by the user, which will change the location of
     * this TableCell;  to be notified of sorts, add a TableSortListener, and re-retreive the cell after the sort is
     * complete.
     */
    public TableCell getValue(int row, int col) {
        if (row == 0) // ignore header row
            return null;

        row--; // map 1-based table data to 0-based table rows
        if (getRowCount() < row)
            return null;
        else
            return getRow(row).getTableCell(col);
    }

    /*
     * Sets the values in specified row/column. Expects a Comparable Object for sorting.
     */
    public TableCell setValue(int row, int col, Comparable value) {
        return setValue(row, col, value, null);
    }


    /**
     * Sets the value and the display widget for the specified row/column.  The value is a Comparable used for
     * sorting purposes only.  The Widget is used as the table cell.
     */
    public TableCell setValue(int row, int col, Comparable value, Widget displayWidget) {
        return setValue(row, col, value, displayWidget, false);
    }

    /**
     * Sets the value and the display widget for the specified row/column.  The value is a Comparable used for
     * sorting purposes only.  The Widget is used as the table cell.
     */
    public TableCell setValue(int row, int col, Comparable value, Widget displayWidget, boolean forceRefresh) {
        // The row should begin with 1 (row 0 is for the header)
        if (row == HEADER_ROW) {
            setHeaderValue(col, (value == null) ? "" : value.toString(), forceRefresh); // widget not applicable
            return null;
        }

        // Update the data model
        ensureTableSize(row);
        TableCell cell = new TableCell(value, displayWidget);
        getRow(row - 1).setValue(col, cell);

        // Set the cell's display attributes
        if (forceRefresh) {
            setCellStyles(row, col, cell);
            setCellWidget(row, col, cell);
        }

        return cell;
    }

    /**
     * Allows a forced refresh of a cell.  The cell is refreshed with the current value of the cell in the internal
     * data model;  the value of the cell may NOT be specified, so that the data model and the visible DOM
     * representation of the table do not get out of sync, and to prevent a cell from displaying the wrong value
     * if, for example, the user sorts or pages the table during the process of updating the cell.
     */
    public void refreshCell(int row, int col) {
        setCellStyles(row, col, getRow(row - 1).getTableCell(col));
        setCellWidget(row, col, getRow(row - 1).getTableCell(col));
    }

    public void refreshColumn(int col) {
        for (int row = 1; row < getNumDataRows(); row++)
            refreshCell(row, col);
    }

    /**
     * Sets the given row/column of the flextable based on the given table cell
     *
     * @param row
     * @param col
     * @param cell
     * @return
     */
    public TableCell setTableCell(int row, int col, TableCell cell) {
        // The row should begin with 1 (row 0 is for the header)
        if (row == HEADER_ROW)
            return null;

        // Update the data model
        ensureTableSize(row);
        getRow(row - 1).setValue(col, cell);
        return cell;
    }


    public void removeRow(int rowToRemove) {
        if (rowToRemove > 0) {
            // Remove the row from the browser display and the data model
            super.removeRow(rowToRemove);
            _tableRows.remove(rowToRemove - 1);
            refreshTableBody(true, false);
        }
    }

    public void setColumnVisible(int column, boolean visible) {
        TableColumn tableColumn = _tableCols.get(column);
        if (tableColumn != null) {
            tableColumn.setVisible(visible);
        }

    }

    private void ensureTableSize(int row) {
        if ((row - 1) >= _tableRows.size() || null == _tableRows.get(row - 1))
            _tableRows.add(row - 1, new TableRow());
    }

    protected void setHeaderStyles(int col) {
        getCellFormatter().setStyleName(HEADER_ROW, col, DEFAULT_TABLE_HEADER_STYLE);
    }

    /**
     * uses the underlying FlexTable index, not the TableRow index
     */
    public void setCellStyles(int row) {
        TableRow tableRow = _tableRows.get(row);
        for (int col = 0; col < _tableCols.size(); col++)
            setCellStyles(row, col, tableRow.getTableCell(col));
    }

    protected void setCellStyles(int row, int col, TableCell cell) {
        if (cell == null) {
            _logger.error("SortableTable setCellStyles cell is null for column " + col + " row " + row);
            return;
        }
        getFlexCellFormatter().setColSpan(row, col, 1);
        getCellFormatter().addStyleName(row, col, getCellStyle());
        getCellFormatter().addStyleName(row, col, getRowStyle(row));
        getCellFormatter().addStyleName(row, col, getCol(col).getTextStyleName());
        if (cell.getWidget() == null || DOM.getInnerText(cell.getWidget().getElement()).equals(""))
            getCellFormatter().addStyleName(row, col, getHighlightSelect() ? DEFAULT_CELL_SELECT_STYLE : DEFAULT_CELL_TEXT_STYLE);
        if (_highlightSelect && row != 0 && _selectedRow != null && _selectedRow.getRowIndex() == row) {
            TableRow thisRow = _tableRows.get(row - 1);
            if (thisRow == _selectedRow.getRowObject())
                getCellFormatter().addStyleName(row, col, DEFAULT_CELL_POST_SELECT);
        }
    }

    public void setHeaderValue(int colNum, String displayName, boolean refreshCell) {
        TableColumn column = getCol(colNum);
        column.setDisplayName(displayName);
        setHeaderWidget(colNum, getCol(colNum));

        if (refreshCell)
            refreshTableHeader();
    }

    protected void setHeaderWidget(int colNum, TableColumn column) {
        if (column == null) {
            _logger.error("SortableTable setHeaderWidget column is null for colNum " + colNum);
            return;
        }
        setWidget(HEADER_ROW, colNum, createHeaderHtmlWidget(colNum, column));
        setVisible(getCellFormatter().getElement(0, colNum), column.isVisible());
        _controller.annotateCell(HEADER_ROW, colNum);
    }

    /**
     * This is the method that acutally updates the DOM with the table cell's HTML
     */
    private void setCellWidget(int row, int col, TableCell cell) {
        if (cell == null) {
            _logger.error("SortableTable setCellWidget cell is null for column " + col + " row " + row);
            return;
        }

        // Update the DOM with the HTML or widget, and set the cell's visiblity to the column's
        if (cell.getWidget() != null) {
            // If widget produces no visible output, we need to add some kind of content or border styles look funny
            if (DOM.getInnerText(cell.getWidget().getElement()).equals("") && !getCol(col).hasImageContent())
                setHTML(row, col, "&nbsp;");
            else
                setWidget(row, col, cell.getWidget());
        }
        else {
            if (cell.getValue() == null || (cell.getValue().toString().trim().equals("")))
                setHTML(row, col, "&nbsp;");
            else
                setHTML(row, col, cell.getValue().toString());
        }
        setVisible(getCellFormatter().getElement(row, col), getCol(col).isVisible());

        // Finally, annotate the data cells and the DOM cell with their row and column
        cell.setRow(row);
        cell.setCol(col);
        _controller.annotateCell(row, col);
    }

    public void addCellHoverStyle(int row, int col) {
        getCellFormatter().addStyleName(row, col, DEFAULT_TABLE_ROW_HOVER_STYLE);
    }

    public void removeCellHoverStyle(int row, int col) {
        getCellFormatter().removeStyleName(row, col, DEFAULT_TABLE_ROW_HOVER_STYLE);
    }

    public void addCellHoverStyle(int row) {
        for (int col = 0; col < getNumCols(); col++)
            addCellHoverStyle(row, col);
    }

    public void removeCellHoverStyle(int row) {
        for (int col = 0; col < getNumCols(); col++)
            removeCellHoverStyle(row, col);
    }

    /*
     * Sorts the data model asynchronously, notifying SortListeners before start (onBusy()) and after completion (onBusyDone()).
     * The actual sort is postponed for 1/10th of a sec (in another thread) so the browser can process the listener's
     * DOM updates (like a busy indicator) before the browser is too busy to respond.
     */
    public void sortAsync(SortableColumn[] sortColumns) {
        // Notify listeners of impending sort, then start the sort after a 1/10-sec delay
        onSort();
        new SortTimer(sortColumns).schedule(100);
    }

    /**
     * Synchronous sort
     */
    public void sort() {
        sort(getSortColumns());
    }

    /**
     * Synchronous sort
     *
     * @param sortColumns the columns to sort by
     */
    public void sort(SortableColumn[] sortColumns) {
        onSort();
        sortInternal(sortColumns);
    }

    /**
     * The actual sorting, which is invoked by a timer to separate the onSort() call from the computationally-intensive sort
     */
    private void sortInternal(SortableColumn[] sortColumns) {
        // Sort the data, then update the HTML table so they're displayed to the user
        if (sortColumns != null && sortColumns.length > 0)
            Collections.sort(_tableRows, new MultiColumnComparator(sortColumns));

        // Notify listeners that the sort's complete
        onSortComplete();
        _logger.debug("... done sorting");
    }

    public SortableColumn[] getAllSortableColumns() {
        List<SortableColumn> sortableColumnsList = new ArrayList<SortableColumn>();
        int nCols = getNumCols();
        for (int i = 0; i < nCols; i++) {
            TableColumn tCol = getCol(i);
            if (tCol.isSortable()) {
                sortableColumnsList.add(new SortableColumn(i, tCol.getDisplayName(), SortableColumn.SORT_ASC));
            }
        }
        SortableColumn[] allSortableColumns = new SortableColumn[sortableColumnsList.size()];
        for (int i = 0; i < allSortableColumns.length; i++) {
            allSortableColumns[i] = sortableColumnsList.get(i);
        }
        return allSortableColumns;
    }

    public SortableColumn[] getDefaultSortColumns() {
        return _defaultSortCols;
    }

    public void setDefaultSortColumns(SortableColumn[] sortColumns) {
        if (sortColumns == null) {
            _defaultSortCols = new SortableColumn[]{
                    new SortableColumn(0, getCol(0).getDisplayName(), SortableColumn.SORT_ASC)
            };
        }
        else {
            _defaultSortCols = sortColumns;
        }
    }

    public SortableColumn[] getSortColumns() {
        if (_sortCols == null && _defaultSortCols != null) {
            // if the sort cols have not been set yet but there are default sort columms clone the defaults
            _sortCols = new SortableColumn[_defaultSortCols.length];
            for (int i = 0; i < _sortCols.length; i++) {
                _sortCols[i] = new SortableColumn(_defaultSortCols[i].getColumnPosition(),
                        _defaultSortCols[i].getColumnHeading(),
                        _defaultSortCols[i].getSortArgumentName(),
                        _defaultSortCols[i].getSortDirection());
            }
        }
        return _sortCols;
    }

    public void setSortColumns(SortableColumn[] sortColumns) {
        _sortCols = sortColumns;
        if (_sortCols != null) {
            // if the column is not sortable disable the sorting
            for (SortableColumn _sortCol : _sortCols) {
                TableColumn tableColumn = getCol(_sortCol.getColumnPosition());
                if (tableColumn == null || !tableColumn.isSortable()) {
                    // if no such column or the column is not sortable
                    // make sure that there's not sorting
                    _sortCol.setSortDirection(SortableColumn.SORT_NOTSET);
                }
            }
        }
    }

    private class MultiColumnComparator implements Comparator {
        SortableColumn[] _sortColumns;

        private MultiColumnComparator(SortableColumn[] sortColumns) {
            _sortColumns = sortColumns;
        }

        public int compare(Object o1, Object o2) {
            if (_sortColumns == null || _sortColumns.length == 0) {
                return 0;
            }
            TableRow r1 = (TableRow) o1;
            TableRow r2 = (TableRow) o2;
            int compRes = 0;
            for (SortableColumn _sortColumn : _sortColumns) {
                if (_sortColumn.getSortDirection() == SortableColumn.SORT_NOTSET) {
                    continue;
                }
                int colPos = _sortColumn.getColumnPosition();
                TableColumn tableCol = getCol(colPos);
                if (tableCol == null || !tableCol.isSortable()) {
                    continue;
                }

                Comparable c1 = r1.getColumnValue(colPos);
                Comparable c2 = r2.getColumnValue(colPos);
                try {
                    compRes = c1.compareTo(c2);
                }
                catch (ClassCastException e) {
                }

                if (compRes == 0) {
                    continue;
                }
                // if we have to sort in descending order we simply reverse the comparison result
                if (_sortColumn.getSortDirection() == SortableColumn.SORT_DESC) {
                    compRes = -compRes; // reverse the result
                }
                // and then we return since we already found a difference
                break;
            }
            return compRes;
        }

    }

    //TODO: move sort process into helper/controller class?
    protected class SortTimer extends Timer {
        SortableColumn[] _sortColumns;

        public SortTimer(SortableColumn[] sortColumns) {
            _sortColumns = sortColumns;
        }

        public void run() {
            sortInternal(_sortColumns);
        }
    }

    private TableRow getRow(int row) {
        return _tableRows.get(row);
    }

    public TableColumn getCol(int col) {
        TableColumn tableCol = null;
        if (col < _tableCols.size()) {
            tableCol = _tableCols.get(col);
        }
        return tableCol;
    }

    public void toggleSortDirection(int col) {
        boolean colFound = false;
        TableColumn tableColumn = getCol(col);
        if (tableColumn == null || !tableColumn.isSortable()) {
            // if the column is not sortable don't do anything
            return;
        }
        SortableColumn[] currentSortCols = getSortColumns();
        // at this point we know the column is sortable
        if (currentSortCols != null) {
            for (SortableColumn currentSortCol : currentSortCols) {
                if (currentSortCol.getColumnPosition() == col) {
                    colFound = true;
                    int sortDirection = currentSortCol.getSortDirection();
                    sortDirection = (sortDirection == SortableColumn.SORT_ASC) ?
                            SortableColumn.SORT_DESC :
                            SortableColumn.SORT_ASC;
                    currentSortCol.setSortDirection(sortDirection);
                }
            }
        }
        if (!colFound) {
            setSortColumns(new SortableColumn[]{
                    new SortableColumn(col, tableColumn.getDisplayName(), SortableColumn.SORT_ASC)
            });
        }
    }

    /**
     * refreshes the styles used on the table.  Useful if the table might be in an inconsitent state, such as
     * showing a row highlighted after a series of mouse events.
     */
    public void refresh() {
        refreshTableHeader();
        refreshTableBody(true, true);
    }

    private void refreshTableHeader() {
        Iterator colHeaderIter = _tableCols.iterator();
        for (int col = 0; colHeaderIter.hasNext(); col++) {
            setHeaderStyles(col);
            setHeaderWidget(col, (TableColumn) colHeaderIter.next());
        }
    }

    /*
     * Renders the body or the remaining rows of the table except the header.
     */
    private void refreshTableBody(boolean refreshWidgets, boolean refreshStyles) {
        try {
            if (_tableCols == null || _tableRows == null) {
                return;
            }
            for (int col = 0; col < _tableCols.size(); col++) {
                for (int row = 0; row < _tableRows.size(); row++) {
                    TableRow tableRow = _tableRows.get(row);
                    if (tableRow == null)
                        _logger.error("SortableTable refreshTableBody tableRow is null for column " + col + " row " + row);
                    else
                        refreshTableCell(refreshWidgets, refreshStyles, col, row + 1, tableRow.getTableCell(col));
                }
            }
            onPopulateComplete();
        }
        catch (RuntimeException e) {
            _logger.error("SortableTable refreshTableBody caught exception", e);
            //throw e;
            //todo Can't figure where it's coming from.. eat it for now
        }
    }

    private void refreshTableCell(boolean refreshWidgets, boolean refreshStyles, int col, int row, TableCell tableCell) {
        if (refreshStyles)
            setCellStyles(row, col, tableCell);
        if (refreshWidgets)
            setCellWidget(row, col, tableCell);
    }

    /*
     * Creates the HTML (text + possible sort image) for a column header
     */
    private Widget createHeaderHtmlWidget(int colNum, TableColumn column) {
        int sortFlag = SortableColumn.SORT_NOTSET;
        SortableColumn[] currentSortCols = getSortColumns();
        if (column.isSortable()) {
            if (currentSortCols != null) {
                for (SortableColumn currentSortCol : currentSortCols) {
                    if (currentSortCol.getColumnPosition() == colNum) {
                        sortFlag = currentSortCol.getSortDirection();
                        break;
                    }
                }
            }
            return createHeaderHtmlForSortableColumn(sortFlag, column, colNum);
        }
        else {
            return createHeaderHtmlForNonSortableColumn(column);
        }
    }

    private Widget createHeaderHtmlForNonSortableColumn(TableColumn column) {
        return HtmlUtils.getHtml(column.getDisplayName(), DEFAULT_NONSORTABLE_TABLE_HEADER_TEXT_STYLE);
    }

    private Widget createHeaderHtmlForSortableColumn(int sortDirection, final TableColumn column, int colNum) {
        // Create the text link (with a popup if text was supplied)
        HTML html;
        if (column.getPopupText() != null) {
            html = new PopperUpperHTML(column.getDisplayName(),
                    HEADER_POPUP_HOST_TEXT_STYLE, HEADER_POPUP_HOST_HOVER_STYLE,
                    HEADER_POPUP_HOST_TEXT_STYLE, HEADER_POPUP_HOST_HOVER_STYLE,
                    HtmlUtils.getHtml(column.getPopupText(), "infoText"));
            if (colNum + 1 == getNumCols()) // right-align last column popup or FF has display problems
                ((PopperUpperHTML) html).setLauncher(new PopupAboveRightAlignedLauncher());
            else
                ((PopperUpperHTML) html).setLauncher(new PopupAboveLauncher());
        }
        else {
            html = new HTML(column.getDisplayName());
            html.setStyleName(DEFAULT_SORTABLE_TABLE_HEADER_TEXT_STYLE);
            // Add a hover listener to flip the text style
            html.addMouseListener(new HoverStyleSetter(html, DEFAULT_SORTABLE_TABLE_HEADER_TEXT_STYLE, DEFAULT_TABLE_HEADER_TEXT_HOVER_STYLE, (HoverListener) null));
        }

        // Create a panel with the text and the right image
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(html);
        panel.add(getSortImage(sortDirection));

        return panel;
    }

    private Image getSortImage(int sortDirection) {
        Image image;
        if (sortDirection == SortableColumn.SORT_ASC)
            image = ImageBundleFactory.getControlImageBundle().getSortAscendingImage().createImage();
        else if (sortDirection == SortableColumn.SORT_DESC)
            image = ImageBundleFactory.getControlImageBundle().getSortDescendingImage().createImage();
        else
            image = ImageBundleFactory.getControlImageBundle().getSortBlankImage().createImage();

        image.setStyleName(DEFAULT_TABLE_HEADER_SORT_IMAGE_STYLE);
        return image;
    }

    public int getNumCols() {
        return _tableCols.size();
    }

    public int getNumDataRows() {
        return _tableRows.size();
    }

    /**
     * Clears all table data and displays a "Loading..." message in the table
     */
    public void setLoading() {
        setLoading("Loading...");
    }

    public void setLoading(String msg) {
        setTempMessage(msg, "loadingMsgText", true);
    }

    /**
     * Remove the loading style
     */
    public void unsetLoading() {
        setTempMessage("&nbsp;", DEFAULT_CELL_STYLE, true);
    }

    /**
     * Clears all table data and displays a "No Data." message in the table
     */
    public void setNoData() {
        setNoData("No Data.");
    }

    public void setNoData(String msg) {
        setTempMessage(msg, "text", true);
    }

    public void setError() {
        setError("An error occurred retrieving data from the server");
    }

    public void setError(String msg) {
        setTempMessage(msg, "error", true);
    }

    /**
     * Removes the display of data and displays a short message instead.  If clearData is true, the
     * underlying table data is trashed.  NOTE: this directly updates the DOM, unlike the refresh()
     * methods which expect data in TableRows.  This means that SortableTable clients don't have to
     * know that the table is displaying data - getNumDataRows() is still 0, and adding data to the
     * table will overwrite the temp message.
     */
    private void setTempMessage(String message, String styleName, boolean clearData) {
        if (clearData)
            clear();

        // Set styles then value
        getCellFormatter().setStyleName(1, 0, styleName);
        getFlexCellFormatter().setColSpan(1, 0, getNumCols());
        setHTML(1, 0, "&nbsp;" + message + "&nbsp;");
    }

    public void clearHover() {
        for (int row = 0; row < _tableRows.size(); row++)
            removeCellHoverStyle(row + 1);
    }

    /**
     * Clears the table model of all data rows, clears the DOM of all data rows, and resets the current sort to the
     * default.  The header remains.
     */
    public void clear() {
        clearDataAndDisplay();
        clearSort();
        onClearComplete();
    }

    public void clearDataAndDisplay() {
        // has to be called in this sequence
        clearSelect();
        clearDisplay();
        clearData();
    }

    public void clearData() {
        for (int row = 0; row < _tableRows.size(); row++) {
            TableRow tableRow = getRow(row);
            for (int col = 0; row < _tableRows.size(); row++) {
                TableCell cell = tableRow.getTableCell(col);
                cell.setRow(TableCell.NOT_SET);
                cell.setCol(TableCell.NOT_SET);
            }
        }
        _tableRows.clear();
    }

    public void clearDisplay() {
        // Clear the display - have to explicitly remove each row (super.clear() doesn't work for some reason)
        for (int row = _tableRows.size(); row > 0 && row < getRowCount(); row--) {
            _logger.debug("Removing row with super.removeRow for row=" + row);
            super.removeRow(row);
        }
    }

    public void clearSort() {
        setSortColumns(getDefaultSortColumns());
    }

    public String getEvenRowStyle() {
        return _evenRowStyle;
    }

    public void setEvenRowStyle(String evenRowStyle) {
        _evenRowStyle = evenRowStyle;
    }

    public String getOddRowStyle() {
        return _oddRowStyle;
    }

    public void setOddRowStyle(String oddRowStyle) {
        _oddRowStyle = oddRowStyle;
    }

    public void setDataRowsVisible(int rowStart, int rowEnd, boolean visible) {
        if (!hasData()) {
            return;
        }
        // This is a sortable table meaning that first will always be the header row
        // otherwise there won't be anything to sort by
        rowStart++;
        rowEnd++;
        for (int row = rowStart; row <= rowEnd; row++) {
            for (int col = 0; col < getNumCols(); col++) {
                setVisible(getCellFormatter().getElement(row, col), visible);
            }
        }
    }

    public void setData(List<TableRow> tableRows) {
        if (tableRows == null || tableRows.size() == 0) {
            _logger.debug("tableRows is null or empty - clearing and returning rather than setting data");
            clearDataAndDisplay();
            return;
        }
        _tableRows = tableRows;
    }

    /**
     * Replaces the table's internal data model with a new model, and updates the display with no flashing.
     * <p/>
     * This method solves the flashing problems on FF2 associated with doing the similar sequence of
     * clearDataAndDisplay() then setData() then refresh(), so that the transition to the updated data is
     * very fast and causes no flashing. If the new data set is shorter than the old, the unused table
     * display rows are removed.
     */
    //public void setDataAndRefreshDisplay(List<TableRow> newRows)
    //{
    //    clearSelect();
    //    setData(newRows);
    //    syncDisplaySizeToData();
    //    refresh();
    //}

    //private void syncDisplaySizeToData()
    //{
    //    int dataRows = getNumDataRows();
    //    int tableRows = getRowCount() - 1; // ignore header row
    //    if (tableRows > dataRows) {
    //        for (int i = tableRows; i > dataRows; i--) {
    //            _logger.debug("removing extraneous row " + i);
    //            super.removeRow(i);
    //        }
    //    }
    //}
    private boolean hasData() {
        return _tableRows != null && _tableRows.size() > 0;
    }

    public void setDataRowsVisible(boolean visible) {
        if (hasData()) {
            setDataRowsVisible(0, _tableRows.size() - 1, visible);
        }
    }

    public void setAllColumnsVisible(boolean visible) {
        for (Object _tableCol : _tableCols) {
            TableColumn tableColumn = (TableColumn) _tableCol;
            tableColumn.setVisible(visible);
        }
    }

    public void setEmptyColumnsVisible() {
        try {
            // Start with a set of the colums; assume all empty until we find values
            for (Object _tableCol : _tableCols) {
                TableColumn tableColumn = (TableColumn) _tableCol;
                tableColumn.setVisible(false);
            }
            for (int col = 0; col < _tableCols.size(); col++) {
                TableColumn tableColumn = getCol(col);
                int row = 0;
                while (row < _tableRows.size()) {
                    TableRow tableRow = _tableRows.get(row);
                    TableCell tableCell = tableRow.getTableCell(col);
                    if (tableCell != null && !tableCell.isEmpty()) {
                        tableColumn.setVisible(true);
                        if (_logger.isDebugEnabled())
                            _logger.debug("SortableTable setEmptyColumnsVisible setting col to visible: " + col);
                        break;
                    }
                    row++;
                }
            }
        }
        catch (RuntimeException e) {
            _logger.debug("SortableTable setEmptyColumnsVisible caught excpetion " + e);
            throw e;
        }
    }

    public void addPopulateListener(TablePopulateListener populateListener) {
        _populateListeners.add(populateListener);
    }

    public void removePopulateListener(TablePopulateListener populateListener) {
        _populateListeners.remove(populateListener);
    }

    /**
     * Accepts a TableSortListener that allows the table user to be notified during sort delays.
     */
    public void addSortListener(TableSortListener sortListener) {
        _sortListeners.add(sortListener);
    }

    public void removeSortListener(TableSortListener sortListener) {
        _sortListeners.remove(sortListener);
    }

    public void addClearListener(TableClearListener clearListener) {
        _clearListeners.add(clearListener);
    }

    public void removeClearListener(TableClearListener clearListener) {
        _clearListeners.remove(clearListener);
    }

    public void addSelectionListener(SelectionListener selectionListener) {
        _selectionListeners.add(selectionListener);
    }

    public void removeSelectionListener(SelectionListener selectionListener) {
        _selectionListeners.remove(selectionListener);
    }

    public void addDoubleClickSelectionListener(DoubleClickSelectionListener selectionListener) {
        _doubleClickSelectionListeners.add(selectionListener);
    }

    public void removeDoubleClickSelectionListener(DoubleClickSelectionListener selectionListener) {
        _doubleClickSelectionListeners.remove(selectionListener);
    }

    private void onSort() {
        for (TableSortListener _sortListener : _sortListeners) {
            _sortListener.onBusy(this);
        }
    }

    private void onSortComplete() {
        for (TableSortListener _sortListener : _sortListeners) {
            _sortListener.onBusyDone(this);
        }
    }

    private void onClearComplete() {
        if (_clearListeners != null) {
            for (TableClearListener _clearListener : _clearListeners) {
                _clearListener.onBusyDone(this);
            }
        }
    }

    private void onPopulateComplete() {
        if (_populateListeners != null) {
            for (TablePopulateListener _populateListener : _populateListeners) {
                _populateListener.onBusyDone(this);
            }
        }
    }

    /**
     * package scope for controller access
     */
    void selectRowWithDoubleClick(int row) {
        selectRow(row);
        notifyDoubleClickSelectionListeners(row);
    }

    /**
     * Called when a row has been selected (or unselected)
     *
     * @param row the selected row index (NOT the data row index), which includes the header row
     */
    public void selectRow(int row) {
        if (!isSelectable())
            return;

        if (_selectedRow != null && !isClickOnSelectedRow(row))
            clearSelect();

        // Highlight the row
        TableRow tableRow = _tableRows.get(row - 1);
        if (_highlightSelect) {
            _logger.debug("in highlightSelect");
            String rowStyle = getRowStyle(row);
            for (int col = 0; col < tableRow.getNumColumns(); col++) {
                getCellFormatter().removeStyleName(row, col, rowStyle);
                getCellFormatter().addStyleName(row, col, DEFAULT_CELL_POST_SELECT);
            }
        }

        // Notify listeners of the select action
        _logger.debug("selected row " + row);
        _selectedRow = new SelectedRow(tableRow, row);
        notifySelectionListeners(true, row);
    }

    public void clearSelect() {
        if (_selectedRow != null)
            unSelectRow(getSelectedRow().getRowIndex());
    }

    public void unSelectRow(int row) {
        if (!isSelectable())
            return;

        if (_highlightSelect && _selectedRow != null) {
            TableRow previousRow = _selectedRow.getRowObject();
            int previousIndex = _selectedRow.getRowIndex();
            String previousStyle = getRowStyle(previousIndex);
            for (int col = 0; col < previousRow.getNumColumns(); col++) {
                getCellFormatter().removeStyleName(previousIndex, col, DEFAULT_CELL_POST_SELECT);
                getCellFormatter().addStyleName(previousIndex, col, previousStyle);
            }
        }

        // Notify listeners of the unselect action
        _logger.debug("unselected row " + row);
        _selectedRow = null;
        notifySelectionListeners(false, row);
    }

    /* package scope for controller access only */
    boolean isClickOnSelectedRow(int newRow) {
        TableRow tableRow = _tableRows.get(newRow - 1);
        return _selectedRow != null && _selectedRow.getRowObject() == tableRow;
    }

    private void notifySelectionListeners(boolean select, int row) {
        for (SelectionListener _selectionListener : _selectionListeners) {
            if (select) {
                _selectionListener.onSelect(String.valueOf(row));
            }
            else {
                _selectionListener.onUnSelect(String.valueOf(row));
            }
        }
    }

    /**
     * Notifies double click selection listeners of a double-click-select (there's no double-click-unselect)
     */
    private void notifyDoubleClickSelectionListeners(int row) {
        for (DoubleClickSelectionListener _doubleClickSelectionListener : _doubleClickSelectionListeners) {
            _doubleClickSelectionListener.onSelect(String.valueOf(row));
        }
    }

    public String toTabDelimitedValues() {
        return toDelimitedValues(TAB, EOL);
    }

    public String toCommaDelimitedValues() {
        return toDelimitedValues(COMMA, EOL);
    }

    private String toDelimitedValues(char fieldDelim, char lineDelim) {
        StringBuffer out = new StringBuffer();
        // Output the headers
        for (TableColumn _tableCol : _tableCols) {
            //if (isVisible(getCellFormatter().getElement(0, col)))
            if (_tableCol.isVisible())
                out.append("\"").append(_tableCol.getDisplayName()).append("\"").append(fieldDelim);
        }
        out.append(lineDelim);
        // Output the data rows
        int row = 0;
        while (row < _tableRows.size()) {
            TableRow columns = _tableRows.get(row);
            for (int col = 0; col < columns.getNumColumns(); col++)
                outputValue(col, _tableCols.get(col), columns, out, fieldDelim); // won't output anything if value is null
            out.append(lineDelim);
            row++;
        }
        return out.toString();
    }

    /**
     * Outputs one cell value for delimited format. Only outputs the value if the field is visible (wraps in double quotes)
     */
    private void outputValue(int col, TableColumn tableColumn, TableRow columns, StringBuffer out, char fieldDelim) {
        if (!tableColumn.isVisible())
            return;
        if (columns.getTableCell(col) != null && columns.getTableCell(col).getValue() != null) {
            String value = columns.getTableCell(col).getValue().toString(); // Have to toString() the Comparable from getValue()
            if (value != null)
                out.append("\"").append(value).append("\"");
        }
        out.append(fieldDelim);
    }

    /**
     * Returns the row in which the supplied value is found in the supplied column.
     *
     * @return the row number (1-based, meaning 1 is row 1 which is the first data row since the header is row 0),
     *         or -1 if the value is not found
     */
    public int findValueInCol(int col, String value) throws Exception {
        if (col >= getNumCols() || value == null) {
            return -1;
        }
        for (int row = 1; row < getRowCount() - 1; row++) {
            TableCell cell = getValue(row, col);
            if (cell != null && value.equals(cell.getValue()))
                return row;
        }
        return -1;
    }

    public List<TableRow> getTableRows() {
        return _tableRows;
    }

    public String getCellStyle() {
        return _cellStyle;
    }

    public void setCellStyle(String cellStyle) {
        _cellStyle = cellStyle;
    }

    public void setInvisibleColumns(int[] invisibleColumns) {
        if (invisibleColumns == null || invisibleColumns.length == 0) {
            setAllColumnsVisible(true);
        }
        else {
            for (int invisibleColumn : invisibleColumns) {
                setColumnVisible(invisibleColumn, false);
            }
        }
    }

    public void setHighlightSelect(boolean highlightSelect) {
        _highlightSelect = highlightSelect;
    }

    public boolean getHighlightSelect() {
        return _highlightSelect;
    }

    public SelectedRow getSelectedRow() {
        return _selectedRow;
    }

    public void setSelectedRow(SelectedRow selectedRow) {
        _selectedRow = selectedRow;
    }

    /**
     * @return
     */
    public Object clone() {
        try {
            //Add more state as needed
            SortableTable clone = new SortableTable();
            clone._tableCols = (List) ((ArrayList) _tableCols).clone();
            clone._tableRows = (List) ((ArrayList) _tableRows).clone();
            return clone;
        }
        catch (Exception e) {
            _logger.error("SortableTable clone() caught exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean isSelectable() {
        return (_selectionListeners.size() > 0 || _doubleClickSelectionListeners.size() > 0);
    }
}