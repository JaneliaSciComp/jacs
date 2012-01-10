
package org.janelia.it.jacs.compute.engine.data;

import org.janelia.it.jacs.compute.api.ComputeException;

/**
 * @author Tareq Nabeel
 */
public class MissingDataException extends ComputeException {

    public MissingDataException(String msg) {
        super(msg);
    }
}
