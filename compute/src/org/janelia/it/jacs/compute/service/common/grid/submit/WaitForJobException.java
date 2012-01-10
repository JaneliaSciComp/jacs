
package org.janelia.it.jacs.compute.service.common.grid.submit;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class WaitForJobException extends ServiceException {
    /**
     * Construct a WaitForJobException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public WaitForJobException(String msg) {
        super(msg);
    }

    /**
     * Construct a WaitForJobException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public WaitForJobException(Throwable e) {
        super(e);
    }

    /**
     * Construct a WaitForJobException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public WaitForJobException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a WaitForJobException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public WaitForJobException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a WaitForJobException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public WaitForJobException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a WaitForJobException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public WaitForJobException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
