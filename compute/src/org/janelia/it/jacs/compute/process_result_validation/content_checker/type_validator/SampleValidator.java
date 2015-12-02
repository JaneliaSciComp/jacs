package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
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
    public static final String NULL_VALUE = "[Null Value]";
    private ValidationLogger validationLogger;
    private EntityBeanLocal entityBean;
    private SubEntityValidator subEntityValidator;
    private static final ValidationLogger.Category NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE = new ValidationLogger.Category("Image Tiles with Unmatched Sample Processing");
    private static final ValidationLogger.Category NO_SUPPORTING_FILES_CHILD = new ValidationLogger.Category("No Supporting Files");
    private static final ValidationLogger.Category NULL_TILES_ANATOMICAL_AREA = new ValidationLogger.Category("Null Tile Anatomical Area");
    private static final ValidationLogger.Category NULL_PL_ANATOMICAL_AREA = new ValidationLogger.Category("Null Pipeline Anatomical Area");
    private static final String UNMATCHED_TILE_FMT = "Under sample %d, the anatomical areas represented by tiles (%s) do not match those for sample processing results (%s).";
    private static final String NO_SUPPORTING_FILES_FMT = "Sample %d has no supporting files folder.";
    private static final String NULL_ANATAREA_MSG = "Null, missing, or empty value in anatomical area.";
    private static final String[] REQUIRED_ATTRIBUTE_NAMES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };
    private Logger logger = Logger.getLogger( SampleValidator.class );

    private boolean reportableSuccess;

    // Interesting test case 2002560712521547874.  It has errored pipeline runs, and it has unmatched anatomical areas.

    public SampleValidator( ValidationLogger logger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.validationLogger = logger;
        this.entityBean = entityBean;
        this.subEntityValidator = subEntityValidator;
        logger.addCategory( NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE );
        logger.addCategory( NO_SUPPORTING_FILES_CHILD );
        logger.addCategory( NULL_TILES_ANATOMICAL_AREA );
        logger.addCategory( NULL_PL_ANATOMICAL_AREA );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByAttributeName(entity, sampleId, REQUIRED_ATTRIBUTE_NAMES);

        reportableSuccess = true;

        // First, how many image tiles.
        Set<String> tilesAnatomicalAreas = null;
        Entity refreshedSampleEntity = entityBean.getEntityAndChildren(entity.getId());
        Entity supportingFiles = refreshedSampleEntity.getChildByAttributeName( EntityConstants.ATTRIBUTE_SUPPORTING_FILES );
        if ( supportingFiles == null ) {
            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SUPPORTING_FILES_CHILD,
                    String.format( NO_SUPPORTING_FILES_FMT, sampleId )
            );
            reportableSuccess = false;
        }

        checkAnatomicalAreas(entity, sampleId, refreshedSampleEntity, supportingFiles);

        if ( validationLogger.isToReportPositives()  &&  reportableSuccess ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_SAMPLE + " : has one+ pipeline runs matched by sample processing.");
        }
    }

    private void checkAnatomicalAreas(Entity entity, Long sampleId, Entity refreshedSampleEntity, Entity supportingFiles) throws ComputeException {
        Set<String> tilesAnatomicalAreas;
        tilesAnatomicalAreas = getTilesAnatomicalAreas( supportingFiles, sampleId );
        Set<String> sampleProcessingAnatomicalAreas = getSampleProcessingAnatomicalAreas( refreshedSampleEntity, sampleId );

        if (! sampleProcessingAnatomicalAreas.equals( tilesAnatomicalAreas ) ) {
            String sampleProcessingAAStr = StringUtils.getCommaDelimited(sampleProcessingAnatomicalAreas);
            String tilesAAStr = StringUtils.getCommaDelimited( tilesAnatomicalAreas );

            validationLogger.reportError(
                    sampleId,
                    entity,
                    NO_SAMPLE_PROCESSING_FOR_IMAGE_TILE,
                    String.format(UNMATCHED_TILE_FMT, sampleId, tilesAAStr, sampleProcessingAAStr )
            );
            reportableSuccess = false;
        }
    }

    private Set<String> getTilesAnatomicalAreas( Entity supportingFiles, Long sampleId ) throws ComputeException {
        Set<String> tilesAnatomicalAreas = new TreeSet<>();
        if ( supportingFiles != null ) {
            Entity refreshedSFEntity = entityBean.getEntityAndChildren( supportingFiles.getId() );
            for ( Entity child: refreshedSFEntity.getChildren() ) {
                if ( child.getEntityTypeName().equals( EntityConstants.TYPE_IMAGE_TILE ) ) {
                    child = entityBean.getEntityAndChildren( child.getId() );
                    String anatomicalArea = child.getValueByAttributeName( EntityConstants.ATTRIBUTE_ANATOMICAL_AREA );
                    // NOTE: turning on this check, via the nodebug=false, pushes the majority of all samples into the "failed" category.
                    if ( StringUtils.isEmpty( anatomicalArea ) ) {
                        anatomicalArea = NULL_VALUE;
                        if ( validationLogger.isToReportPositives() ) {
                            validationLogger.reportError(
                                    sampleId,
                                    supportingFiles,
                                    NULL_TILES_ANATOMICAL_AREA,
                                    NULL_ANATAREA_MSG
                            );
                            reportableSuccess = false;
                        }
                    }
                    tilesAnatomicalAreas.add( anatomicalArea );
                }
            }
        }
        return tilesAnatomicalAreas;
    }

    private Set<String> getSampleProcessingAnatomicalAreas( Entity sampleEntity, Long sampleId ) throws ComputeException {
        // Use only the latest pipeline run.
        Set<String> sampleProcessingAnatomicalAreas = new TreeSet<>();
        Entity pipelineRun = getPipelineRunEntity( sampleEntity );

        if ( pipelineRun != null ) {
            for ( Entity grandChild: pipelineRun.getChildren() ) {
                if ( grandChild.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT ) ) {
                    grandChild = entityBean.getEntityAndChildren( grandChild.getId() );
                    String anatomicalArea = grandChild.getValueByAttributeName( EntityConstants.ATTRIBUTE_ANATOMICAL_AREA );
                    // NOTE: turning on this check, via the nodebug=false, pushes the majority of all samples into the "failed" category.
                    if (  StringUtils.isEmpty( anatomicalArea ) ) {
                        anatomicalArea = NULL_VALUE;
                        if ( validationLogger.isToReportPositives() ) {
                            validationLogger.reportError(
                                    sampleId,
                                    grandChild,
                                    NULL_PL_ANATOMICAL_AREA,
                                    NULL_ANATAREA_MSG
                            );
                            reportableSuccess = false;
                        }
                    }
                    sampleProcessingAnatomicalAreas.add(anatomicalArea);
                }
            }
        }
        return sampleProcessingAnatomicalAreas;
    }

    private Entity getPipelineRunEntity( Entity sampleEntity ) throws ComputeException {
        Entity pipelineRun = null;
        for ( Entity child: sampleEntity.getChildren() ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_PIPELINE_RUN ) ) {
                if ( pipelineRun == null  ||  pipelineRun.getCreationDate().before( child.getCreationDate() ) ) {
                    pipelineRun = child;
                }
            }
        }
        if ( pipelineRun != null ) {
            pipelineRun = entityBean.getEntityAndChildren( pipelineRun.getId() );
        }

        return pipelineRun;
    }

}
