package org.janelia.it.jacs.compute.process_result_validation.content_checker;

/**
 * Implement this with something that can be validated by the checker.
 *
 * Created by fosterl on 6/18/14.
 */
public interface Validatable {
    // These are expected, frequently-used values for
    //@See validityReason()
    public static String VALIDITY_REASON_OK = "OK";
    public static String VALIDITY_REASON_MISSING = "Missing";

    PrototypeValidatable getProtypeValidatable();

    long getSampleId();

    /**
     * Unique ID.  May / may not be same as
     * @See getName()
     * above. For files, could be full path, to make it unique.
     * May be null if whole validatable is missing.
     *
     * @return unique identifier.
     */
    String getId();

    /** May be null, if whole validatable is missing. */
    String getName();

    /**
     * Final verdict: was it there, or otherwise damaged?
     * @return whether it could be used correctly, if attempted.
     */
    boolean isValid();

    /**
     * "OK", "Missing" or any invalidity support string.
     * @See VALIDITY_REASON_OK
     * @See VALIDITY_REASON_MISSING
     * @return unconstrained reason--possibly to look good on a report.
     */
    String getValidityReason();
}
