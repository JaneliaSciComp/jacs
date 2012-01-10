
package org.janelia.it.jacs.web.gwt.admin.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Nov 2, 2006
 * Time: 4:25:16 PM
 */
public interface AdminServiceAsync {
    public void createUser(String login, String name, AsyncCallback callback);

    public void getDiskUsageReport(AsyncCallback asyncCallback);
}
