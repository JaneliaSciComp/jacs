
package org.janelia.it.jacs.compute.service.common.grid.submit;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class SubmitJobException extends ServiceException {
    /**
     * Construct a SubmitJobException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public SubmitJobException(String msg) {
        super(msg);
    }

    /**
     * Construct a SubmitJobException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public SubmitJobException(Throwable e) {
        super(e);
    }

    /**
     * Construct a SubmitJobException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public SubmitJobException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a SubmitJobException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public SubmitJobException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a SubmitJobException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public SubmitJobException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a SubmitJobException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public SubmitJobException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
