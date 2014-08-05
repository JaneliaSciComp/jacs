package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.apache.log4j.Logger;
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
    private static final ValidationLogger.Category NO_SUPPORTING_FILES_CHILD = new ValidationLogger.Category("No Supporting Files");
    private static final String UNMATCHED_TILE_FMT = "At least %d Image Tile instances under sample %d have no matching Sample Processing instance";
    private static final String NO_SUPPORTING_FILES_FMT = "Sample %d has no supporting files folder.";
    private static final String[] REQUIRED_ATTRIBUTE_NAMES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };
    private Logger logger = Logger.getLogger( SampleValidator.class );

    public SampleValidator( ValidationLogger logger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.validationLogger = logger;
        this.entityBean = entityBean;
        this.subEntityValidator = subEntityValidator;
        logger.addCategory( NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE );
        logger.addCategory( NO_SUPPORTING_FILES_CHILD );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByAttributeName(entity, sampleId, REQUIRED_ATTRIBUTE_NAMES);

        // First, how many image tiles.
        Entity refreshedSampleEntity = entityBean.getEntityAndChildren( entity.getId() );
        Entity supportingFiles = refreshedSampleEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_SUPPORTING_FILES );
        boolean reportableSuccess = false;
        int unmatchedImageTileCount = 0;
        if ( supportingFiles == null ) {
            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SUPPORTING_FILES_CHILD,
                    String.format( NO_SUPPORTING_FILES_FMT, sampleId )
            );
        }
        else {
            Entity refreshedSFEntity = entityBean.getEntityAndChildren( supportingFiles.getId() );
            for ( Entity child: refreshedSFEntity.getChildren() ) {
                if ( child.getEntityTypeName().equals( EntityConstants.TYPE_IMAGE_TILE ) ) {
                    unmatchedImageTileCount ++;
                }
            }

            if ( unmatchedImageTileCount > 0 ) {
                reportableSuccess = true;
            }

        }

        int numTiles = unmatchedImageTileCount;

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
                    String.format(UNMATCHED_TILE_FMT, unmatchedImageTileCount, sampleId)
            );
        }
        else if ( numTiles > 1 ) {
            logger.info( String.format( "Found a sample %d with %d Image Tiles, all with Sample Processing against them.", sampleId, numTiles) );
        }

        if ( validationLogger.isToReportPositives()  &&  reportableSuccess  &&  unmatchedImageTileCount <= 0 ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_SAMPLE + " : has one+ pipeline runs matched by sample processing.");
        }
    }

}
