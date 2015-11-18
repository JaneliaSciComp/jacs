
package org.janelia.it.jacs.compute.access;

/**
 * This class represents checked exceptions that are thrown from the Compute module
 *
 * @author Tareq Nabeel
 */
public class ComputeException extends Exception {

    /**
     * Construct a ComputeException with a descriptive message
     *
     * @param msg The string that describes the error
     */
    public ComputeException(String msg) {
        super(msg);
    }

    /**
     * Construct a ComputeException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public ComputeException(Throwable e) {
        super(e);
    }

    /**
     * Construct a ComputeException to wrap another exception.
     *
     * @param msg The string that describes the error
     * @param e   The exception to be wrapped.
     */
    public ComputeException(String msg, Throwable e) {
        super(msg, e);
    }


}
