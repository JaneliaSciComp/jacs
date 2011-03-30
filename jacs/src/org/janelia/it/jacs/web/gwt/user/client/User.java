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

package org.janelia.it.jacs.web.gwt.user.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.BaseEntryPoint;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 18, 2006
 * Time: 3:18:54 PM
 */
public class User extends BaseEntryPoint {
    public Grid _table;
//    private static StatusServiceAsync _statusservice = (StatusServiceAsync) GWT.create(User.class);
//    static {((ServiceDefTarget) _statusservice).setServiceEntryPoint("status.srv");}

    public void onModuleLoad() {
        // Create and fade in the page contents
        setBreadcrumb(new Breadcrumb[]{
                new Breadcrumb(Constants.PREFS_SECTION_LABEL),
                new Breadcrumb(Constants.PREFS_USER_PREFS_LABEL)
        },
                Constants.ROOT_PANEL_NAME);

        VerticalPanel rows = new VerticalPanel();
        DOM.setStyleAttribute(rows.getElement(), "width", "100%");

        /*
        // Row 1 - data/overview + metadata panels
        HorizontalPanel row1cells = new HorizontalPanel();
        row1cells.setWidth("100%");
        row1cells.setHeight("100%");
        rows.add(row1cells);

        // Create a rounded panel for the sets
        VerticalPanel statusPanel = new VerticalPanel();
        statusPanel.setWidth("100%");
        RoundedPanel2 panel = new RoundedPanel2(statusPanel, RoundedPanel2.ALL, "#777777");
        panel.setCornerStyleName("popupPanelRounding");
        row1cells.add(statusPanel);
        */

        HTMLPanel html = new HTMLPanel("<span id='statusTable'></span>");
        //html.setStyleName("popupPanel");
        html.setWidth("100%");
        rows.add(html);

        //TODO: make generic column-sortable table
        // Add a to the panel - will be populated asynchronously by service call
        _table = new Grid(0, 7);
        _table.setCellPadding(0);
        _table.setCellSpacing(0);
        _table.setStyleName("centeredTable");
        html.add(_table, "statusTable");

        // Make async service call
        getStatus();

        rows.setVisible(true);

        RootPanel.get(Constants.ROOT_PANEL_NAME).add(rows);
    }

    /**
     * Adds column headers to a table
     */
    private void addGridTitleRow(Grid grid, String[] colHeaders) {
        addGridRow(grid, colHeaders, "prompt", "tableHeader");
    }

    /**
     * Adds a row to a table, dynamically resizing the table as necessary
     */
    private void addGridDataRow(Grid grid, String[] strings) {
        addGridRow(grid, strings, "text", "tableCellEven", "tableCellOdd");
    }

    private void addGridRow(Grid grid, String[] strings, String cellStyleName, String rowStyleName) {
        addGridRow(grid, strings, cellStyleName, rowStyleName, null);
    }

    //TODO: extract a FormattedTable object
    private void addGridRow(Grid grid, String[] strings, String cellStyleName, String evenRowStyleName,
                            String oddRowStyleName) {
        int row = grid.getRowCount();
        grid.resizeRows(row + 1);

        String rowStyle = evenRowStyleName;
        if (oddRowStyleName != null && isOdd(row))
            rowStyle = oddRowStyleName;

        for (int i = 0; i < strings.length; i++) {
            HTML html = HtmlUtils.getHtml(strings[i], cellStyleName);
            //TODO: fix this hack
//            html.addMouseListener(new HoverStyleSetter(html, rowStyle, cellStyleName+"Hover", null));

            grid.setWidget(row, i, html);
            grid.getCellFormatter().setStyleName(row, i, rowStyle);
        }
    }

    private boolean isOdd(int i) {
        return i % 2 == 0;
    }

    private void getStatus() {
//        _statusservice.getStatusInfo(new AsyncCallback() {
//            public void onFailure(Throwable caught)
//            {
//                //TODO: put some kind of error in the table
//                System.out.println("setService.onFailure()");
//            }
//
//            // On success, populate the table with the DataNodes received
//            public void onSuccess(Object result)
//            {
//                StatusInfo[] statusList = (StatusInfo[]) result;
//                addGridTitleRow(_table, new String[]{"Job Name", "Username", "JobID", "Program", "Status", "Database", "Submitted"});
//                for (int i = 0; i < statusList.length; i++) {
//                    String[] row = new String[]{
//                            statusList[i].getJobname(),
//                            statusList[i].getUsername(),
//                            statusList[i].getJobId(),
//                            statusList[i].getProgram(),
//                            statusList[i].getStatus(),
//                            statusList[i].getDatabase(),
//                            statusList[i].getSubmitted().toString()};
//                    addGridDataRow(_table, row );
//                }
//            }
//        });
    }
}
