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
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroup;
import org.janelia.it.jacs.web.gwt.common.client.ui.LinkGroupSelectionModel;
import org.janelia.it.jacs.web.gwt.common.client.ui.SelectionListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ControlImageBundle;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Press
 */
public class MultiSelectListTables extends HorizontalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.list.MultiSelectListTables");

    public static final String[] DEFAULT_CANDIDATE_ROW_COUNTS = new String[]{"5", "10", "25"};
    private static final int DEFAULT_VISIBLE_ROWS = 5;
    private static final ControlImageBundle imageBundle = ImageBundleFactory.getControlImageBundle();
    private AbstractImagePrototype left_arrow_enabled = imageBundle.getArrowLeftEnabledImage();
    private AbstractImagePrototype right_arrow_enabled = imageBundle.getArrowRightEnabledImage();

    private SortableTable _availableTable;
    private SortableTable _selectedTable;
    private ViewPort _availableTableViewPort;
    private ViewPort _selectedTableViewPort;
    private Map _tableToViewPort = new HashMap();
    private SelectionListener _listener;
    private LinkGroupSelectionModel _linkGroupSelectionModel;
    private String[] _candidateRowCounts;

    public MultiSelectListTables(String availableHeader, String selectedHeader) {
        this(availableHeader, selectedHeader, DEFAULT_VISIBLE_ROWS);
    }

    public MultiSelectListTables(String availableHeader, String selectedHeader, int defaultNumRows) {
        this(availableHeader, selectedHeader, defaultNumRows, DEFAULT_CANDIDATE_ROW_COUNTS);
    }

    /**
     * Constructor takes headers for both tables, and pagination row count increments.
     *
     * @param availableHeader    header for table with whatever the user has available for filtering.
     * @param selectedHeader     header for table of whatever user has selected for filtering.
     * @param candidateRowCounts next/prev page-numbers link labels.
     */
    public MultiSelectListTables(String availableHeader, String selectedHeader, int defaultNumRows, String[] candidateRowCounts) {
        super();
        _candidateRowCounts = candidateRowCounts;
        init(availableHeader, selectedHeader, defaultNumRows);
    }

    private void init(String availableHeader, String selectedHeader, int defaultNumRows) {
        _linkGroupSelectionModel = new LinkGroupSelectionModel();
        _linkGroupSelectionModel.setSelectedValue(defaultNumRows + "");

        add(createAvailableTable(availableHeader, "Click to select", defaultNumRows));
        add(HtmlUtils.getHtml("&nbsp;", "spacer"));
        add(createSelectedTable(selectedHeader, "Click to remove", defaultNumRows));
    }

    public Panel createAvailableTable(String header, String caption, int numRows) {
        _availableTable = createTable(header);
        _availableTable.addSelectionListener(new SwapTableListener());
        HTML rangeIndicator = new HTML();
        rangeIndicator.setStyleName("infoText");
        rangeIndicator.setHorizontalAlignment(HTML.ALIGN_LEFT);
        _availableTableViewPort = new ViewPort(_availableTable, rangeIndicator);
        _availableTableViewPort.setNumberOfVisibleRows(numRows);
        _tableToViewPort.put(_availableTable, _availableTableViewPort);
        return decorateTable(_availableTable, caption, rangeIndicator);
    }

    public Panel createSelectedTable(String header, String caption, int numRows) {
        _selectedTable = createTable(header);
        _selectedTable.addSelectionListener(new ReswapTableListener());

        HTML rangeIndicator = new HTML();
        rangeIndicator.setStyleName("infoText");
        _selectedTableViewPort = new ViewPort(_selectedTable, rangeIndicator);
        _selectedTableViewPort.setNumberOfVisibleRows(numRows);
        _tableToViewPort.put(_selectedTable, _selectedTableViewPort);
        return decorateTable(_selectedTable, caption, rangeIndicator);
    }

    private Panel decorateTable(SortableTable table, String caption, HTML rangeIndicator) {
        HTML captionHtml = HtmlUtils.getHtml(caption, "hint");
        captionHtml.setHorizontalAlignment(HTML.ALIGN_CENTER);
        ViewPort viewPort = (ViewPort) _tableToViewPort.get(table);
        HTML nextHTML = HtmlUtils.getHtml("next " + DEFAULT_VISIBLE_ROWS, "smallTextLink");
        HTML prevHTML = HtmlUtils.getHtml("prev " + DEFAULT_VISIBLE_ROWS, "smallTextLink");
        NextPrevClickListener nextPrevListener = new NextPrevClickListener(viewPort, nextHTML, prevHTML);
        nextHTML.addClickListener(nextPrevListener);
        prevHTML.addClickListener(nextPrevListener);

        // Make a panel for the top decorations (range and next/prev links)
        DockPanel topDecorationsPanel = new DockPanel();
        topDecorationsPanel.setStyleName("MultiSelectTopDecorationsPanel");
        topDecorationsPanel.setWidth("100%");
        topDecorationsPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        topDecorationsPanel.add(rangeIndicator, DockPanel.WEST);

        topDecorationsPanel.add(new HTML("&nbsp;&nbsp;&nbsp;"), DockPanel.CENTER);

        HorizontalPanel nextPrevPanel = new HorizontalPanel();
        topDecorationsPanel.add(nextPrevPanel, DockPanel.EAST);

        Image left_arrow = left_arrow_enabled.createImage();
        left_arrow.setStyleName("MultiSelectTableLeftArrow");
        nextPrevPanel.add(left_arrow);

        nextPrevPanel.add(prevHTML);
        nextPrevPanel.add(HtmlUtils.getHtml("&nbsp;|&nbsp;", "hint"));
        nextPrevPanel.add(nextHTML);

        Image right_arrow = right_arrow_enabled.createImage();
        right_arrow.setStyleName("MultiSelectTableRightArrow");
        nextPrevPanel.add(right_arrow);

        DockPanel footerPanel = new DockPanel();
        footerPanel.setWidth("100%");

        HorizontalPanel sizerLinkPanel = new HorizontalPanel();
        LinkGroup linkGroup = new LinkGroup(_candidateRowCounts, _linkGroupSelectionModel);
        _linkGroupSelectionModel.addSelectionListener(linkGroup);

        HTML[] rowNumberHTMLs = linkGroup.getGroupMembers();
        HTML promptText = new HTML("&nbsp;Show:&nbsp;&nbsp;");
        promptText.setStyleName("infoPrompt");
        sizerLinkPanel.add(promptText);
        for (int i = 0; i < rowNumberHTMLs.length; i++) {
            sizerLinkPanel.add(rowNumberHTMLs[i]);
            sizerLinkPanel.add(new HTML("&nbsp;"));
        }
        footerPanel.add(sizerLinkPanel, DockPanel.WEST);
        footerPanel.add(captionHtml, DockPanel.CENTER);
        footerPanel.add(HtmlUtils.getHtml("&nbsp;&nbsp;&nbsp;", "spacer"), DockPanel.EAST);
        VisibleRowsSelectionListener visibleRowsListener = new VisibleRowsSelectionListener(nextHTML, prevHTML);
        _linkGroupSelectionModel.addSelectionListener(visibleRowsListener);

        VerticalPanel panel = new VerticalPanel();
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        panel.add(topDecorationsPanel);
        panel.add(table);
        panel.add(footerPanel);

        return panel;
    }

    private SortableTable createTable(String header) {
        SortableTable table = new SortableTable();
        table.setEvenRowStyle("tableRowAll");
        table.setOddRowStyle("tableRowAll");
        table.addColumn(new TextColumn(header));
        table.setWidth("100%");
        return table;
    }

    /**
     * Assigns the list of values to put in the "available" list.  Any Object may be sent in the list - teh value
     * of Object.toString() is used to populate the table.
     */
    public void setAvailableList(List values) {
        _availableTableViewPort.setItemList(values);
    }

    public void setSelectedList(List values) {
        _selectedTableViewPort.setItemList(values);
    }

    public void addSelectionListener(SelectionListener listener) {
        _listener = listener;
    }

    /**
     * Swaps a value from one table to the other
     */
    private void swap(SortableTable fromTable, SortableTable toTable, int row, boolean onSelect) {
        //TODO: swap TableCells instead of values to support Widgets
        // Move the item from the clicked table to the other table
        ViewPort fromViewPort = (ViewPort) _tableToViewPort.get(fromTable);
        ViewPort toViewPort = (ViewPort) _tableToViewPort.get(toTable);

        String value = fromTable.getText(row, 0);
        try {
            fromViewPort.removeItem(value);
        }
        catch (Exception ex) {
            _logger.error(ex.getMessage() + ", on removeAvailableItem " + value, ex);
        }

        try {
            toViewPort.addItem(value);
        }
        catch (Exception ex) {
            _logger.error(ex.getMessage() + ", on addAvailableItem " + value, ex);
        }
        toTable.sortAsync(toTable.getSortColumns());
        notify(onSelect, value);
    }

    //TODO: support multiple listeners
    private void notify(boolean onSelect, String value) {
        // Notify listeners
        if (_listener != null) {
            if (onSelect)
                _listener.onSelect(value);
            else
                _listener.onUnSelect(value);
        }
    }

    public SortableTable getSelectedListBox() {
        return _selectedTable;
    }

    public List<String> getSelectedItems() {
        return _selectedTableViewPort.getItemList();
    }

    /**
     * Listener to push table through pages, on user clicks.
     */
    public class NextPrevClickListener implements ClickListener {
        private ViewPort _viewPort;
        private HTML _nextHTML;
        private HTML _prevHTML;

        public NextPrevClickListener(ViewPort viewPort, HTML nextHTML, HTML prevHTML) {
            _viewPort = viewPort;
            _nextHTML = nextHTML;
            _prevHTML = prevHTML;
        }

        public void onClick(Widget w) {
            if (w == _nextHTML) {
                _viewPort.setNext();
            }
            else if (w == _prevHTML) {
                _viewPort.setPrevious();
            }
        }
    }

    /**
     * Listener to change the next/prev links to accurately reflect new page size.
     */
    private class VisibleRowsSelectionListener implements SelectionListener {
        private HTML _nextHTML;
        private HTML _prevHTML;

        public VisibleRowsSelectionListener(HTML nextHTML, HTML prevHTML) {
            _nextHTML = nextHTML;
            _prevHTML = prevHTML;
        }

        public void onSelect(String value) {
            _availableTableViewPort.setNumberOfVisibleRows(Integer.parseInt(value));
            _selectedTableViewPort.setNumberOfVisibleRows(Integer.parseInt(value));
            _nextHTML.setText("next " + value);
            _prevHTML.setText("prev " + value);
        }

        public void onUnSelect(String value) {
            // Unused.
        }

    }

    /**
     * Listeners to carry out swaps between what is available for filtering and what has already
     * been selected.
     */
    private class SwapTableListener implements SelectionListener {
        public void onSelect(String row) {
            swap(_availableTable, _selectedTable, Integer.valueOf(row).intValue(), true);
        }

        public void onUnSelect(String row) {
        }
    }

    private class ReswapTableListener implements SelectionListener {
        public void onSelect(String row) {
            swap(_selectedTable, _availableTable, Integer.valueOf(row).intValue(), false);
        }

        public void onUnSelect(String row) {
        }
    }
}
