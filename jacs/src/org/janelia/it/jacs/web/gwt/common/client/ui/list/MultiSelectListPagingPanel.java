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

package org.janelia.it.jacs.web.gwt.common.client.ui.list;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.OptionItem;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.SelectOptionsLinks;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagingPanel;

import java.util.Arrays;

/**
 * Extends PagingPanel to make custom footer for multi-select lists.
 *
 * @author Michael Press
 */
public class MultiSelectListPagingPanel extends PagingPanel {
    public MultiSelectListPagingPanel(SortableTable sortableTable, String rowsPerPagePreferenceKey,
                                      ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        super(sortableTable, rowsPerPagePreferenceKey);
        init(selectAllClickListener, selectNoneClickListener);
    }

    public void init(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        setStyleName("MultiSelectList");
        getPagingControlsPanelBottom().clear(); // turn off bottom controls
        createFooter(selectAllClickListener, selectNoneClickListener);
    }

    /**
     * Replace the standard footer with one suitable for a multi-select list
     */
    private void createFooter(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        DockPanel footer = getTableFooterPanel();
        footer.setVisible(false);
        footer.clear();
        footer.setWidth("100%");

        Widget left = getSelectPanel(selectAllClickListener, selectNoneClickListener);
        footer.add(left, DockPanel.WEST);
        footer.setCellHorizontalAlignment(left, HorizontalPanel.ALIGN_LEFT);
        footer.setCellVerticalAlignment(left, VerticalPanel.ALIGN_MIDDLE);

        Widget sizerPanel = getSizerLinkPanel();
        footer.add(sizerPanel, DockPanel.EAST);
        footer.setCellHorizontalAlignment(sizerPanel, HorizontalPanel.ALIGN_RIGHT);
        footer.setCellVerticalAlignment(sizerPanel, VerticalPanel.ALIGN_MIDDLE);

        footer.setVisible(true);
    }

    public Widget getSelectPanel(ClickListener selectAllClickListener, ClickListener selectNoneClickListener) {
        OptionItem selectAll = new OptionItem("all", selectAllClickListener);
        OptionItem selectNone = new OptionItem("none", selectNoneClickListener);
        return new SelectOptionsLinks("Select", Arrays.asList(selectAll, selectNone));
    }

    /**
     * Instead of the Goto menu, just put a separator
     */
    protected Widget createGoToMenu() {
        return createVerticalBarSeparator();
    }

    protected String getNextGoToPrevPanelStyleName() {
        return "MultiSelectListPagingSeparatorPanel"; // need more margin since the menu's gone
    }
}
