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
