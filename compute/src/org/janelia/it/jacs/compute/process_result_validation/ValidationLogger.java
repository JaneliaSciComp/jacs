package org.janelia.it.jacs.compute.process_result_validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;

import java.io.Closeable;
import java.io.PrintWriter;
import java.util.*;

/**
 * This captures error output from validations.  This captures what is wrong.
 * Created by fosterl on 6/27/14.
 */
public class ValidationLogger implements Closeable {
    public enum Status { Success, Failure, Unfinished, }

    // Refer to the header in interpreting the log.
    public static final Category GENERAL_CATEGORY_EXCEPTION = new Category("Something which threw an exception");
    public static final String MISSING = "Missing ";
    public static final String EMPTY = "Empty ";
    public static final String FILE_ERROR = "File Error ";
    public static final String MIN_SIZE = "Min Size ";
    private static final String SECTION_BREAK = "\n\n----------------------------------------------------------\n";
    private static final String VAL_LOG_HEADER = "Sample\tEntity\tEntityType\tCreated\tError\tCategory";
    private static final String VAL_LOG_FMT = "%1$d\t%2$d\t%3$s\t%4$tB %4$te, %4$tY\t%5$s\t%6$s";
    private static final String CATEGORY_LIST_HEADER = "Category List Follows:";
    private static final String CATEGORY_LABEL = "Category: ";
    private static final String ENCOUNTERED_TYPE_PREFIX = "Encountered Type: ";
    private static final String IGNORED_TYPE_PREFIX = "Ignored Type: ";
    private static final String VALIDATED_LIST_HEADER = "Types Encountered and Validated:";
    private static final String INGORED_LIST_HEADER = "Types Encountered but NOT Validated:";
    private Logger internalLogger;
    private Map<Category,List<ReportData>> categoryListMap;
    private Set<String> validatedTypes;
    private Set<String> unvalidatedTypes;
    private Map<String,Long> filePatternToMinSize;
    private String description;
    private int failureCount = 0;
    private Status finalStatus = Status.Unfinished;

    private PrintWriter internalWriter;

    private boolean __reportPositives;

    public ValidationLogger( Logger internalLogger, String description ) {
        this.internalLogger = internalLogger;
        this.description = description;
        this.filePatternToMinSize = new HashMap<>();
        this.categoryListMap = new TreeMap<>();
        this.validatedTypes = new TreeSet<>();
        this.unvalidatedTypes = new TreeSet<>();
        addCategory(GENERAL_CATEGORY_EXCEPTION);
    }

    public void setPrintWriter(PrintWriter writer) {
        this.internalWriter = writer;
    }

    public boolean isToReportPositives() { return __reportPositives; }
    public void setToReportPositives( boolean reportPositives ) { __reportPositives = reportPositives; }
    public void addValidatedType( String type ) { validatedTypes.add( type ); }
    public void addUnvalidatedType( String type ) { unvalidatedTypes.add( type ); }

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
     * @return smallest allowed size.
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
        if ( ! categoryListMap.containsKey( category ) ) {
            categoryListMap.put(category, new ArrayList<ReportData>());
        }
    }

    /**
     * Report error from any validator.
     * A note about testCategory: this is enforced in a Set, because we wish to be able to easily see all things that
     * were checked, rather than assume because no error appeared, some particular condition never occurred.
     * Absence of evidence is not evidence of absence.
     *
     * @param sampleId the sample tree containnig the entity being checked.
     * @param owningEntityType what kind of entity is being checked.
     * @param entity the GUID of the entity with failure.
     * @param testCategory what kind of test.
     * @param message description of failure.
     */
    public void reportError( Long sampleId, Entity entity, String owningEntityType, Category testCategory, String message ) {
        if ( ! categoryListMap.containsKey(testCategory) ) {
            throw new UnknownCategoryException( testCategory, sampleId, entity.getId(), message );
        }
        ReportData reportData = new ReportData();
        reportData.setCreationDate( entity.getCreationDate() );
        reportData.setCategory( testCategory );
        reportData.setEntityId(entity.getId());
        reportData.setSampleId(sampleId);
        reportData.setEntityType(owningEntityType);
        reportData.setMessage( message );
        categoryListMap.get( testCategory ).add( reportData );

        failureCount ++;
    }

    public void reportError( Long sampleId, Entity entity, Category testCategory, String message ) {
        reportError(sampleId, entity, entity.getEntityTypeName(), testCategory, message);
    }

    public void reportSuccess( Long entityId, String owningEntityType ) {
        System.out.println("Found a valid " + owningEntityType + " with ID=" + entityId);
    }

    /**
     * This is the final closure.  Do not try and report any errors or add categories after this.
     */
    @Override
    public void close() {
        if ( internalWriter == null ) {
            writeToLog();
        }
        else {
            outputToWriter();
            internalWriter.close();
        }
        categoryListMap = null;

        finalStatus = ( failureCount == 0 ) ? Status.Success : Status.Failure;
    }

    /** This can be consulted to figure out what to do with the output file. */
    public Status getFinalStatus() {
        if ( finalStatus == Status.Unfinished ) {
            throw new IllegalStateException( "You must first close this class before getting final status." );
        }
        return finalStatus;
    }

    /**
     * File-wise dump for subsequent read by another process.
     */
    private void outputToWriter() {
        internalWriter.println(description);
        internalWriter.println(SECTION_BREAK);

        internalWriter.println(VALIDATED_LIST_HEADER);
        for ( String type: validatedTypes ) {
            internalWriter.println( ENCOUNTERED_TYPE_PREFIX + type );
        }
        internalWriter.println(SECTION_BREAK);

        internalWriter.println(INGORED_LIST_HEADER);
        for ( String type: unvalidatedTypes ) {
            internalWriter.println( IGNORED_TYPE_PREFIX + type + " - ignored" );
        }
        internalWriter.println(SECTION_BREAK);

        internalWriter.println(CATEGORY_LIST_HEADER);
        for ( Category category: categoryListMap.keySet() ) {
            internalWriter.println(CATEGORY_LABEL + category.toString());
        }
        internalWriter.println(SECTION_BREAK);

        for ( Category category: categoryListMap.keySet() ) {
            if ( categoryListMap.get( category ).size() > 0 ) {
                internalWriter.println(VAL_LOG_HEADER);
                for ( ReportData reportData: categoryListMap.get( category ) ) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime( reportData.getCreationDate() );
                    String errorMesage = String.format(
                            VAL_LOG_FMT, reportData.getSampleId(), reportData.getEntityId(), reportData.getEntityType(), calendar, reportData.getMessage(), reportData.getCategory()
                    );
                    internalWriter.println(errorMesage);
                }
                internalWriter.print(SECTION_BREAK);
            }
        }
    }

    /**
     * Log-wise dump for programmer examine.
     */
    private void writeToLog() {
        internalLogger.info(CATEGORY_LIST_HEADER);
        for ( Category category: categoryListMap.keySet() ) {
            internalLogger.info(CATEGORY_LABEL + category);
            internalLogger.info( VAL_LOG_HEADER );
            for ( ReportData reportData: categoryListMap.get( category ) ) {
                String errorMesage = String.format(
                        VAL_LOG_FMT, reportData.getSampleId(), reportData.getEntityId(), reportData.getEntityType(), reportData.getCreationDate(), reportData.getMessage(), reportData.getCategory()
                );
                internalLogger.error( errorMesage );
            }
        }
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
            return o != null   &&   o instanceof ValidationLogger   &&   o.toString().equals( toString() );
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
        private Date creationDate;

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

        public Date getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(Date creationDate) {
            this.creationDate = creationDate;
        }
    }
}
