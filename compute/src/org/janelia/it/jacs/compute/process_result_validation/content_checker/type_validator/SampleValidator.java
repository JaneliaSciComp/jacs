package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.StringUtils;

import java.util.Set;
import java.util.TreeSet;

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
    private static final String UNMATCHED_TILE_FMT = "Under sample %d, the anatomical areas represented by tiles (%s) do not match those for sample processing results (%s).";
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

        boolean reportableSuccess = true;

        // First, how many image tiles.
        Entity refreshedSampleEntity = entityBean.getEntityAndChildren(entity.getId());
        Entity supportingFiles = refreshedSampleEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_SUPPORTING_FILES );
        Set<String> tilesAnatomicalAreas = new TreeSet<>();
        if ( supportingFiles == null ) {
            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SUPPORTING_FILES_CHILD,
                    String.format( NO_SUPPORTING_FILES_FMT, sampleId )
            );
            reportableSuccess = false;
        }
        else {
            Entity refreshedSFEntity = entityBean.getEntityAndChildren( supportingFiles.getId() );
            for ( Entity child: refreshedSFEntity.getChildren() ) {
                if ( child.getEntityTypeName().equals( EntityConstants.TYPE_IMAGE_TILE ) ) {
                    child = entityBean.getEntityAndChildren( child.getId() );
                    String anatomicalArea = child.getValueByAttributeName( EntityConstants.ATTRIBUTE_ANATOMICAL_AREA );
                    if ( anatomicalArea != null ) {
                        tilesAnatomicalAreas.add( anatomicalArea );
                    }
                }
            }

        }

        // Next: are there enough Sample Processing Results?  Use only the latest pipeline run.
        Set<String> sampleProcessingAnatomicalAreas = new TreeSet<>();
        Entity pipelineRun = null;
        for ( Entity child: refreshedSampleEntity.getChildren() ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_PIPELINE_RUN ) ) {
                if ( pipelineRun == null  ||  pipelineRun.getCreationDate().before( child.getCreationDate() ) ) {
                    pipelineRun = child;
                }
            }
        }

        if ( pipelineRun != null ) {
            Entity refreshedPipeline = entityBean.getEntityAndChildren( pipelineRun.getId() );
            for ( Entity grandChild: refreshedPipeline.getChildren() ) {
                if ( grandChild.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT ) ) {
                    grandChild = entityBean.getEntityAndChildren( grandChild.getId() );
                    String anatomicalArea = grandChild.getValueByAttributeName( EntityConstants.ATTRIBUTE_ANATOMICAL_AREA );
                    sampleProcessingAnatomicalAreas.add(anatomicalArea);
                }
            }
        }

        if (! sampleProcessingAnatomicalAreas.equals( tilesAnatomicalAreas ) ) {
            String sampleProcessingAAStr = StringUtils.getCommaDelimited( sampleProcessingAnatomicalAreas );
            String tilesAAStr = StringUtils.getCommaDelimited( tilesAnatomicalAreas );

            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE,
                    String.format(UNMATCHED_TILE_FMT, sampleId, tilesAAStr, sampleProcessingAAStr )
            );
            reportableSuccess = false;
        }

        if ( validationLogger.isToReportPositives()  &&  reportableSuccess ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_SAMPLE + " : has one+ pipeline runs matched by sample processing.");
        }
    }

}
