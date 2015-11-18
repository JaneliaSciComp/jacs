
package org.janelia.it.jacs.compute.engine.data;

import org.janelia.it.jacs.compute.access.ComputeException;

/**
 * @author Tareq Nabeel
 */
public class MissingDataException extends ComputeException {

    public MissingDataException(String msg) {
        super(msg);
    }
    public MissingDataException(String msg, Throwable e) {
        super(msg, e);
    }

}
