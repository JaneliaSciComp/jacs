package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker;

/**
 * Thrown if a finder for anything of relevance throws an exception.
 *
 * Created by fosterl on 6/19/14.
 */
public class FinderException extends Exception {
    /**
     * Throw this to wrap another exc.
     *
     * @param message should reflect what was being attempted.
     * @param ex the encapsulated exception - what went wrong.
     */
    public FinderException( String message, Exception ex ) {
        super( message, ex );
    }
}
