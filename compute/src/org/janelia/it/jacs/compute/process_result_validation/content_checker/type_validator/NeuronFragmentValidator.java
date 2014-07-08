package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Validates contents of a neuron fragment.
 *
 * Created by fosterl on 6/27/14.
 */
public class NeuronFragmentValidator implements TypeValidator {

    private FileValidator fileValidator;
    private static final String[] REQUIRED_CHILD_FILES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_MASK_IMAGE,
            EntityConstants.ATTRIBUTE_CHAN_IMAGE,
    };

    public NeuronFragmentValidator( ValidationLogger validationLogger ) {
        fileValidator = new FileValidator(validationLogger);
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        if ( fileValidator.validateFileSet(entity, sampleId, REQUIRED_CHILD_FILES)  &&  REPORT_POSITIVES ) {
            System.out.println( "Found a valid Neuron Fragment " + entity.getId() );
        }
    }
}
