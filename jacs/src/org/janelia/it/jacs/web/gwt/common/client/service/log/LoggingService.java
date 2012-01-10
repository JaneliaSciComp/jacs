
package org.janelia.it.jacs.web.gwt.common.client.service.log;

import com.google.gwt.user.client.rpc.RemoteService;

public interface LoggingService extends RemoteService {
    public void log(String message);

    public void log(Throwable throwable);

    public void log(String message, Throwable throwable);

    public void test(String message);

    public String stringTest(String message);

}
