package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.model.entity.Entity;

/**
 * Validate contents of a pipeline run.
 *
 * Created by fosterl on 6/27/14.
 */
public class PipelineRunValidator implements TypeValidator {
    /**
     * Check: all expected children on hand?
     *
     * @param entity check this pipeline run.
     * @param sampleId ...under this sample--for identification purposes.
     * @throws Exception for called methods.
     */
    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {

    }
}
