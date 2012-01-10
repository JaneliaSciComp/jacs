
package org.janelia.it.jacs.web.gwt.common.server;

import org.janelia.it.jacs.web.gwt.common.client.service.log.LoggingService;

public class LoggingServiceImpl extends JcviGWTSpringController implements LoggingService {

    public void log(String msg) {
        log(msg, null);
    }

    public void log(Throwable throwable) {
        log(null, throwable);
    }

    public void log(String msg, Throwable throwable) {
        if (msg != null) {
            System.out.println(msg);
        }
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    public void test(String message) {
        log(message, null);
    }

    public String stringTest(String message) {
        log(message, null);
        return message;
    }

}
