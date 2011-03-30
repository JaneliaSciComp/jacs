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

import org.janelia.it.jacs.model.common.SortArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Michael Press
 */
public class CachedPagedDataRetriever implements PagedDataRetriever {

    static class CachedData {
        private List dataList;
        private SortArgument[] sortOptions;
        private int offset;
        private int length;

        CachedData() {
        }

        /**
         * The method checks whether the data is available in the cache
         *
         * @param sortArgs
         * @param from
         * @param n
         * @return
         */
        boolean isAvailable(SortArgument[] sortArgs, int from, int n) {
            boolean b = false;
            if (dataList != null && dataList.size() > 0 && from >= offset) {
                if (length < 0 ||
                        n >= 0 && from + n <= offset + dataList.size()) {
                    // the required range is cached
                    if (compareSortOptions(sortArgs)) {
                        // with the same sort options
                        b = true;
                    }
                }
            }
            return b;
        }

        void clear() {
            dataList = null;
            offset = -1;
            length = 0;
        }

        /**
         * The function retrieves the requested data from the cache assuming that isAvailable returned true
         *
         * @param from
         * @param n
         * @return
         */
        Object retrieveData(int from, int n) {
            ArrayList requiredDataList = new ArrayList();
            int nRecords = 0;
            for (int i = from - offset; ; i++) {
                if (n < 0) {
                    if (nRecords >= dataList.size()) {
                        break;
                    }
                }
                else {
                    if (nRecords >= dataList.size() || nRecords >= n) {
                        break;
                    }
                }
                requiredDataList.add(dataList.get(i));
                nRecords++;
            }
            return requiredDataList;
        }

        void setData(Object data, SortArgument[] sortOptions, int offset, int length) {
            // since we haven't standardized on listener's callback result to be a list or an array we consider both
            if (data instanceof List) {
                dataList = (List) data;
            }
            else {
                dataList = Arrays.asList((Object[]) data);
            }
            this.sortOptions = null;
            if (sortOptions != null) {
                this.sortOptions = new SortArgument[sortOptions.length];
                for (int i = 0; i < sortOptions.length; i++) {
                    this.sortOptions[i] = new SortArgument(sortOptions[i]);
                }
            }
            this.offset = offset;
            this.length = length;
        }

        private boolean compareSortOptions(SortArgument[] sortArgs) {
            boolean bresult = false;
            if (sortOptions == null || sortOptions.length == 0) {
                if (sortArgs == null || sortArgs.length == 0) {
                    bresult = true;
                }
            }
            else if (sortArgs != null) {
                if (sortOptions.length == sortArgs.length) {
                    bresult = true;
                    for (int i = 0; i < sortOptions.length; i++) {
                        if (!sortOptions[i].equals(sortArgs[i])) {
                            bresult = false;
                            break;
                        }
                    }
                }
            }
            return bresult;
        }
    }

    private int totalNumberOfRows;
    private int maxCacheSize;
    private PagedDataRetriever dataRetriever;
    private CachedData cachedData;

    public CachedPagedDataRetriever() {
    }

    public CachedPagedDataRetriever(PagedDataRetriever dataRetriever, int maxCacheSize) {
        this.dataRetriever = dataRetriever;
        this.maxCacheSize = maxCacheSize;
        totalNumberOfRows = -1;
        cachedData = new CachedData();
    }

    public void clear() {
        totalNumberOfRows = -1;
        cachedData.clear();
    }

    /**
     * Client must return the total number of available rows
     */
    public void retrieveTotalNumberOfDataRows(final DataRetrievedListener listener) {
        if (totalNumberOfRows != -1) {
            listener.onSuccess(totalNumberOfRows);
        }
        else {
            dataRetriever.retrieveTotalNumberOfDataRows(new DataRetrievedListener() {
                public void onSuccess(Object data) {
                    Integer numRows = (Integer) data;
                    if (numRows != null) {
                        totalNumberOfRows = numRows;
                    }
                    cachedData.clear();
                    listener.onSuccess(data);
                }

                public void onFailure(Throwable throwable) {
                    totalNumberOfRows = -1;
                    listener.onFailure(throwable);
                }

                public void onNoData() {
                    totalNumberOfRows = -1;
                    listener.onNoData();
                }
            });
        }
    }

    /**
     * Client returns the data for a given page
     */
    public void retrieveDataRows(int startIndex,
                                 int numRows,
                                 final SortArgument[] sortArgs,
                                 final DataRetrievedListener listener) {
        if (cachedData.isAvailable(sortArgs, startIndex, numRows)) {
            listener.onSuccess(cachedData.retrieveData(startIndex, numRows));
            return;
        }
        if (numRows >= 0 && numRows < maxCacheSize) {
            numRows = maxCacheSize;
        }
        if (totalNumberOfRows != -1 && numRows >= 0) {
            if (startIndex < 0) {
                startIndex = 0;
            }
            if (startIndex + numRows >= totalNumberOfRows) {
                numRows = -1;
            }
        }
        final int dataOffset = startIndex;
        final int dataLength = numRows;
        cachedData.clear();
        dataRetriever.retrieveDataRows(dataOffset, dataLength, sortArgs, new DataRetrievedListener() {
            public void onSuccess(Object data) {
                cachedData.setData(data, sortArgs, dataOffset, dataLength);
                listener.onSuccess(data);
            }

            public void onFailure(Throwable throwable) {
                listener.onFailure(throwable);
            }

            public void onNoData() {
                listener.onNoData();
            }
        });
    }
}
