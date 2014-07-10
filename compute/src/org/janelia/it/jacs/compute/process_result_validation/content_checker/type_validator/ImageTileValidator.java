package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

/**
 * Check the contents of an Image Tile, to ensure it is good.  Report what is missing.  Typical names for
 * such entities are "brain", or "cns"
 *
 * Created by fosterl on 7/1/14.
 */
public class ImageTileValidator implements TypeValidator {
    private static final String STITCH_PREFIX = "stitched-";
    private static final ValidationLogger.Category NO_LSM_STACKS = new ValidationLogger.Category("No LSM stacks");
    private ValidationLogger validationLogger;
    private EntityBeanLocal entitybean;
    private SubEntityValidator subEntityValidator;

    private static final String[] REQUIRED_ATTRIBUTE_NAMES = new String[] {
                EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
                EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
                EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };

    public ImageTileValidator( ValidationLogger validationLogger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.validationLogger = validationLogger;
        this.subEntityValidator = subEntityValidator;
        this.entitybean = entityBean;
        this.validationLogger.addCategory( NO_LSM_STACKS );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        // Check the simple parts: got these files?
        boolean isValid = subEntityValidator.validateSubEntitiesByAttributeName(entity, sampleId, REQUIRED_ATTRIBUTE_NAMES);
        int lsmCount = 0;
        // The more unique parts of the val: look for some nondescriptly-named entities.
        for ( EntityData entityData : entity.getEntityData() ) {
            // Look at all generic Entity children.
            if ( entityData.getEntityAttrName().equals( EntityConstants.ATTRIBUTE_ENTITY ) ) {
                // Must ensure all needed info has been pulled from db.
                Entity newChildEntity = entitybean.getEntityById( entityData.getChildEntity().getId() );
                if ( newChildEntity.getEntityTypeName().equals( EntityConstants.TYPE_LSM_STACK ) ) {
                    lsmCount ++;
                }
            }
        }

        if (lsmCount == 0) {
            validationLogger.reportError( sampleId, entity.getId(), entity.getEntityTypeName(), NO_LSM_STACKS, "LSMs missing from " + entity.getName() );
            isValid = false;
        }

        if ( isValid   &&   validationLogger.isToReportPositives() ) {
            validationLogger.reportSuccess( entity.getId(), entity.getEntityTypeName() );
        }
    }
}
