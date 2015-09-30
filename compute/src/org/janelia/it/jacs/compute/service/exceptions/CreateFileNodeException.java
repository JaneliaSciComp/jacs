
package org.janelia.it.jacs.compute.service.exceptions;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class CreateFileNodeException extends ServiceException {
    /**
     * Construct a CreateFileNodeException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public CreateFileNodeException(String msg) {
        super(msg);
    }

    /**
     * Construct a CreateFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateFileNodeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a CreateFileNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateFileNodeException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a CreateFileNodeException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public CreateFileNodeException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateFileNodeException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateFileNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateFileNodeException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
