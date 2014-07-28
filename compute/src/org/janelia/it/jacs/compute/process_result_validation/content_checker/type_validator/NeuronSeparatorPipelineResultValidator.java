package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
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
    private EntityBeanLocal entityBean;
    private FileValidator fileValidator;

    private static final ValidationLogger.Category NO_FRAGMENTS = new ValidationLogger.Category("Missing Fragments");
    private static final ValidationLogger.Category NO_DEFAULT_3D_IMAGE = new ValidationLogger.Category("Missing Default 3D Image");
    private static final ValidationLogger.Category NO_FAST_LOAD_3D_IMAGE = new ValidationLogger.Category("Missing Fast-Load 3D Image");

    private static final ValidationLogger.Category NO_OBJECTIVE_ATTRIBUTE = new ValidationLogger.Category("Missing Objective Attribute");
    private static final ValidationLogger.Category NO_PIXRES_ATTRIBUTE = new ValidationLogger.Category("Missing Pixel Resolution Attribute");
    private static final ValidationLogger.Category NO_OPTICALRES_ATTRIBUTE = new ValidationLogger.Category("Missing Optical Resolution Attribute");

    @SuppressWarnings("unused")
    private static final ValidationLogger.Category FOOLS_ERRAND = new ValidationLogger.Category("Missing No-Such Attribute");

    private static final String ERROR_RPT_FMT = "No %s found for %s: %d.";

    public NeuronSeparatorPipelineResultValidator( ValidationLogger validationLogger, EntityBeanLocal entityBean ) {
        this.validationLogger = validationLogger;
        this.entityBean = entityBean;
        this.fileValidator = new FileValidator( validationLogger );
        this.validationLogger.addCategory( NO_FRAGMENTS );
        this.validationLogger.addCategory( NO_DEFAULT_3D_IMAGE );
        this.validationLogger.addCategory( NO_FAST_LOAD_3D_IMAGE );

        this.validationLogger.addCategory( NO_OBJECTIVE_ATTRIBUTE );
        this.validationLogger.addCategory( NO_OPTICALRES_ATTRIBUTE );
        this.validationLogger.addCategory( NO_PIXRES_ATTRIBUTE );

        //DEBUG: this.validationLogger.addCategory( FOOLS_ERRAND );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        validateChildren(entity, sampleId);
        validateDefaultAndFastLoad(entity, sampleId);
    }

    private void validateDefaultAndFastLoad(Entity entity, Long sampleId) throws Exception {
        // Checking on the fast-load file.
        Entity default3DImage = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if ( default3DImage == null ) {
            validationLogger.reportError( sampleId, entity, NO_DEFAULT_3D_IMAGE,
                    String.format( ERROR_RPT_FMT, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
        }
        else {
            Entity refreshedEntity = entityBean.getEntityAndChildren( default3DImage.getId() );
            String default3dImageFile = refreshedEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_FILE_PATH );
            Entity fastLoad = refreshedEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE );
            if ( fastLoad == null ) {
                validationLogger.reportError( sampleId, entity, NO_FAST_LOAD_3D_IMAGE,
                        String.format( ERROR_RPT_FMT, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
            }
            else {
                refreshedEntity = entityBean.getEntityAndChildren( fastLoad.getId() );
                String fastLoadImageFile = refreshedEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_FILE_PATH );
                fileValidator.validateFile( fastLoadImageFile, EntityConstants.TYPE_MOVIE, sampleId, entity );
            }
            fileValidator.validateFile( default3dImageFile, EntityConstants.TYPE_IMAGE_3D, sampleId, entity );
        }
    }

    private void validateChildren(Entity entity, Long sampleId) {
        // Look for fragment collection.
        Entity fragments = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        if ( fragments == null ) {
            validationLogger.reportError(
                    sampleId, entity, NO_FRAGMENTS,
                    String.format( ERROR_RPT_FMT, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
        }

        // Look for specific attributes by name.
        validateAttribute(entity, sampleId, EntityConstants.ATTRIBUTE_OBJECTIVE, NO_OBJECTIVE_ATTRIBUTE );
        validateAttribute(entity, sampleId, EntityConstants.ATTRIBUTE_PIXEL_RESOLUTION, NO_PIXRES_ATTRIBUTE );
        validateAttribute(entity, sampleId, EntityConstants.ATTRIBUTE_OPTICAL_RESOLUTION, NO_OPTICALRES_ATTRIBUTE );

        // DEBUG: Check that something which should not be there, truly is not found, and hence validate validation.
        // DEBUG:  validateAttribute(entity, sampleId, "Red Herring", FOOLS_ERRAND );

    }

    private void validateAttribute(Entity entity, Long sampleId, String attributeName, ValidationLogger.Category category) {
        String value = entity.getValueByAttributeName( attributeName );
        if ( value == null   ||   value.trim().length() == 0 ) {
            validationLogger.reportError(
                    sampleId, entity, category,
                    String.format( ERROR_RPT_FMT, attributeName, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
        }
    }
}
