
package org.janelia.it.jacs.web.gwt.detail.client.service.protein;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 10:14:14 PM
 */
public interface ProteinDetailServiceAsync {
    void getProteinClusterInfo(String proteinAcc, AsyncCallback callback);

    void getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs, AsyncCallback callback);
}
