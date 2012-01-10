
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTableController;

/**
 * Controls interaction between the RemotePagingPanel, RemotingPaginator and the underlying SortableTable
 *
 * @author Michael Press
 */
public class RemotePagingController extends SortableTableController {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingController");
    private RemotePagingPanel _pagingPanel;
    private RemotingPaginator _paginator;

    public RemotePagingController(SortableTable table, RemotePagingPanel pagingPanel, RemotingPaginator paginator) {
        super(table);
        _pagingPanel = pagingPanel;
        _paginator = paginator;
    }

    /**
     * When user clicks a table header, notify the RemotePagingController of the column requested
     */
    protected void onSortRequest(int row, int col) {
        if (row != SortableTable.HEADER_ROW || !_table.getCol(col).isSortable()) {
            return;
        }
        // Clear any data retrieved, set the new sort on the table, then have the paginator reload the first page of the new data
        _logger.debug("RemotePagingController got sort request on column " + col);
        _paginator.clearData();
        getSortableTable().toggleSortDirection(col);
        _pagingPanel.first();   // handles loading label visiblility
    }
}
