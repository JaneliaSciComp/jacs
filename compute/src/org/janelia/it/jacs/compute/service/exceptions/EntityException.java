package org.janelia.it.jacs.compute.service.exceptions;

import org.janelia.it.jacs.compute.access.ComputeException;

/**
 * Indicates a problem having to do with entities.  
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityException extends ComputeException {

    public EntityException(String msg) {
        super(msg);
    }

    public EntityException(Throwable e) {
        super(e);
    }

    public EntityException(String msg, Throwable e) {
        super(msg, e);
    }
}
