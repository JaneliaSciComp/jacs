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

import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupBelowLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.advancedsort.AdvancedSortPopup;

/**
 * @author Cristian Goina
 */
public class AdvancedSortableRemotePaginatorClickListener implements AdvancedSortClickListener {
    private SortableTable table;
    private RemotingPaginator paginator;
    private RemotePagingPanel pagingPanel;

    public AdvancedSortableRemotePaginatorClickListener(SortableTable table,
                                                        RemotingPaginator paginator,
                                                        RemotePagingPanel pagingPanel) {
        this.table = table;
        this.paginator = paginator;
        this.pagingPanel = pagingPanel;
    }

    public void onClick(Widget widget) {
        new PopupBelowLauncher(new AdvancedSortPopup(paginator.getAllSortableColumns(),
                table.getSortColumns(),
                this,
                false)).showPopup(widget);
    }

    public void sortBy(SortableColumn[] sortColumns) {
        paginator.clearData();
        // only set the sort columns in the table since the paginator will get them from there
        paginator.setSortColumns(sortColumns);
        pagingPanel.first();   // handles loading label visiblility
    }

    protected SortableTable getTable() {
        return table;
    }

    protected RemotingPaginator getPaginator() {
        return paginator;
    }

    protected RemotePagingPanel getPagingPanel() {
        return pagingPanel;
    }

}
