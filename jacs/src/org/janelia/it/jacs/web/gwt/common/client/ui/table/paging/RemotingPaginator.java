
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class RemotingPaginator extends BasePaginator {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePaginator");

    private Map<Integer, TableRow> _model = new HashMap<Integer, TableRow>(); // HashMap<Integer, TableRow> holds data loaded so far
    private int _totalRowCount = 0; // total rows available in the database
    private PagedDataRetriever _dataRetriever; // call to load data
    private List<DataRetrievedListener> _dataRetrievedCallbacks = new ArrayList<DataRetrievedListener>();// called after data is loaded
    private List<DataRetrievedListener> _numRowsRetrievedCallbacks = new ArrayList<DataRetrievedListener>();// called after number of rows is retrieved
    private SortableColumn[] sortableColumns; // value to pass to data retriever to specify current column to sort on
    private int startIndex;

    public RemotingPaginator() {
    }

    public RemotingPaginator(SortableTable sortableTable, String[][] sortableColumns, String rowsPerPagePreferenceKey) {
        this(sortableTable, null, sortableColumns, rowsPerPagePreferenceKey);
    }

    public RemotingPaginator(SortableTable sortableTable,
                             PagedDataRetriever dataRetriever,
                             String[][] sortableColumns,
                             String rowsPerPagePreferenceKey) {
        super(sortableTable, rowsPerPagePreferenceKey);
        _dataRetriever = dataRetriever;
        if (sortableColumns != null) {
            this.sortableColumns = new SortableColumn[sortableColumns.length];
            for (int i = 0; i < sortableColumns.length; i++) {
                this.sortableColumns[i] = new SortableColumn(i, sortableColumns[i][1], sortableColumns[i][0]);
            }
        }
    }

    public SortableColumn[] getAllSortableColumns() {
        return sortableColumns;
    }

    public void setDataRetriever(PagedDataRetriever dataRetriever) {
        this._dataRetriever = dataRetriever;
    }

    public SortableColumn[] getSortColumns() {
        SortableColumn[] sortColumns = getSortableTable().getSortColumns();
        if (sortColumns != null) {
            for (SortableColumn sortColumn : sortColumns) {
                sortColumn.setSortArgumentName(getSortableColummnName(sortColumn.getColumnPosition()));
            }
        }
        return sortColumns;
    }

    public void setSortColumns(SortableColumn[] sortColumns) {
        if (sortColumns != null) {
            for (SortableColumn sortColumn : sortColumns) {
                sortColumn.setSortArgumentName(getSortableColummnName(sortColumn.getColumnPosition()));
            }
        }
        getSortableTable().setSortColumns(sortColumns);
    }

    /**
     * Adds a listener for data updated events.
     * Note that these listeners will be notified with a bogus data argument
     * in case of success so the data received as part of the notification is not usable at all
     *
     * @param listener callback to tell data has been retrieved
     */
    public void addDataRetrievedCallback(DataRetrievedListener listener) {
        _dataRetrievedCallbacks.add(listener);
    }

    public void removeDataRetrievedCallback(DataRetrievedListener listener) {
        _dataRetrievedCallbacks.remove(listener);
    }

    public void addNumRowsRetrievedCallback(DataRetrievedListener listener) {
        _numRowsRetrievedCallbacks.add(listener);
    }

    public void removeNumRowsRetrievedCallback(DataRetrievedListener listener) {
        _numRowsRetrievedCallbacks.remove(listener);
    }

    /**
     * Overrides parent next to use retrieve new data if not already cached
     */
    public void next() {
        _logger.debug("RemotingPaginator.next()");
        if (getCurrentOffset() + getRowsPerPage() <= getTotalRowCount()) {
            gotoRow(getCurrentOffset() + getRowsPerPage());
        }
    }

    /**
     * Overrides parent next to use retrieve new data if not already cached
     */
    public void previous() {
        _logger.debug("RemotingPaginator.previous()");
        int offset = getCurrentOffset() - getRowsPerPage();
        if (offset < 1) {
            offset = 1;
        }
        gotoRow(offset);
    }

    /**
     * Overrides parent next to use retrieve new data if not already cached
     */
    public void last() {
        _logger.trace("RemotingPaginator.last()");
        setCurrentOffset(determineLastPageOffset());
        gotoRow(getCurrentOffset());
    }

    /**
     * Overrides parent next to use retrieve new data if not already cached
     */
    public void first() {
        _logger.debug("RemotingPaginator.first()");
        gotoRow(1);
    }

    /**
     * Displays the data at the given starting row if it has already been retrieved, or schedules it for retrieval otherwise
     *
     * @param startRow row to display data for
     */
    public void gotoRow(int startRow) {
        _logger.debug("gotoRow(" + startRow + ")");
        setCurrentOffset(startRow);
        if (hasDataAt(startRow)) {
            updateTable(startRow);
        }
        else {
            retrieveDataRows(startRow);
        }
    }

    /**
     * Returns the total row count in the database (not necessarily the total number of rows that have been retrieved)
     */
    public int getTotalRowCount() {
        return _totalRowCount;
    }

    public void setTotalRowCount(int totalRowCount) {
        _logger.debug("RemotingPaginator setting total row count to " + totalRowCount);
        _totalRowCount = totalRowCount;
    }

    public boolean hasData() {
        return _totalRowCount > 0;
    }

    /**
     * The method checks if the entire page is cached in the model;
     * If any row is missing then it returns false
     *
     * @param startIndex - page offset
     * @return boolean if there is data at the start index passed in
     */
    protected boolean hasDataAt(int startIndex) {
        for (int i = startIndex; i < startIndex + getRowsPerPage(); i++) {
            //_logger.debug("checking for data at row" + i);
            if (_model.get(i) == null) {
                return false;
            }
        }
        return true;
    }

    public void removeRow(TableRow row) {
        if (getLastRow() != getTotalRowCount()) {
            // The paginator is not on the last page
            // the easiest thing to do is clear the model
            // update the total number of records and retrieve the data again
            if (_totalRowCount > 1) {
                _totalRowCount--;
            }
            // since the sorting parameters are "stored" in the sortable table
            // before removing the row save them and restore them before retrieving the data
            SortableColumn[] prevSortColumns = getSortableTable().getSortColumns();
            clear();
            getSortableTable().setSortColumns(prevSortColumns);
            retrieveDataRows(getCurrentOffset());
        }
        else {
            // the paginator is on the last page
            // determine the row that has to be removed
            int removedRowIndex = -1;
            int pageStartIndex = getCurrentOffset();
            int pageEndIndex = getCurrentOffset() + getRowsPerPage();
            for (int rowIndex = pageStartIndex; rowIndex < pageEndIndex; rowIndex++) {
                TableRow tableRow = _model.get(rowIndex);
                if (tableRow == row) {
                    removedRowIndex = rowIndex;
                    break;
                }
            }
            if (removedRowIndex < 0) {
                // This should not happen for now simply log the error and return
                _logger.error("RemotingPaginator.removeRow row could not locate the row in the model");
                return;
            }
            // remove the row and reindex all subsequent rows that exist in the model
            removeAndShiftTableRows(removedRowIndex);
            if (removedRowIndex == getCurrentOffset() && removedRowIndex > _totalRowCount) {
                // there was only one row on the last page which has just been removed
                // therefore go to the previous page
                previous();
            }
            else {
                // determine the removed row index in the sorted table
                int displayedRowIndex = (removedRowIndex % getRowsPerPage());
                getSortableTable().removeRow(displayedRowIndex);
                // notify data retrieve listeners so that we'll update the panel's controls
                notifyDataRetrievalListeners(null);
            }
        }
    }

    public void clear() {
        super.clear();
        clearData();
    }

    /**
     * Clears all loaded data
     */
    public void clearData() {
        _model.clear();
        setTotalRowCount(0);
    }

    /**
     * Pushes the data at (1-based) startIndex to the visible table;  assumes the data is already retrieved.
     *
     * @param startIndex index to start from
     */
    private void updateTable(int startIndex) {
        _logger.debug("updateTable()");
        this.startIndex = startIndex;
        refresh();
    }

    protected void update() {
        _logger.trace("RemotingPaginator.update()");
        _logger.debug("updating table at row " + startIndex + " with new data using this number of rows=" + getRowsPerPage());

        // Create a new model for the table
        List<TableRow> newRows = createPageRows();

        // Clear the existing model and update with the new one
        if (_logger.isDebugEnabled()) _logger.debug("clearDataAndDisplay()");
        getSortableTable().clearDataAndDisplay(); // retains current sort indicator
        if (_logger.isDebugEnabled()) _logger.debug("setData()");
        getSortableTable().setData(newRows);

        if (_logger.isDebugEnabled()) _logger.debug("refresh()");
        getSortableTable().refresh();
        if (_logger.isDebugEnabled()) _logger.debug("notifyDataRetrievalListeners()");
        // notify data retrieve listeners so that we'll update the panel's controls
        notifyDataRetrievalListeners(null);
        _logger.debug("At end of RemotingPaginator.update() - have this number of rows=" + getRowsPerPage());
    }

    private void cacheData(List<TableRow> tableRows, int rowIndex) {
        if (tableRows == null) {
            return;
        }

        if (_logger.isDebugEnabled()) _logger.debug("caching " + tableRows.size() + " rows at index " + rowIndex);
        for (int newRow = 0, destRow = rowIndex; newRow < tableRows.size(); newRow++, destRow++) {
            _model.put(destRow, tableRows.get(newRow));
        }
    }

    private void notifyDataRetrievalListeners(Throwable error) {
        for (Object _dataRetrievedCallback : _dataRetrievedCallbacks) {
            if (error == null) {
                ((DataRetrievedListener) _dataRetrievedCallback).onSuccess(this);
            }
            else {
                ((DataRetrievedListener) _dataRetrievedCallback).onFailure(error);
            }
        }
    }

    private void notifyNumRowsRetrievalListeners() {
        for (Object _numRowsRetrievedCallback : _numRowsRetrievedCallbacks) {
            ((DataRetrievedListener) _numRowsRetrievedCallback).onSuccess(this);
        }
    }

    public List<TableRow> getData() {
        return (List<TableRow>) _model.values();
    }

    /**
     * Retrieve the total number of data rows for the range indicator
     */
    protected void retrieveNumDataRows() {
        // retrieve only the number of rows
        invokeRetrieveNumDataRows(-1);
    }

    /**
     * Retrieve one page of data
     *
     * @param rowIndex index of the row we're getting data for
     */
    protected void retrieveDataRows(final int rowIndex) {
        _logger.debug("retrieveDataRows(" + rowIndex + ")");
        // Ignore retrieval requests that occur during init;  they're due to the GUI setup and won't result in data retrievals
        if (rowIndex == 0) {
            return;
        }
        if (_totalRowCount == 0) {
            // If total number of rows has not been retrieved, get it now
            // and once that completes retrieve the row data
            invokeRetrieveNumDataRows(rowIndex);
        }
        else {
            invokeRetrieveDataRows(rowIndex);
        }
    }

    /**
     * Retrieve the total number of data rows for the range indicator
     * if the rowIndex is greater than or equal to 0 then the completion of row num retrieval
     * triggers a data retrieval
     *
     * @param rowIndex index to retrieve data for
     */
    private void invokeRetrieveNumDataRows(final int rowIndex) {
        _logger.debug("invokeRetrieveNumDataRows(" + rowIndex + ")");
        _dataRetriever.retrieveTotalNumberOfDataRows(new DataRetrievedListener() {
            public void onSuccess(Object data) {
                Integer numRows = (Integer) data;
                if (numRows == null) {
                    _logger.error("Got null numRows");
                }
                else {
                    _logger.debug("RemotingPaginator: DataRetriever says total row count is " + numRows);
                    setTotalRowCount(numRows);
                    notifyNumRowsRetrievalListeners();
                    if (rowIndex >= 0) {
                        invokeRetrieveDataRows(rowIndex);
                    }
                }
            }

            public void onFailure(Throwable throwable) {
                _logger.error("RemotingPaginator.retrieveTotalNumberOfDataRows().onFailure()", throwable);
                if (getPagingInProgressLabel() != null) {
                    getPagingInProgressLabel().setVisible(false);
                }
                getSortableTable().setError("Error retrieving the number of records: " + throwable.getMessage());
            }

            /** Client might not know the total yet */
            public void onNoData() {
            }
        });
    }

    /**
     * Retrieve one page of data
     * precondition _totalRowCount is set
     *
     * @param rowIndex row index to get data for
     */
    private void invokeRetrieveDataRows(final int rowIndex) {
        _logger.debug("invokeRetrieveDataRows(" + rowIndex + ")");
        // Ignore retrieval requests that occur during init;  they're due to the GUI setup and won't result in data retrievals
        if (rowIndex == 0) {
            return;
        }
        int rowsToRetrieve = getRowsPerPage();
        if (_totalRowCount > 0 && (rowIndex + rowsToRetrieve - 1 > _totalRowCount))
            rowsToRetrieve = _totalRowCount - rowIndex + 1;
        _logger.debug("_dataRetriever.retrieveDataRows() rowsToRetrieve=" + rowsToRetrieve);
        _dataRetriever.retrieveDataRows(rowIndex - 1, rowsToRetrieve, getSortColumns(),
                new DataRetrievedListener() {
                    public void onSuccess(Object data) { // List<TableRow>
                        List<TableRow> tableRows = (List<TableRow>) data;
                        if (tableRows == null || tableRows.size() == 0) { // this is ok, there might actually be no data
                            onNoData();
                            return;
                        }
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("RemotingPaginator: " +
                                    _dataRetriever + " returned " + tableRows.size() + " tableRows");
                        }
                        cacheData(tableRows, rowIndex);
                        updateTable(rowIndex);
                    }

                    public void onFailure(Throwable throwable) {
                        _logger.error("RemotingPaginator.retrieveDataRows().onFailure()", throwable);
                        if (getPagingInProgressLabel() != null) {
                            getPagingInProgressLabel().setVisible(false);
                        }
                        getSortableTable().setError("Error retrieving the data" +
                                (throwable.getMessage() != null ? ": " + throwable.getMessage() : ""));
                        notifyDataRetrievalListeners(throwable);
                    }

                    public void onNoData() {
                        _logger.debug("RemotingPaginator: DataRetreiver returned 0 rows");
                        updateTable(rowIndex); // also updates controls and removes the "loading..." labels
                    }
                });
    }

    private String getSortableColummnName(int colPos) {
        String sortableColName = null;
        if (sortableColumns != null && colPos < sortableColumns.length) {
            sortableColName = sortableColumns[colPos].getSortArgumentName();
        }
        return sortableColName;
    }

    private void removeAndShiftTableRows(int startIndex) {
        _model.remove(startIndex);
        for (int rowIndex = startIndex + 1; rowIndex <= _totalRowCount; rowIndex++) {
            TableRow tableRow = _model.remove(rowIndex);
            if (tableRow != null) {
                _model.put(rowIndex - 1, tableRow);
            }
        }
        if (_totalRowCount > 0) {
            --_totalRowCount;
        }
    }

    public List<TableRow> createPageRows() {
        // Create a new model for the table
        List<TableRow> newRows = new ArrayList<TableRow>();
        for (int i = startIndex; i < startIndex + getRowsPerPage() && i <= getTotalRowCount(); i++) {
            TableRow row = _model.get(i);
            if (row != null) {
                // I am checking for not null because I may delete it from the model first
                // and then call update
                newRows.add(row);
            }
        }
        return newRows;
    }

}
