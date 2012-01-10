
package org.janelia.it.jacs.compute.service.blast.createtask;

import org.janelia.it.jacs.compute.engine.service.ServiceException;

/**
 * @author Tareq Nabeel
 */
public class CreateTaskException extends ServiceException {

    public static final int USER_DOES_NOT_EXIST = 1;

    /**
     * Construct a CreateTaskException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public CreateTaskException(String msg) {
        super(msg);
    }

    /**
     * Construct a CreateTaskException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateTaskException(Throwable e) {
        super(e);
    }

    /**
     * Construct a CreateTaskException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public CreateTaskException(String msg, Throwable e) {
        super(msg, e);
    }


    /**
     * Construct a CreateTaskException with a descriptive String
     *
     * @param msg       The string that describes the error
     * @param errorCode more description on the error for possible special handling
     */
    public CreateTaskException(String msg, int errorCode) {
        super(msg);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateTaskException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateTaskException(Throwable e, int errorCode) {
        super(e);
        setErrorCode(errorCode);
    }

    /**
     * Construct a CreateTaskException to wrap another exception.
     *
     * @param e         The exception to be wrapped.
     * @param errorCode more description on the error for possible special handling
     */
    public CreateTaskException(String msg, Throwable e, int errorCode) {
        super(msg, e);
        setErrorCode(errorCode);
    }
}
