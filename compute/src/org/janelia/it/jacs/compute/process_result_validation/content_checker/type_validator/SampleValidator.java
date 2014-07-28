package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Check that all stuff that must reside in a sample actually IS in it.
 * Created by fosterl on 6/27/14.
 */
public class SampleValidator implements TypeValidator {
    private ValidationLogger validationLogger;
    private EntityBeanLocal entityBean;
    private SubEntityValidator subEntityValidator;
    private static final ValidationLogger.Category NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE = new ValidationLogger.Category("Image Tiles with Unmatched Sample Processing");
    private static final String UNMATCHED_TILE_FMT = "At least %d Image Tile instances under sample %d have no matching Sample Processing instance";
    private static final String[] REQUIRED_ATTRIBUTE_NAMES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };

    public SampleValidator( ValidationLogger logger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.validationLogger = logger;
        this.entityBean = entityBean;
        this.subEntityValidator = subEntityValidator;
        logger.addCategory( NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByAttributeName(entity, sampleId, REQUIRED_ATTRIBUTE_NAMES);

        // First, how many image tiles.
        Entity refreshedSampleEntity = entityBean.getEntityAndChildren( entity.getId() );
        Entity supportingFiles = refreshedSampleEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_SUPPORTING_FILES );
        Entity refreshedSFEntity = entityBean.getEntityAndChildren( supportingFiles.getId() );
        int unmatchedImageTileCount = 0;
        for ( Entity child: refreshedSFEntity.getChildren() ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_IMAGE_TILE ) ) {
                unmatchedImageTileCount ++;
            }
        }

        boolean reportableSuccess = false;
        if ( unmatchedImageTileCount > 0 ) {
            reportableSuccess = true;
        }

        // Next: are there enough Sample Processing Results?
        for ( Entity child: refreshedSampleEntity.getChildren() ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_PIPELINE_RUN ) ) {
                Entity refreshedPipeline = entityBean.getEntityAndChildren( child.getId() );
                for ( Entity grandChild: refreshedPipeline.getChildren() ) {
                    if ( grandChild.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT ) ) {
                        unmatchedImageTileCount --;
                    }
                }
                break;
            }
        }

        if ( unmatchedImageTileCount > 0 ) {
            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE,
                    String.format( UNMATCHED_TILE_FMT, unmatchedImageTileCount, sampleId )
            );
        }

        if ( validationLogger.isToReportPositives()  &&  reportableSuccess  &&  unmatchedImageTileCount <= 0 ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_SAMPLE + " : has one+ pipeline runs matched by sample processing.");
        }
    }

}
