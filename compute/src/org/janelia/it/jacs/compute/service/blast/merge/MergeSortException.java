
package org.janelia.it.jacs.compute.service.blast.merge;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class MergeSortException extends ServiceException {

    /**
     * Construct a MergeSortException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public MergeSortException(String msg) {
        super(msg);
    }

    /**
     * Construct a MergeSortException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public MergeSortException(Throwable e) {
        super(e);
    }

    /**
     * Construct a MergeSortException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public MergeSortException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a MergeSortException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public MergeSortException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a MergeSortException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public MergeSortException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a MergeSortException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public MergeSortException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
