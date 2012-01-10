
package org.janelia.it.jacs.compute.service.blast.persist.results.finall;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class PersistBlastResultsException extends ServiceException {
    /**
     * Construct a PersistBlastResultsException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public PersistBlastResultsException(String msg) {
        super(msg);
    }

    /**
     * Construct a PersistBlastResultsException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistBlastResultsException(Throwable e) {
        super(e);
    }

    /**
     * Construct a PersistBlastResultsException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public PersistBlastResultsException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a PersistBlastResultsException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public PersistBlastResultsException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistBlastResultsException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistBlastResultsException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a PersistBlastResultsException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public PersistBlastResultsException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
