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
