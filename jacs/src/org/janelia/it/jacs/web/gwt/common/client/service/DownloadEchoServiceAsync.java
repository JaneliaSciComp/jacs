
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DownloadEchoServiceAsync {
    void postData(String data, AsyncCallback callback);
}
