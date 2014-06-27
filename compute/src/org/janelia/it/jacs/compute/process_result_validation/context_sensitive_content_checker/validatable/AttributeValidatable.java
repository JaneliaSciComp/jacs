package org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.PrototypeValidatable;
import org.janelia.it.jacs.compute.process_result_validation.context_sensitive_content_checker.Validatable;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created by fosterl on 6/19/14.
 */
public class AttributeValidatable implements Validatable {

    private static final String VALIDATION_REASON_EXCEPTION = "Exception on Validation Attempt";

    private PrototypeValidatable prototypeValidatable;
    private Long sampleId;
    private StringBuilder validationReason;
    private boolean valid;  // False by default.

    public AttributeValidatable( PrototypeValidatable prototypeValidatable, Entity parentEntity, AttributeFinder attributeFinder, long sampleId ) {
        this.prototypeValidatable = prototypeValidatable;
        this.sampleId = sampleId;

        validate( parentEntity, attributeFinder );
    }

    @Override
    public PrototypeValidatable getProtypeValidatable() {
        return prototypeValidatable;
    }

    @Override
    public long getSampleId() {
        return sampleId;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String getValidityReason() {
        return validationReason.toString();
    }

    private void validate( Entity parentEntity, AttributeFinder attributeFinder ) {

        validationReason = new StringBuilder();
        ValidatableHelper validatableHelper = new ValidatableHelper();
        // Need to find out if the parent entity has this attribute, and it is not empty.
        try {
            String attributeName = prototypeValidatable.getValidationTypeCategory();
            String attributeValue = attributeFinder.getAttribute( parentEntity, attributeName );
            if ( attributeValue == null  ||  attributeValue.trim().length() == 0 ) {
                validatableHelper.addInstance( validationReason, VALIDITY_REASON_MISSING );
            }
            else {
                validatableHelper.addInstance( validationReason, VALIDITY_REASON_OK );
                valid = true;
            }
        } catch ( Exception ex ) {
            validatableHelper.addInstance( validationReason, VALIDATION_REASON_EXCEPTION );
        }
    }
}
