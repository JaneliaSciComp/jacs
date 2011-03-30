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

package org.janelia.it.jacs.web.gwt.common.client.ui.paging;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cristian Goina
 */
public class SimplePaginator {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.ui.paging.SimplePaginator");

    private PagedDataRetriever dataRetriever;
    private SortableColumn[] sortableFields; // value to pass to data retriever to specify current column to sort on
    private SortableColumn[] sortOptions;
    private List _dataChangedListeners = new ArrayList();// called when new data has just been retrieved
    private List pagedData;
    private int currentOffset;
    private int pageSize;
    private int totalCount;
    private boolean initialized;

    public SimplePaginator() {
    }

    public SimplePaginator(PagedDataRetriever dataRetriever,
                           int pageSize,
                           String[][] sortableFields) {
        this.dataRetriever = dataRetriever;
        this.pageSize = pageSize;
        currentOffset = -1;
        totalCount = -1;
        pagedData = new ArrayList();
        initialized = false;
        if (sortableFields != null) {
            this.sortableFields = new SortableColumn[sortableFields.length];
            for (int i = 0; i < sortableFields.length; i++) {
                this.sortableFields[i] = new SortableColumn(i, sortableFields[i][1], sortableFields[i][0]);
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public SortableColumn[] getAllSortableFields() {
        return sortableFields;
    }

    public void first() {
        if (currentOffset != 1) {
            currentOffset = 1;
            retrieveDataRows(currentOffset);
        }
    }

    public void next() {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Invalid page size");
        }
        if (currentOffset > 0) {
            if (currentOffset + pageSize <= totalCount) {
                currentOffset += pageSize;
                retrieveDataRows(currentOffset);
            }
        }
        else {
            currentOffset = 1;
            retrieveDataRows(currentOffset);
        }
    }

    public void previous() {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Invalid page size");
        }
        if (currentOffset > 0) {
            int offset = currentOffset - pageSize;
            if (offset < 1) {
                offset = 1;
            }
            if (offset < currentOffset) {
                currentOffset = offset;
                retrieveDataRows(currentOffset);
            }
        }
    }

    public void last() {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Invalid page size");
        }
        if (totalCount < 0) {
            invokeRetrieveNumDataRows(new DataRetrievedListener() {
                public void onSuccess(Object data) {
                    totalCount = ((Integer) data).intValue();
                    currentOffset = determineLastPageOffset();
                    if (currentOffset > 0) {
                        retrieveDataRows(currentOffset);
                    }
                }

                public void onFailure(Throwable throwable) {
                    throw new IllegalArgumentException("Error while computing the number of records");
                }

                public void onNoData() {
                    throw new IllegalArgumentException("Illegal value for the record count");
                }
            });
        }
        else {
            int offset = determineLastPageOffset();
            if (offset != currentOffset) {
                currentOffset = offset;
                retrieveDataRows(currentOffset);
            }
        }
    }

    public boolean hasData() {
        return pagedData.size() > 0;
    }

    public boolean hasNext() {
        if (currentOffset > 0) {
            return currentOffset + pageSize <= totalCount;
        }
        else {
            return false;
        }
    }

    public boolean hasPrevious() {
        if (currentOffset > 0) {
            return currentOffset > 1;
        }
        else {
            return false;
        }
    }

    public void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Invalid page size value: " + pageSize);
        }
        this.pageSize = pageSize;
        if (currentOffset > 0 && currentOffset + pageSize > totalCount) {
            currentOffset = totalCount - pageSize + 1;
            if (currentOffset < 1) {
                currentOffset = 1;
            }
        }
        if (hasData()) {
            refresh();
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * sets the 1-based record offset
     *
     * @param currentOffset
     */
    public void setCurrentOffset(int currentOffset) {
        this.currentOffset = currentOffset;
    }

    /**
     * @return the 1-based offset of the first record from the page
     */
    public int getCurrentOffset() {
        return currentOffset;
    }

    /**
     * @return the 1-based offset of the last record from the page
     */
    public int getEndPageOffset() {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Invalid page size");
        }
        int endPageOffset = -1;
        if (totalCount >= 0) {
            if (currentOffset + pageSize <= totalCount) {
                endPageOffset = currentOffset + pageSize - 1;
            }
            else {
                endPageOffset = totalCount;
            }
        }
        return endPageOffset;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List getPageData() {
        return pagedData;
    }

    public void refresh() {
        pagedData.clear();
        invokeRetrieveNumDataRows(new DataRetrievedListener() {
            public void onSuccess(Object data) {
                int newCount = ((Integer) data).intValue();
                if (newCount != totalCount) {
                    // the number of records has changed
                    // so we have to recalculate the current offset
                    totalCount = newCount;
                    int lastPageOffset = determineLastPageOffset();
                    if (currentOffset <= 0) {
                        currentOffset = 1;
                    }
                    else if (currentOffset > lastPageOffset) {
                        currentOffset = lastPageOffset;
                    }
                }
                retrieveDataRows(currentOffset);
            }

            public void onFailure(Throwable throwable) {
                throw new IllegalArgumentException("Error while computing the number of records");
            }

            public void onNoData() {
                throw new IllegalArgumentException("Illegal value for the record count");
            }
        });
    }

    public void clear() {
        pagedData.clear();
        currentOffset = -1;
        initialized = false;
        fireDataChangedEvent(null, null);
    }

    public void setSortOptions(SortableColumn[] sortOptions) {
        if (sortOptions != null) {
            for (int i = 0; i < sortOptions.length; i++) {
                sortOptions[i].setSortArgumentName(getSortArgumentName(sortOptions[i].getColumnHeading(),
                        sortOptions[i].getColumnPosition()));
            }
        }
        this.sortOptions = sortOptions;
        // for now I assume that if the sort options are non null
        // something has changed
        if (this.sortOptions != null && this.sortOptions.length > 0 && hasData()) {
            refresh();
        }
    }

    public void addDataChangedListener(DataRetrievedListener listener) {
        _dataChangedListeners.add(listener);
    }

    public void removeDataChangedListener(DataRetrievedListener listener) {
        _dataChangedListeners.add(listener);
    }

    protected int determineLastPageOffset() {
        if (totalCount < 0) {
            return -1;
        }
        else {
            return totalCount == 0 ? 0 : ((totalCount - 1) / pageSize) * pageSize + 1;
        }
    }

    private void fireDataChangedEvent(List data, Throwable error) {
        for (int i = 0; i < _dataChangedListeners.size(); i++) {
            DataRetrievedListener dataRetrievedCB = (DataRetrievedListener) _dataChangedListeners.get(i);
            if (error != null) {
                dataRetrievedCB.onFailure(error);
            }
            else if (data == null || data.size() == 0) {
                dataRetrievedCB.onNoData();
            }
            else {
                dataRetrievedCB.onSuccess(data);
            }
        }
    }

    private String getSortArgumentName(String columnHeading, int colPos) {
        String sortArgName = null;
        if (sortableFields != null) {
            if (colPos >= 0 && colPos < sortableFields.length) {
                sortArgName = sortableFields[colPos].getSortArgumentName();
            }
            else if (columnHeading != null && columnHeading.length() > 0) {
                // go through the sortable fields and see if there's any that matches the heading
                for (int i = 0; i < sortableFields.length; i++) {
                    if (columnHeading.equals(sortableFields[i].getColumnHeading())) {
                        sortArgName = sortableFields[i].getSortArgumentName();
                        break;
                    }
                }
            }
        }
        return sortArgName;
    }

    /**
     * Retrieve the total number of data rows for the range indicator
     * if the rowIndex is greater than or equal to 0 then the completion of row num retrieval
     * triggers a data retrieval
     */
    private void invokeRetrieveNumDataRows(DataRetrievedListener countRetrievedListener) {
        _logger.debug("Retrieving total number of data rows ");
        dataRetriever.retrieveTotalNumberOfDataRows(countRetrievedListener);
    }

    /**
     * Retrieve one page of data
     * precondition totalCount is set
     */
    private void invokeRetrieveDataRows(final int oneBasedRowIndex) {
        int rowsToRetrieve = getPageSize();
        if (totalCount > 0 && (oneBasedRowIndex - 1 + rowsToRetrieve > totalCount)) {
            rowsToRetrieve = totalCount - oneBasedRowIndex + 1;
        }
        if (_logger.isDebugEnabled()) {
            String orderBy = "";
            if (sortOptions != null) {
                StringBuffer orderByBuffer = new StringBuffer();
                for (int i = 0; i < sortOptions.length; i++) {
                    orderByBuffer.append(sortOptions[i].toString());
                    if (i + 1 < sortOptions.length) {
                        orderByBuffer.append(',');
                    }
                }
                if (orderByBuffer.length() > 0) {
                    orderBy = " order by " + orderByBuffer.toString();
                }
            }
            _logger.debug("Retrieving data rows " +
                    oneBasedRowIndex + " - " + (oneBasedRowIndex + rowsToRetrieve - 1) + orderBy);
        }
        dataRetriever.retrieveDataRows(oneBasedRowIndex - 1, rowsToRetrieve, sortOptions,
                new DataRetrievedListener() {
                    public void onSuccess(Object data) {
                        List listData = (List) data;
                        if (_logger.isDebugEnabled()) {
                            _logger.debug("SimplePaginator: DataRetreiver returned " + listData.size() + " tableRows");
                        }
                        if (listData != null) { // this is ok, there might actually be no data
                            pagedData = listData;
                        }
                        else {
                            pagedData.clear();
                        }
                        if (!initialized) {
                            initialized = true;
                        }
                        // as well as the other data retrieval listeners
                        fireDataChangedEvent(listData, null);
                    }

                    public void onFailure(Throwable throwable) {
                        _logger.error("RemotingPaginator.retrieveDataRows().onFailure()", throwable);
                        fireDataChangedEvent(null, throwable);
                    }

                    public void onNoData() {
                        _logger.debug("RemotingPaginator: DataRetreiver returned 0 rows");
                        fireDataChangedEvent(null, null);
                    }

                });
    }

    /**
     * Retrieve one page of data
     */
    private void retrieveDataRows(final int oneBasedRowIndex) {
        // Ignore retrieval requests that occur during init;
        // they're due to the GUI setup and won't result in data retrievals
        if (totalCount < 0) {
            // If total number of rows has not been retrieved, get it now
            // and once that completes retrieve the row data
            invokeRetrieveNumDataRows(new DataRetrievedListener() {
                public void onSuccess(Object data) {
                    Integer numRows = (Integer) data;
                    if (numRows == null) {
                        _logger.error("Got null numRows");
                    }
                    else {
                        _logger.debug("SimplePaginator: DataRetriever says total row count is " + numRows);
                        totalCount = numRows.intValue();
                        if (oneBasedRowIndex > 0) {
                            invokeRetrieveDataRows(oneBasedRowIndex);
                        }
                    }
                }

                public void onFailure(Throwable throwable) {
                    _logger.error("SimplePaginator.retrieveTotalNumberOfDataRows().onFailure()", throwable);
                }

                /** Client might not know the total yet */
                public void onNoData() {
                }

            });
        }
        else {
            invokeRetrieveDataRows(oneBasedRowIndex);
        }
    }

}
