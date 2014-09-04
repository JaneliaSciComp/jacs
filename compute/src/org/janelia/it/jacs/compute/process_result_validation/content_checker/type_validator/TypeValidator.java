package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Implement this to make something that can validate the entity provided, given what should be directly
 * below that entity.
 *
 * Created by fosterl on 6/27/14.
 */
public interface TypeValidator {
    void validate(Entity entity, Long sampleId ) throws Exception;
}
