
package org.janelia.it.jacs.model.vo;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 25, 2006
 * Time: 4:23:14 PM
 */
public class ParameterException extends Exception implements Serializable, IsSerializable {
    public ParameterException() {
    }

    public ParameterException(String message) {
        super(message);
    }

    public ParameterException(Exception originalException) {
        super(originalException.getMessage(), originalException);
    }
}
