package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Verify what's in the separator result.
 *
 * Created by fosterl on 6/27/14.
 */
public class NeuronSeparatorPipelineResultValidator implements TypeValidator {

    private ValidationLogger validationLogger;
    private static final String NO_FRAGMENTS = "No Fragments";

    public NeuronSeparatorPipelineResultValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
        this.validationLogger.addCategory( NO_FRAGMENTS );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        Entity fragments = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        if ( fragments == null ) {
            validationLogger.reportError( sampleId, entity.getId(), entity.getEntityTypeName(), NO_FRAGMENTS,
                    "No fragments found for " + EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION + ": " + entity.getName() + "/" + entity.getId() + ".");
        }
    }
}
