
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 30, 2006
 * Time: 11:52:59 AM
 */
public class DaoException extends Exception {
    private static Logger logger = Logger.getLogger(DaoException.class);

    public DaoException(Exception e, String actionWhichProducedError) {
        super(actionWhichProducedError, e);
        logger.error("There was an exception performing - " + actionWhichProducedError + "\nOriginal exception: " +
                e.getStackTrace());
    }

    public DaoException(String actionWhichProducedError) {
        super(actionWhichProducedError);
        logger.error("There was an exception performing - " + actionWhichProducedError);
    }
}
