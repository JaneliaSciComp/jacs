
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * @author Tareq Nabeel
 */
public class DaoException extends ComputeException {
    private static Logger logger = Logger.getLogger(DaoException.class);

    /**
     * Construct a ServiceException with a descriptive String
     *
     * @param msg The string that describes the error
     */
    public DaoException(String msg) {
        super(msg);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public DaoException(Throwable e) {
        super(e);
    }

    /**
     * Construct a ServiceException to wrap another exception.
     *
     * @param e The exception to be wrapped.
     */
    public DaoException(String msg, Throwable e) {
        super(msg, e);
    }

    public DaoException(Exception e, String actionWhichProducedError) {
        super(actionWhichProducedError, e);
        logger.error("There was an exception performing - " + actionWhichProducedError + "\nOriginal exception: " +
                Arrays.toString(e.getStackTrace()));
    }

}
