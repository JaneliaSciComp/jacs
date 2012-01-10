
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.RemoteService;

public interface DownloadEchoService extends RemoteService {
    public String postData(String data);
}
