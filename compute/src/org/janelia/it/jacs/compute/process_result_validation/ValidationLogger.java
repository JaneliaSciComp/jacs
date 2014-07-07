package org.janelia.it.jacs.compute.process_result_validation;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.util.*;

/**
 * This captures error output from validations.  This captures what is wrong.
 * Created by fosterl on 6/27/14.
 */
public class ValidationLogger implements Closeable {
    // Refer to the header in interpreting the log.
    public static final Category GENERAL_CATEGORY_EXCEPTION = new Category("Something which threw an exception");
    public static final String MISSING = "Missing ";
    public static final String EMPTY = "Empty ";
    public static final String FILE_ERROR = "File Error ";
    public static final String MIN_SIZE = "Min Size ";
    private static final String VAL_LOG_HEADER = "Sample\tEntity\tEntityType\tError\tCategory";
    private static final String VAL_LOG_FMT = "%d\t%d\t%s\t%s\t%s";
    private Logger internalLogger;
    private Map<Category,List<ReportData>> categoryListMap;
    private Map<String,Long> filePatternToMinSize;

    public ValidationLogger( Logger internalLogger ) {
        this.internalLogger = internalLogger;
        this.filePatternToMinSize = new HashMap<>();
        this.categoryListMap = new TreeMap<>();
        addCategory(GENERAL_CATEGORY_EXCEPTION);
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
        addCategory(new Category(category));
    }

    public void addCategory( Category category ) {
        categoryListMap.put(category, new ArrayList<ReportData>());
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
    public void reportError( Long sampleId, Long entityId, String owningEntityType, Category testCategory, String message ) {
        if ( ! categoryListMap.containsKey(testCategory) ) {
            throw new UnknownCategoryException( testCategory, sampleId, entityId, message );
        }
        ReportData reportData = new ReportData();
        reportData.setCategory( testCategory );
        reportData.setEntityId(entityId);
        reportData.setSampleId(sampleId);
        reportData.setEntityType(owningEntityType);
        reportData.setMessage( message );
        categoryListMap.get( testCategory ).add( reportData );
    }

    /**
     * This is the final closure.  Do not try and report any errors or add categories after this.
     */
    @Override
    public void close() {
        internalLogger.info("Category List Follows:");
        for ( Category category: categoryListMap.keySet() ) {
            internalLogger.info("CATEGORY: " + category);
            internalLogger.info( VAL_LOG_HEADER );
            for ( ReportData reportData: categoryListMap.get( category ) ) {
                String errorMesage = String.format(
                        VAL_LOG_FMT, reportData.getSampleId(), reportData.getEntityId(), reportData.getEntityType(), reportData.getMessage(), reportData.getCategory()
                );
                internalLogger.error( errorMesage );
            }
        }

        categoryListMap = null;

    }

    /**
     * This is used to enforce the category constraint.
     */
    public static class UnknownCategoryException extends RuntimeException {
        private static final String MSG_FMT = "Unknown category %s while reporting message %s on sample %d's descendent %s.  Please register the category.";
        private UnknownCategoryException( Category category, Long sampleId, Long entityId, String msg ) {
            super( String.format( MSG_FMT, category, sampleId, entityId, msg ) );
        }
    }

    /**
     * Category is constructed/used largely like exceptions are used: seed it with a string, and use it like a marker.
     */
    public static class Category implements Comparable<Category> {
        private String categoryText;
        public Category( String categoryText ) {
            this.categoryText = categoryText;
        }

        @Override
        public String toString() { return categoryText; }

        /**
         * This o'ride liberally is true for anything whose to-string is also equal.
         *
         * @param o some other object--probably another Category.
         * @return true if to-string between this and other match.
         */
        @Override
        public boolean equals( Object o ) {
            if ( o == null ) {
                return false;
            }
            return o.toString().equals( toString() );
        }

        @Override
        public int hashCode() { return toString().hashCode(); }

        @Override
        public int compareTo(Category o) {
            if ( o == null ) {
                return Integer.MAX_VALUE;
            }
            return categoryText.compareTo( o.toString() );
        }
    }

    public static class ReportData {
        private Category category;
        private Long sampleId;
        private Long entityId;
        private String entityType;
        private String message;

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Long getSampleId() {
            return sampleId;
        }

        public void setSampleId(Long sampleId) {
            this.sampleId = sampleId;
        }

        public Long getEntityId() {
            return entityId;
        }

        public void setEntityId(Long entityId) {
            this.entityId = entityId;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
