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
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;

/**
 * @author Michael Press
 */
public class RemotePagingPanel extends PagingPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.RemotePagingPanel");

    //TODO: push PagingController up to PagingPanel
    private RemotePagingController _pagingController;     // coordinates PagingPanel, Paginator and TableController

    public RemotePagingPanel() {
    }

    public RemotePagingPanel(SortableTable table, RemotingPaginator paginator, String rowsPerPagePreferenceKey) {
        this(table,
                paginator,
                false,
                0,
                PagingPanel.DEFAULT_VISIBLE_ROWS,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table, RemotingPaginator paginator, int defaultRowsPerPage, String rowsPerPagePreferenceKey) {
        this(table,
                paginator,
                false,
                0,
                defaultRowsPerPage,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table,
                             RemotingPaginator paginator,
                             boolean includeTableFooter,
                             int createAdvancedSortLink,
                             int defaultRowsPerPage,
                             String rowsPerPagePreferenceKey) {
        this(table,
                DEFAULT_ROWS_PER_PAGE_OPTIONS,
                false,
                DEFAULT_CREATE_PAGE_LOADING_LABEL,
                paginator,
                includeTableFooter,
                createAdvancedSortLink,
                defaultRowsPerPage,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table, RemotingPaginator paginator, String[] rowsPerPageOptions,
                             int defaultRowsPerPage, String rowsPerPagePreferenceKey) {
        this(table,
                rowsPerPageOptions,
                false,
                DEFAULT_CREATE_PAGE_LOADING_LABEL,
                paginator,
                false,
                0,
                defaultRowsPerPage,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table, RemotingPaginator paginator, String[] rowsPerPageOptions,
                             String rowsPerPagePreferenceKey) {
        this(table,
                rowsPerPageOptions,
                false,
                DEFAULT_CREATE_PAGE_LOADING_LABEL,
                paginator,
                false,
                0,
                PagingPanel.DEFAULT_VISIBLE_ROWS,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table,
                             String[] rowsPerPageOptions,
                             boolean includeScrolling,
                             boolean createLoadingPageLabel,
                             RemotingPaginator paginator,
                             boolean includeTableFooter,
                             int createAdvancedSortLink,
                             String rowsPerPagePreferenceKey) {
        this(table,
                rowsPerPageOptions,
                includeScrolling,
                createLoadingPageLabel,
                paginator,
                includeTableFooter,
                createAdvancedSortLink,
                PagingPanel.DEFAULT_VISIBLE_ROWS,
                rowsPerPagePreferenceKey);
    }

    public RemotePagingPanel(SortableTable table,
                             String[] rowsPerPageOptions,
                             boolean includeScrolling,
                             boolean createLoadingPageLabel,
                             RemotingPaginator paginator,
                             boolean includeTableFooter,
                             int createAdvancedSortLink,
                             int defaultRowsPerPage,
                             String rowsPerPagePreferenceKey) {
        super(table,
                rowsPerPageOptions,
                defaultRowsPerPage,
                includeScrolling,
                createLoadingPageLabel,
                paginator,
                includeTableFooter,
                createAdvancedSortLink,
                rowsPerPagePreferenceKey);
        _pagingController = new RemotePagingController(table, this, (RemotingPaginator) getPaginator());
        table.setTableController(_pagingController);

        //TODO: move this to the PagingController
        // We need to be notified to update the controls after the RemotePaginator retrieves the data
        ((RemotingPaginator) getPaginator()).addDataRetrievedCallback(new PageDataRetrievedListener());
        ((RemotingPaginator) getPaginator()).addNumRowsRetrievedCallback(new NumRowsRetrievedListener());
    }

    public RemotingPaginator getRemotePaginator() {
        return (RemotingPaginator) getPaginator();
    }

    public RemotePagingController getRemotePagingController() {
        return _pagingController;
    }

    /**
     * Same as superclass except updateControls() not called (paginator will call after remote data load)
     */
    protected void next() {
        if (getPaginator().hasNext()) {
            showLoadingLabel();
            getPaginator().next();
        }
    }

    /**
     * Same as superclass except updateControls() not called (paginator will call after remote data load)
     */
    protected void previous() {
        if (getPaginator().hasPrevious()) {
            showLoadingLabel();
            getPaginator().previous();
        }
    }

    /**
     * Same as superclass except updateControls() not called (paginator will call after remote data load)
     */
    public void first() {
        showLoadingLabel();
        getPaginator().first();
    }

    public void removeRow(TableRow tableRow) {
        showLoadingLabel();
        getPaginator().removeRow(tableRow);
    }

    /**
     * Same as superclass except updateControls() not called (paginator will call after remote data load)
     */
    protected void last() {
        showLoadingLabel();
        getPaginator().last();
    }

    /**
     * User has changed the number of items to view, so inform the RemotePaginator in case it needs to retrieve more data
     */
    protected void modifyRowsPerPage(int size) {
        _logger.debug("RemotePagingPanel.modifyRowsPerPage(" + size + ")");
        showLoadingLabel();
        getPaginator().modifyRowsPerPage(size);
        ((RemotingPaginator) getPaginator()).gotoRow(getPaginator().getCurrentOffset());
        updateControls();
    }

    /**
     * This is our callback from the data retriever that the total number of rows has been retrieved,
     * so we need to update the range indicator.
     */
    public class NumRowsRetrievedListener implements DataRetrievedListener {
        public NumRowsRetrievedListener() {
        }

        public void onSuccess(Object ignored) {
            _logger.debug("RemotePagingPanel.NumRowsRetrievedListener.onSuccess()");
            updateRangeIndicator();
        }

        public void onFailure(Throwable throwable) {
            hideLoadingLabel();
            getSortableTable().setError("Error retrieving the number of records: " + throwable.getMessage());
        }

        public void onNoData() {
        }
    }

    /**
     * This is our callback from the data retriever that the next page of data has been retrieved and set in the table,
     * so we need to update the paging controls to reflect the new state and turn off the loading label.
     */
    public class PageDataRetrievedListener implements DataRetrievedListener {
        public PageDataRetrievedListener() {
        }

        public void onSuccess(Object ignored) {
            _logger.debug("RemotePagingPanel.PageDataRetrievedListener.onSuccess()");
            updateControls();
            hideLoadingLabel();
            if (!getPaginator().hasData())
                getSortableTable().setNoData(getNoDataMessage());
        }

        public void onFailure(Throwable throwable) {
            displayErrorMessage("Error retrieving the data" +
                    (throwable.getMessage() != null ? ": " + throwable.getMessage() : ""));
        }

        public void onNoData() {
            updateControls();
            hideLoadingLabel();
        }
    }
}
