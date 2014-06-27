package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Verifies that expected things are found under any Image 3D.
 *
 * Created by fosterl on 6/27/14.
 */
public class Image3DFileValidator implements TypeValidator {

    private FileValidator fileValidator;

    private static String[] REQUIRED_CHILD_FILES;
    static {
        REQUIRED_CHILD_FILES = new String[] {
                EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
                EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
                EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
                EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,

                EntityConstants.ATTRIBUTE_ALIGNMENT_VERIFY_MOVIE,
                EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE,
                EntityConstants.ATTRIBUTE_ALIGNMENT_SPACE,
                EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION,
        };
    }


    public Image3DFileValidator(ValidationLogger validationLogger) {
        fileValidator = new FileValidator(validationLogger);
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        fileValidator.validateFileSet(entity, sampleId, REQUIRED_CHILD_FILES);
    }

}
