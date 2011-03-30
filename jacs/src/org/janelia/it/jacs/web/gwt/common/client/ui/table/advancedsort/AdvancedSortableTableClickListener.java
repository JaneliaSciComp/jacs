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

package org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort;

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cristian Goina
 */
public class AdvancedSortableTableClickListener implements AdvancedSortClickListener {

    private SortableTable table;
    private SortableColumn[] sortableColumns;

    public AdvancedSortableTableClickListener(SortableTable table, SortableColumn[] sortableColumns) {
        this.table = table;
        this.sortableColumns = sortableColumns;
    }

    public void onClick(Widget widget) {
        // make only visible columns available for sorting
        List sortableColumnsList = new ArrayList();
        for (int i = 0; i < sortableColumns.length; i++) {
            TableColumn tCol = table.getCol(sortableColumns[i].getColumnPosition());
            if (tCol.isVisible()) {
                sortableColumnsList.add(sortableColumns[i]);
            }
        }
        SortableColumn[] currentSortableColumns = new SortableColumn[sortableColumnsList.size()];
        for (int i = 0; i < currentSortableColumns.length; i++) {
            currentSortableColumns[i] = (SortableColumn) sortableColumnsList.get(i);
        }
        new PopupBelowLauncher(new AdvancedSortPopup(currentSortableColumns,
                table.getSortColumns(),
                this,
                false)).showPopup(widget);
    }

    public void sortBy(SortableColumn[] sortColumns) {
        // only set the sort columns in the table since the paginator will get them from there
        table.setSortColumns(sortColumns);
        table.sort();
    }

}
