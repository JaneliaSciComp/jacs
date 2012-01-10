
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
