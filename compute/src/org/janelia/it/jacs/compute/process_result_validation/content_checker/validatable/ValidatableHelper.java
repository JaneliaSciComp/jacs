package org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable;

/**
 * Created by fosterl on 6/18/14.
 */
public class ValidatableHelper {
    public StringBuilder addInstance(StringBuilder combinedValue, String reason) {
        if ( combinedValue.length() != 0 ) {
            combinedValue.append( "; " );
        }
        combinedValue.append( reason );
        return combinedValue;
    }

}
