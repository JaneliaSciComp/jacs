package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Check that all stuff that must reside in a sample actually IS in it.
 * Created by fosterl on 6/27/14.
 */
public class SampleValidator implements TypeValidator {
    private ValidationLogger validationLogger;
    private SubEntityValidator subEntityValidator;
    private static final String[] REQUIRED_CHILD_ENTITY_TYPES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };

    public SampleValidator( ValidationLogger logger, SubEntityValidator subEntityValidator ) {
        this.validationLogger = logger;
        this.subEntityValidator = subEntityValidator;
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntities( entity, sampleId, REQUIRED_CHILD_ENTITY_TYPES);
    }

}
