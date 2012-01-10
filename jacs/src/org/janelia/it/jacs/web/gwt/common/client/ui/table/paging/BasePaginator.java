
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.user_data.prefs.UserPreference;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.service.prefs.Preferences;
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableClearListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableSortListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for providing paging functionality to a PagingPanel that contains
 * a SortableTable.  It's next(), previous(), first(), and last() methods resets the current row
 * and resets the SortableTable's current data set.  The approach of making rows visible and invisible
 * had GWT issues and was thereby abandoned.
 *
 * @author Tareq Nabeel
 */
abstract public class BasePaginator implements Paginator {
    private static final Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.BasePaginator");

    private int currentOffset;
    private int rowsPerPage = 0;
    // The SortableTable instance to wrap
    private SortableTable sortableTable;
    private String rowsPerPagePreferenceKey;
    private List<TableRow> allTableRows;
    private static final String ROWS_PER_PAGE_CATEGORY = "BasePaginatorRowsPerPage";
    private boolean initialized = false;

    /**
     * Used to display a message while moving to prev/next/first/last page
     */
    protected LoadingLabel pagingInProgressLabel;

    /**
     * @param sortableTable The SortableTable instance to wrap
     */
    public BasePaginator(SortableTable sortableTable, String rowsPerPagePreferenceKey) {
        validate(sortableTable);
        this.sortableTable = sortableTable;
        this.rowsPerPagePreferenceKey = rowsPerPagePreferenceKey;
        sortableTable.addSortListener(new SortableTableSortListener());
        sortableTable.addClearListener(new SortableTableClearListener());
    }

    public BasePaginator(SortableTable sortableTable, LoadingLabel pagingInProgressLabel, String rowsPerPagePreferenceKey) {
        this(sortableTable, rowsPerPagePreferenceKey);
        setPagingInProgressLabel(pagingInProgressLabel);
    }

    public BasePaginator() {
    }

    /**
     * Moves to the next page
     */
    public void next() {
        if (getCurrentOffset() + rowsPerPage <= getTotalRowCount()) {
            setCurrentOffset(getCurrentOffset() + rowsPerPage);
            refresh();
        }
    }

    /**
     * Moves to the previous page
     */
    public void previous() {
        setCurrentOffset(getCurrentOffset() - rowsPerPage);
        if (getCurrentOffset() < 1) {
            setCurrentOffset(1);
        }
        refresh();
    }

    /**
     * Moves to the last page
     */
    public void last() {
        setCurrentOffset(determineLastPageOffset());
        refresh();
    }

    /**
     * Offset becomes the round-off to highest multiple of number-of-visible rows.
     */
    protected int determineLastPageOffset() {
        return ((getTotalRowCount() - 1) / getRowsPerPage()) * getRowsPerPage() + 1;
    }

    /**
     * Moves to the first page
     */
    public void first() {
        setCurrentOffset(1);
        refresh();
    }

    // This method checks to see if preferences exist and uses that rather than the
    // provided value, which is assumed to be a default.
    public void initRowsPerPage(int rowsPerPage) {
        // We assume this is the first time this method is being called, during initialization of the
        // Paginator. Before we use the value in the method call, we first check to see what the user
        // preference for this paginator is and use that instead, assuming that this first call is
        // triggered by the init code rather than the user.
        UserPreference rowsPerPagePreference = Preferences.getUserPreference(rowsPerPagePreferenceKey, ROWS_PER_PAGE_CATEGORY);
        if (rowsPerPagePreference != null) {
            // User has previously set the preference
            logger.debug("Value for key=" + rowsPerPagePreferenceKey + " is=" + rowsPerPagePreference.getValue());
            rowsPerPage = new Integer(rowsPerPagePreference.getValue());
            logger.debug("Found pref key=" + rowsPerPagePreferenceKey + " setting value to=" + rowsPerPage);
        }
        else {
            // User has not previously set the preference - we do nothing in the init case
            logger.debug("preference was NULL for rowsPerPage using key=" + rowsPerPagePreferenceKey + " category=" + ROWS_PER_PAGE_CATEGORY +
                    " - will use value in method, which is=" + rowsPerPage);
        }
        setRowsPerPage(rowsPerPage);
        initialized = true;
    }

    public void setRowsPerPage(int rowsPerPage) {
        logger.debug("BasePaginator.setRowsPerPage(" + rowsPerPage + ")");
        this.rowsPerPage = rowsPerPage;
    }

    // This method does change preferences
    public void modifyRowsPerPage(int rowsPerPage) {
        logger.debug("BasePaginator.modifyRowsPerPage rowsPerPage=" + rowsPerPage + " prefKey=" + rowsPerPagePreferenceKey);
        if (!initialized) {
            // We do not permit this method to be called before initRowsPerPage
            logger.debug("switching control to initRowsPerPage because Paginator is not initialized");
            initRowsPerPage(rowsPerPage);
        }
        else {
            // rowsPerPage was previously set, and the user is changing it to a new value. We will update
            // the UserPreference in addition to setting it.
            if (rowsPerPage != this.rowsPerPage) {
                logger.debug("New value for rowsPerPage=" + rowsPerPage + " does not match previous=" + this.rowsPerPage + " so setting pref");
                UserPreference rowsPerPagePreference = new UserPreference(rowsPerPagePreferenceKey, ROWS_PER_PAGE_CATEGORY,
                        Integer.toString(rowsPerPage));
                Preferences.setUserPreference(rowsPerPagePreference);
            }
            else {
                logger.debug("current and previous rowsPerPage match=" + rowsPerPage + " therefore doing nothing");
            }
            setRowsPerPage(rowsPerPage);
        }
    }


    /**
     * Returns the last row number in the SortableTable
     *
     * @return the row number of the last table row
     */
    public int getLastRow() {
        int lastValue;
        if (getCurrentOffset() + rowsPerPage <= getTotalRowCount()) {
            lastValue = getCurrentOffset() + rowsPerPage - 1;
        }
        else {
            lastValue = getTotalRowCount();
        }
        //logger.debug("Paginator getLastRow() currentOffset="+currentOffset + " rowsPerPage="+rowsPerPage + " getTotalRowCount()="+getTotalRowCount() + " lastValue="+lastValue);
        return lastValue;
    }


    /**
     * @return true if table has data; false otherwise
     */
    private boolean tableHasData() {
        return allTableRows != null && allTableRows.size() > 0;
    }

    /**
     * Returns total row count
     *
     * @return the total row count
     */
    public int getTotalRowCount() {
        if (tableHasData()) {
            return allTableRows.size();
        }
        else {
            return 0;
        }
    }

    /**
     * Validates supplied SortableTable to Paginator
     *
     * @param sortableTable the table that Paginator is wrapping
     */
    private void validate(SortableTable sortableTable) {
        if (sortableTable == null) {
            String error = "Paginator() sortableTable cannot be null";
            logger.error(error);
            throw new RuntimeException(error);
        }
    }

    /**
     * This listener is used to set the page to first row when user sorts on a column
     */
    private class SortableTableSortListener implements TableSortListener {
        public void onBusy(Widget widget) {
            // Sorting is performed on SortableTable's data.  Since Paginator retains only a subset of
            // SortableTable's data, when user clicks one of the sort links and before sorting begins,
            // we need to reset SortableTable's data back to all of the orginal data so sorting is done on
            // whole dataset. onBusyDone() will reset SortableTable's data to first page when sorting is complete
            if (hasData()) {
                logger.debug("Paginator SortableTableSortListener onBusy allTableRows.size()=" + allTableRows.size());

                // NO!!!!  sortableTable and allTableRows must always be separate collections!
                //sortableTable.setData(allTableRows);

                ArrayList<TableRow> newSortableTableAllList = new ArrayList<TableRow>();
                for (TableRow allTableRow : allTableRows) {
                    newSortableTableAllList.add(allTableRow);
                }
                sortableTable.setData(newSortableTableAllList);
            }
        }

        public void onBusyDone(Widget widget) {
            // Obtaining a reference to sortableTable's table rows will not do since we're going
            // to clear SortableTable's contents in the refresh method.  We need to transfer 'em over
            // to a new list.  Collections.copy method is not supported in GWT
            //logger.debug("Paginator SortableTableSortListener onBusyDone before transferTableDataToAllTableRows .. sortableTable.getTableRows().size()="+sortableTable.getTableRows().size());
            transferTableDataToAllTableRows();
            //logger.debug("Paginator SortableTableSortListener onBusyDone after transferTableDataToAllTableRows ... allTableRows.size()="+allTableRows.size());
            first();

        }
    }

    /**
     * Obtaining a reference to sortableTable's table rows will not do since we're going to clear
     * SortableTable's contents in the refresh method.  We need to transfer 'em over to a new list.
     * Collections.copy method is not supported in GWT
     */
    private void transferTableDataToAllTableRows() {
        if (allTableRows == null) {
            allTableRows = new ArrayList<TableRow>();
        }
        allTableRows.clear();
        if (sortableTable.getTableRows() != null) {
            for (Object o : sortableTable.getTableRows()) {
                allTableRows.add((TableRow) o);
            }
        }
    }


    /**
     * This Listener is used to clear the dataset used to set pages when the original sortable table's dataset is cleared
     */
    private class SortableTableClearListener implements TableClearListener {
        public void onBusy(Widget widget) {
        }

        public void onBusyDone(Widget widget) {
            if (allTableRows != null) {
                allTableRows.clear();
            }
        }
    }

    public boolean hasNext() {
        return hasData() && (getCurrentOffset() + rowsPerPage <= getTotalRowCount());
    }

    public boolean hasPrevious() {
        return hasData() && (getCurrentOffset() - rowsPerPage >= 1);
    }

    public boolean hasData() {
        return tableHasData();
    }

    public int getCurrentOffset() {
        return this.currentOffset;
    }

    protected void setCurrentOffset(int offset) {
        this.currentOffset = offset;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public SortableTable getSortableTable() {
        return sortableTable;
    }

    public List<TableRow> getData() {
        return allTableRows;
    }

    public void setData(List<TableRow> allTableRows) {
        this.allTableRows = allTableRows;
    }

    public void clear() {
        if (tableHasData()) {
            allTableRows.clear();
        }
        sortableTable.clear();
    }

    protected List<TableRow> getAllTableRows() {
        return allTableRows;
    }

    /**
     * The base implementation is based on the assumption that allTableRows always
     * mirrors the content of the associated sortable table
     *
     * @param row
     */
    public void removeRow(TableRow row) {
        // determine the row that has to be removed
        int removedRowIndex = -1;
        int pageStartIndex = getCurrentOffset();
        int pageEndIndex = getCurrentOffset() + getRowsPerPage();
        for (int rowIndex = pageStartIndex; rowIndex < pageEndIndex; rowIndex++) {
            TableRow tableRow = allTableRows.get(rowIndex);
            if (tableRow == row) {
                removedRowIndex = rowIndex;
                break;
            }
        }
        if (removedRowIndex < 0) {
            // This should not happen for now simply log the error and return
            logger.error("RemotingPaginator.removeRow row could not locate the row in the model");
            return;
        }
        allTableRows.remove(removedRowIndex);
        if (removedRowIndex == getCurrentOffset() && removedRowIndex > getTotalRowCount()) {
            // there was only one row on the last page which has just been removed
            // therefore go to the previous page
            previous();
        }
        else {
            // determine the removed row index in the sorted table
            int displayedRowIndex = (removedRowIndex % getRowsPerPage());
            getSortableTable().removeRow(displayedRowIndex);
        }
    }

    public void refresh() {
        // Refresh is an instensive operation.  Give previous operations a chance to take effect e.g. loadingLabel.setVisisble(true)
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                update();
                if (pagingInProgressLabel != null)
                    pagingInProgressLabel.setVisible(false);
            }
        });
    }

    protected abstract void update();

    public void setPagingInProgressLabel(LoadingLabel pagingInProgressLabel) {
        this.pagingInProgressLabel = pagingInProgressLabel;
    }

    public LoadingLabel getPagingInProgressLabel() {
        return this.pagingInProgressLabel;
    }

    public abstract List createPageRows();

}
