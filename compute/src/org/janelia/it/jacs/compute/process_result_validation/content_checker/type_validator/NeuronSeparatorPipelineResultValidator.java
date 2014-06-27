package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Collection;

/**
 * Verify what's in the separator result.
 *
 * Created by fosterl on 6/27/14.
 */
public class NeuronSeparatorPipelineResultValidator implements TypeValidator {

    private ValidationLogger validationLogger;

    public NeuronSeparatorPipelineResultValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        Entity fragments = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        if ( fragments == null ) {
            validationLogger.reportError(
                    "No " + EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION + " found for " + entity.getEntityTypeName() + ": " + entity.getName() + "/" + entity.getId() + "."
            );
        }
    }
}
