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
import org.janelia.it.jacs.web.gwt.common.client.ui.LoadingLabel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Press
 */
public class LocalPaginator extends BasePaginator {
    private static final Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.LocalPaginator");


    public LocalPaginator() {
    }

    public LocalPaginator(SortableTable sortableTable, LoadingLabel pagingInProgressLabel, String rowsPerPagePreferenceKey) {
        super(sortableTable, pagingInProgressLabel, rowsPerPagePreferenceKey);
    }

    protected void update() {
        logger.debug("LocalPaginator.update()");
        if (hasData()) {
            getSortableTable().clearDataAndDisplay();
            getSortableTable().setData(createPageRows());
        }
        // Call to refresh changes the DOM and makes data visible to user
        getSortableTable().refresh();
    }


    /**
     * Returns the subset of <code>allTableRows</code> that corresponds to <code>currentOffSet</code> and <code>rowsPerPage</code>
     *
     * @return a list containing the table rows specified
     */
    public List<TableRow> createPageRows() {
        try {
            //currentOffset and lastValue are one based
            //currentOffSet=1 and lastValue=10 should set rows 0 through 9 to visible for 0 based table rows
            int rowToStart = getCurrentOffset() - 1;
            int rowToEndAt = getLastRow() - 1;
            List<TableRow> currentList = new ArrayList<TableRow>();
            //logger.debug("Paginator getList allTableRows="+allTableRows);
            for (int i = rowToStart; i <= rowToEndAt && i < getAllTableRows().size(); i++) {
                currentList.add(getAllTableRows().get(i));
            }
            //logger.debug("Paginator getList returning currentList="+currentList);
            return currentList;
        }
        catch (RuntimeException e) {
            // Log the exception so we know where "null is null or not an object" JavaScript error is coming from
            logger.error("Paginator getList() caught exception " + e.getMessage());
            throw e;
        }

    }


}
