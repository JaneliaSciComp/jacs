
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import org.janelia.it.jacs.model.common.SortArgument;

/**
 * @author Michael Press
 */
public interface PagedDataRetriever {
    /**
     * Client must return the total number of available rows
     */
    public void retrieveTotalNumberOfDataRows(DataRetrievedListener listener);

    /**
     * Client returns the data for a given page
     */
    public void retrieveDataRows(int startIndex, int numRows, SortArgument[] sortArgument, DataRetrievedListener listener);
}
