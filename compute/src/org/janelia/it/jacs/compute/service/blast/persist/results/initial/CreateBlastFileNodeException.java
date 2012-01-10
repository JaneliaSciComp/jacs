
package org.janelia.it.jacs.compute.service.blast.persist.results.initial;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class CreateBlastFileNodeException extends ServiceException {
    /**
     * Construct a CreateBlastFileNodeException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public CreateBlastFileNodeException(String msg) {
        super(msg);
    }

    /**
     * Construct a CreateBlastFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateBlastFileNodeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a CreateBlastFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateBlastFileNodeException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a CreateBlastFileNodeException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public CreateBlastFileNodeException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateBlastFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateBlastFileNodeException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateBlastFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateBlastFileNodeException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
