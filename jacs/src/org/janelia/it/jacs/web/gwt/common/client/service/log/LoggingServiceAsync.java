
package org.janelia.it.jacs.web.gwt.common.client.service.log;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LoggingServiceAsync {
    //void log(int level, String message, AsyncCallback callback);
    //void log(int level, Throwable throwable, AsyncCallback callback);
    //void log(int level, String message, Throwable throwable, AsyncCallback callback);
    //TODO: remove
    void log(String message, AsyncCallback callback);

    void log(Throwable throwable, AsyncCallback callback);

    void log(String message, Throwable throwable, AsyncCallback callback);

    public void test(String message, AsyncCallback callback);

    public void stringTest(String message, AsyncCallback callback);

}
