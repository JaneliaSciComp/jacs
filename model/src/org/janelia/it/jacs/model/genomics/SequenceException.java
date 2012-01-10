
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Jan 5, 2007
 * Time: 11:34:47 AM
 */
public class SequenceException extends RuntimeException implements IsSerializable {

    public SequenceException() {
    }

    public SequenceException(String message) {
        super(message);
    }
}
