package org.janelia.it.jacs.compute.process_result_validation;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * This captures error output from validations.  This captures what is wrong.
 * Created by fosterl on 6/27/14.
 */
public class ValidationLogger {
    // Refer to the header in interpreting the log.
    public static final String GENERAL_CATEGORY_EXCEPTION = "Something which threw an exception";
    public static final String MISSING = "Missing ";
    public static final String EMPTY = "Empty ";
    public static final String FILE_ERROR = "File Error ";
    public static final String MIN_SIZE = "Min Size ";
    private static final String VAL_LOG_HEADER = "Sample\tEntity\tEntityType\tError\tCategory";
    private static final String VAL_LOG_FMT = "%d\t%d\t%s\t%s\t%s";
    private Logger internalLogger;
    private Set<String> categories;
    private Map<String,Long> filePatternToMinSize;

    public ValidationLogger( Logger internalLogger ) {
        this.internalLogger = internalLogger;
        this.filePatternToMinSize = new HashMap<>();
        this.categories = new HashSet<>();
        this.categories.add( GENERAL_CATEGORY_EXCEPTION );
    }

    /**
     * Setup the minimum file size, given the filePattern.  Can then return for any validation.
     * @param filePattern ending of file name.  Expected to have been trimmed, not null, case sensitive.
     * @param minSize register this for broad use.
     */
    public void setMinSize( String filePattern, Long minSize ) {
        filePatternToMinSize.put(filePattern, minSize);
    }

    /**
     * Tells a validator how long files like this need to be in order to pass muster.
     *
     * @param filePattern ending of file name. Expected to have been trimmmed, not null, case sensitive.
     * @return
     */
    public Long getMinSize( String filePattern ) {
        Long rtnVal = filePatternToMinSize.get(filePattern);
        if ( rtnVal == null ) {
            rtnVal = 0L;
        }
        return rtnVal;
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
     * @param owningEntityType what kind of entity is being checked.
     * @param entityId the GUID of the entity with failure.
     * @param testCategory what kind of test.
     * @param message description of failure.
     */
    public void reportError( Long sampleId, Long entityId, String owningEntityType, String testCategory, String message ) {
        if ( ! categories.contains( testCategory ) ) {
            throw new UnknownCategoryException( testCategory, sampleId, entityId, message );
        }
        String errorMesage = String.format( VAL_LOG_FMT, sampleId, entityId, owningEntityType, message, testCategory );
        internalLogger.error( errorMesage );
    }

    /**
     * This is used to enforce the category constraint.
     */
    public static class UnknownCategoryException extends RuntimeException {
        private static final String MSG_FMT = "Unknown category %s while reporting message %s on sample %d's descendent %s.  Please register the category.";
        public UnknownCategoryException( String category, Long sampleId, Long entityId, String msg ) {
            super( String.format( MSG_FMT, category, sampleId, entityId, msg ) );
        }
    }
}
