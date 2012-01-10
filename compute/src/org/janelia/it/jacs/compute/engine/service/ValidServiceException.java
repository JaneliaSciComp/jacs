
package org.janelia.it.jacs.compute.engine.service;

/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Sep 17, 2009
 * Time: 11:51:46 AM
 */
public class ValidServiceException extends ServiceException {

    /**
     * Construct a ValidServiceException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public ValidServiceException(String msg) {
        super(msg);
    }

    /**
     * Construct a ValidServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public ValidServiceException(Throwable e) {
        super(e);
    }

    /**
     * Construct a ValidServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public ValidServiceException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a ValidServiceException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error
     */
    public ValidServiceException(String msg, int errorCode) {
        super(msg, errorCode);
    }

    /**
     * Construct a ValidServiceException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error
     */
    public ValidServiceException(Throwable e, int errorCode) {
        super(e, errorCode);
    }

    /**
     * Construct a ValidServiceException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error
     */
    public ValidServiceException(String msg, Throwable e, int errorCode) {
        super(msg, e, errorCode);
    }
}
