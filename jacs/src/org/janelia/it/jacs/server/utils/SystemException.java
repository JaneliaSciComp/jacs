
package org.janelia.it.jacs.server.utils;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 23, 2006
 * Time: 11:42:41 AM
 */
public class SystemException extends Exception {
    private static Logger logger = Logger.getLogger(SystemException.class);

    public SystemException(String errorMsg) {
        super(errorMsg);
        logger.error("SystemException - \n" + errorMsg);
    }

    public SystemException(Throwable originalException) {
        super(originalException.getMessage(), originalException);
        logger.error("SystemException", originalException);
    }
}
