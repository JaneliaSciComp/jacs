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

package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.RowIndex;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDate;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Jan 17, 2007
 * Time: 11:45:36 AM
 */
public class TableUtils {

    private static final Logger logger = Logger.getLogger("TableUtils");

    /**
     * This method is used to set the style of value cells in a table
     *
     * @param table
     * @param rowIndex
     */
    public static void setValueCellStyle(HTMLTable table, int rowIndex) {
        setValueCellStyle(table, rowIndex, 1);
    }

    public static void setValueCellStyle(HTMLTable table, int rowIndex, int colIndex) {
        setCellStyle(table, rowIndex, colIndex, "text");
        addCellStyle(table, rowIndex, colIndex, "gridCell");
        addCellStyle(table, rowIndex, colIndex, "gridCellTop");
    }

    /**
     * This method is used to add an HTML string to a table
     */
    public static void addHTMLRow(HTMLTable table, RowIndex rowIndex, String prompt, String itemValue) {
        addHTMLRow(table, rowIndex, 0, prompt, itemValue);
    }

    public static void addHTMLRow(HTMLTable table, RowIndex rowIndex, int startCol, String prompt, String itemValue) {
        if (itemValue == null)
            return;
        table.setHTML(rowIndex.getCurrentRow(), startCol, prompt + ":");
        table.setHTML(rowIndex.getCurrentRow(), startCol + 1, itemValue);

        setLabelCellStyle(table, rowIndex.getCurrentRow(), startCol);
        setValueCellStyle(table, rowIndex.getCurrentRow(), startCol + 1);
    }

    public static void addHTMLRow(HTMLTable table, RowIndex rowIndex, int startCol, String prompt, Widget widget) {
        addHTMLRow(table, rowIndex, startCol, prompt, "");
        table.setWidget(rowIndex.getCurrentRow(), startCol + 1, widget);
    }

    /**
     * This method is used to add a data item of type String to a table
     */
    public static void addTextRow(HTMLTable table, RowIndex rowIndex, String prompt, String itemValue) {
        addTextRow(table, rowIndex, 0, prompt, itemValue);
    }

    public static void addTextRow(HTMLTable table, RowIndex rowIndex, int startCol, String prompt, String itemValue) {
        if (itemValue == null)
            return;

        table.setText(rowIndex.getCurrentRow(), startCol, prompt + ":");
        table.setText(rowIndex.getCurrentRow(), startCol + 1, itemValue);

        setLabelCellStyle(table, rowIndex.getCurrentRow(), startCol);
        setValueCellStyle(table, rowIndex.getCurrentRow(), startCol + 1);
    }

    public static void addTextRow(HTMLTable table, RowIndex rowIndex, int startCol, String prompt, Widget widget) {
        addTextRow(table, rowIndex, startCol, prompt, "");
        table.setWidget(rowIndex.getCurrentRow(), startCol + 1, widget);
    }

    public static void addWidgetRow(HTMLTable table, RowIndex rowIndex, String prompt, Widget widget) {
        if (widget == null)
            return;

        // Set the prompt style
        table.setText(rowIndex.getCurrentRow(), 0, prompt + ":");
        table.getCellFormatter().setVerticalAlignment(rowIndex.getCurrentRow(), 0, HasVerticalAlignment.ALIGN_MIDDLE);
        setLabelCellStyle(table, rowIndex.getCurrentRow(), 0);

        // Set the value widget
        table.setWidget(rowIndex.getCurrentRow(), 1, widget);
        setValueCellStyle(table, rowIndex.getCurrentRow(), 1);
    }


    /**
     * This method is used to add a data item of type Date to a table
     *
     * @param table
     * @param rowIndex
     * @param prompt
     * @param itemValue
     */
    public static void addDateRow(HTMLTable table, RowIndex rowIndex, String prompt, Date itemValue) {
        if (itemValue != null) {
            addTextRow(table, rowIndex, prompt, new FormattedDate(itemValue.getTime()).toString());
        }
    }

    /**
     * This method is used to set the style of a table cell
     *
     * @param table
     * @param rowIndex
     * @param colIndex
     * @param styleName
     */
    public static void setCellStyle(HTMLTable table, int rowIndex, int colIndex, String styleName) {
        table.getCellFormatter().setStyleName(rowIndex, colIndex, styleName);
    }

    /**
     * This method is used to add a style to a table cell
     *
     * @param table
     * @param rowIndex
     * @param colIndex
     * @param styleName
     */
    public static void addCellStyle(HTMLTable table, int rowIndex, int colIndex, String styleName) {
        table.getCellFormatter().addStyleName(rowIndex, colIndex, styleName);
    }

    /**
     * This method is used to set the style of label cells in a table
     */
    public static void setLabelCellStyle(HTMLTable table, int rowIndex) {
        setLabelCellStyle(table, rowIndex, 0);
    }

    public static void setLabelCellStyle(HTMLTable table, int rowIndex, int colIndex) {
        setCellStyle(table, rowIndex, colIndex, "prompt");
        addCellStyle(table, rowIndex, colIndex, "gridCell");
        addCellStyle(table, rowIndex, colIndex, "gridCellFullWidth");
        addCellStyle(table, rowIndex, colIndex, "gridCellTop");
    }

    /**
     * Shows or hides the given column
     *
     * @param table
     * @param column
     * @param visible
     */
    //TODO: make visiblilty an attribute of TableColumn so this doesn't have to be called AFTER refresh
    public static void setColumnVisible(HTMLTable table, int column, boolean visible) {
        try {
            for (int row = 0; row < table.getRowCount(); row++) {
                Element elem = table.getCellFormatter().getElement(row, column);
                if (elem == null) {
                    logger.error("TableUtils setColumnVisible elem (" + row + "," + column + ") was null");
                }
                else {
                    if (logger.isDebugEnabled())
                        logger.debug("TableUtils Setting (" + row + "," + column + ") visible to " + visible);
                    HTMLTable.setVisible(elem, visible);
                }
            }
        }
        catch (RuntimeException e) {
            logger.debug("TableUtils setColumnVisible caught excpetion " + e);
            throw e;
        }
    }

    /**
     * Shows or hides the given columns
     *
     * @param table
     * @param columns
     * @param visible
     */
    public static void setColumnsVisible(HTMLTable table, int[] columns, boolean visible) {
        if (columns != null && columns.length > 0) {
            for (int column : columns) {
                TableUtils.setColumnVisible(table, column, visible);
            }
        }
    }

    /**
     * This is used to create the links back on More Information panel in MooreDataPanel and PublicationPanelHelper
     *
     * @param externalLink
     * @return
     */
    public static Widget getLinkCell(Widget externalLink) {
        DockPanel panel = new DockPanel();
        String id = HTMLPanel.createUniqueId();
        HTMLPanel linkPanel = new HTMLPanel("<span class='greaterGreater'>&gt;&gt;&nbsp;</span><span id='" + id + "'></span>");
        linkPanel.add(externalLink, id);
        DOM.setStyleAttribute(linkPanel.getElement(), "display", "inline");
        DOM.setStyleAttribute(externalLink.getElement(), "display", "inline");
        panel.add(linkPanel, DockPanel.WEST);
        panel.add(new HTML("&nbsp;"), DockPanel.CENTER); // fill the rest of the row with blank space
        return panel;
    }
}
