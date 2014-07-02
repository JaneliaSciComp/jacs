package org.janelia.it.jacs.compute.process_result_validation;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * This captures error output from validations.  This captures what is wrong.
 * Created by fosterl on 6/27/14.
 */
public class ValidationLogger {
    // Refer to the header in interpreting the log.
    public static final String GENERAL_CATEGORY_EXCEPTION = "Something which threw an exception";
    private static final String VAL_LOG_HEADER = "Entity\tSample\tError\tCategory";
    private static final String VAL_LOG_FMT = "%d\t%d\t%s\t%s";
    private Logger internalLogger;
    private Set<String> categories;

    public ValidationLogger( Logger internalLogger ) {
        this.internalLogger = internalLogger;
        this.categories = new HashSet<>();
        this.categories.add( GENERAL_CATEGORY_EXCEPTION );
    }

    /**
     * Callers must add any category upon which an error is reported, here.  All categories being checked
     * should be recorded here.
     *
     * @param category something that will be checked.
     */
    public void addCategory( String category ) {
        categories.add( category );
    }

    /**
     * Report error from any validator.
     * A note about testCategory: this is enforced in a Set, because we wish to be able to easily see all things that
     * were checked, rather than assume because no error appeared, some particular condition never occurred.
     * Absence of evidence is not evidence of absence.
     *
     * @param sampleId the sample tree containnig the entity being checked.
     * @param entityId the GUID of the entity with failure.
     * @param testCategory what kind of test.
     * @param message description of failure.
     */
    public void reportError( Long sampleId, Long entityId, String testCategory, String message ) {
        if ( ! categories.contains( testCategory ) ) {
            throw new UnknownCategoryException( testCategory, sampleId, entityId, message );
        }
        String errorMesage = String.format( VAL_LOG_FMT, entityId, sampleId, message, testCategory );
        internalLogger.error( errorMesage );
    }

    /**
     * This is used to enforce the category constraint.
     */
    public static class UnknownCategoryException extends RuntimeException {
        private static final String MSG_FMT = "Unknown category %s while reporting message %s on sample %d's descendent %d.  Please register the category.";
        public UnknownCategoryException( String category, Long sampleId, Long entityId, String msg ) {
            super( String.format( MSG_FMT, category, sampleId, entityId, msg ) );
        }
    }
}
