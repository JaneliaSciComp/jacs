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

    private static final ValidationLogger.Category NO_FRAGMENTS = new ValidationLogger.Category("No Fragments");
    private static final ValidationLogger.Category NO_DEFAULT_3D_IMAGE = new ValidationLogger.Category("No Default 3D Image");
    private static final ValidationLogger.Category NO_FAST_LOAD_3D_IMAGE = new ValidationLogger.Category("No Fast-Load 3D Image");

    private static final String ERROR_RPT_FMT = "No %s found for %s: %d.";

    public NeuronSeparatorPipelineResultValidator( ValidationLogger validationLogger, EntityBeanLocal entityBean ) {
        this.validationLogger = validationLogger;
        this.entityBean = entityBean;
        this.fileValidator = new FileValidator( validationLogger );
        this.validationLogger.addCategory( NO_FRAGMENTS );
        this.validationLogger.addCategory( NO_DEFAULT_3D_IMAGE );
        this.validationLogger.addCategory( NO_FAST_LOAD_3D_IMAGE );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        Entity fragments = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION);
        if ( fragments == null ) {
            validationLogger.reportError(
                    sampleId, entity.getId(), entity.getEntityTypeName(), NO_FRAGMENTS,
                    String.format( ERROR_RPT_FMT, EntityConstants.ATTRIBUTE_MASK_ENTITY_COLLECTION, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
        }

        // Checking on the fast-load file.
        Entity default3DImage = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
        if ( default3DImage == null ) {
            validationLogger.reportError( sampleId, entity.getId(), entity.getEntityTypeName(), NO_DEFAULT_3D_IMAGE,
                    String.format( ERROR_RPT_FMT, EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, entity.getId() ) );
        }
        else {
            Entity refreshedEntity = entityBean.getEntityAndChildren( default3DImage.getId() );
            String default3dImageFile = refreshedEntity.getValueByAttributeName( EntityConstants.ATTRIBUTE_FILE_PATH );
            Entity fastLoad = refreshedEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE );
            if ( fastLoad == null ) {
                validationLogger.reportError( sampleId, entity.getId(), entity.getEntityTypeName(), NO_FAST_LOAD_3D_IMAGE,
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
}
