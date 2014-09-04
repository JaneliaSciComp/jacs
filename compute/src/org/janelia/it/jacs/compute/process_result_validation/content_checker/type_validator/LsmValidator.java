package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;

/**
 * Created by fosterl on 7/1/14.
 */
public class LsmValidator implements TypeValidator {

    private ValidationLogger validationLogger;

    public LsmValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {

    }
}
