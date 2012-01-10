
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTableController;

/**
 * This class is used to override SortableTableController onSortRequest when Pagination is used
 *
 * @author Tareq Nabeel
 */
public class PaginatorTableController extends SortableTableController {
    public PaginatorTableController(SortableTable sortableTable) {
        super(sortableTable);
    }

    /**
     * Paginator is a SortListener and when sort is done it's call to table.refresh
     * will only get a subset of the data (the page) displayed versus having to
     * display all the day and then shrink to page size
     */
    protected void onSortRequest(int row, int col) {
        if (row != SortableTable.HEADER_ROW || !getSortableTable().getCol(col).isSortable()) {
            return;
        }
        _table.toggleSortDirection(col);
        _table.sort();
    }
}
