
package org.janelia.it.jacs.compute.service.blast.persist.query;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class PersistQNodeException extends ServiceException {

    public static final int USER_DOES_NOT_EXIST = 1;

    /**
     * Construct a PersistQNodeException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public PersistQNodeException(String msg) {
        super(msg);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistQNodeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistQNodeException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a PersistQNodeException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistQNodeException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistQNodeException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
