
package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Michael Press
 */
public class GWTServiceException extends Exception implements IsSerializable {
    public GWTServiceException() {
        super();
    }

    public GWTServiceException(String message) {
        super(message);
    }

    public GWTServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public GWTServiceException(Throwable cause) {
        super(cause);
    }
}
