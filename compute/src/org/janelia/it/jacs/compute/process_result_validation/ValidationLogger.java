package org.janelia.it.jacs.compute.process_result_validation;

import org.apache.log4j.Logger;

/**
 * This captures error output from validations.  This captures what is wrong.
 * Created by fosterl on 6/27/14.
 */
public class ValidationLogger {
    private Logger internalLogger;
    public ValidationLogger( Logger internalLogger ) {
        this.internalLogger = internalLogger;
    }
    public void reportError( String message ) {
        internalLogger.error( message);
    }
}
