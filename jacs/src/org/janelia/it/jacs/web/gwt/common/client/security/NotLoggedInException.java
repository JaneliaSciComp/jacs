
package org.janelia.it.jacs.web.gwt.common.client.security;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author Michael Press
 */
public class NotLoggedInException extends Exception implements IsSerializable {
    private String _msg;

    public NotLoggedInException() {
        super();
    }

    public NotLoggedInException(String message) {
        super();
        _msg = message;
    }

    public String getMessage() {
        return _msg;
    }
}
