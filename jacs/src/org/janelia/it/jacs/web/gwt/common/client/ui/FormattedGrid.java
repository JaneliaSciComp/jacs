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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Grid;

/**
 * Generic widget for laying out tabular data. Like GWT's Grid, but automatically formats cells using JaCS styles.
 *
 * @author Michael Press
 */
public class FormattedGrid extends Grid {
    public FormattedGrid() {
        super();
    }
/*
    public FormattedGrid(int col, int col1)
    {
        super(col, col1);
    }

    private void addGridRow(String prompt, String value, Grid grid, int row)
    {
        addGridRow(prompt, value, null, grid, row);
    }

    private void addGridRow(Widget widget, Grid grid, int row)
    {
        addGridRow(null, null, widget, grid, row);
    }

    private void addGridRow(String prompt, String value, Widget widget, Grid grid, int row)
    {
        int col = 0;
        if (prompt != null) {
            grid.setWidget(row, col, HtmlUtils.getHtml(prompt + ":", "prompt"));
            grid.getCellFormatter().setStyleName(row, col, "gridCell");
            grid.getCellFormatter().addStyleName(row, col, "gridCellFullWidth");  // prevent the prompt from breaking on whitespace
            col++;
        }

        if (widget != null)
            grid.setWidget(row, col, widget);
        else {
            value = (value == null) ? "&nbsp;" : value;
            HTMLPanel setPanel = new HTMLPanel(value);
            setPanel.setStyleName("text");
            grid.setWidget(row, col, setPanel);
        }
        grid.getCellFormatter().setStyleName(row, col, "gridCell");
    }
    */
}
