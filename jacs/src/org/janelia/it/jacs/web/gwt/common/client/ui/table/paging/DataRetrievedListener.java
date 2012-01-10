
package org.janelia.it.jacs.web.gwt.common.client.ui.table.paging;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * @author Michael Press
 *         <p/>
 *         This object is used in order to notify that the data retrieval
 *         process has finished
 */
public interface DataRetrievedListener extends IsSerializable, Serializable {
    public void onSuccess(Object data);

    public void onFailure(Throwable throwable);

    public void onNoData();
}
