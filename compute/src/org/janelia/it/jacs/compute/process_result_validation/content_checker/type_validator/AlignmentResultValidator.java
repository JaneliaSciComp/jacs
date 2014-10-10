package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Set;

/**
 * Validate the descendent-hierarchy of an alignnent result.
 *
 * Created by fosterl on 7/7/14.
 */
public class AlignmentResultValidator implements TypeValidator {
    //todo research the name as defined constant.
    public static final String JBA_ALIGNMENT_ENTITY_NAME = "JBA Alignment";
    private static final ValidationLogger.Category MISSING_QI_CATEGORY = new ValidationLogger.Category("Qi Score Missing");
    private static final String MISSING_QI_FMT = MISSING_QI_CATEGORY + " under JBA Alignment %d / %s in sample %d";

    private static final String[] REQUIRED_CHILD_ENTITY_TYPES = new String[] {
            EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT,
            EntityConstants.TYPE_SUPPORTING_DATA,
    };

    private static final String[] REQUIRED_NF_CHILD_FILES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_MASK_IMAGE,
            EntityConstants.ATTRIBUTE_CHAN_IMAGE,
    };

    private final static String[] REQUIRED_CHILD_FILES = new String[] {
            EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE,
            EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE,
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };

    private final static String[] REQUIRED_JPA_CHILD_FILES = new String[] {
            EntityConstants.ATTRIBUTE_REFERENCE_MIP_IMAGE,
            EntityConstants.ATTRIBUTE_SIGNAL_MIP_IMAGE,
    };

    private EntityBeanLocal entityBean;
    private SubEntityValidator subEntityValidator;
    private ValidationLogger validationLogger;

    private FileValidator fileValidator;
    //private SampleProcessingValidator sampleProcessingValidator;

    public AlignmentResultValidator( ValidationLogger validationLogger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.subEntityValidator = subEntityValidator;
        this.entityBean = entityBean;
        this.fileValidator = new FileValidator( validationLogger );
        this.validationLogger = validationLogger;
        //sampleProcessingValidator = new SampleProcessingValidator( validationLogger, subEntityValidator, entityBean );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByEntityType(entity, sampleId, REQUIRED_CHILD_ENTITY_TYPES, entityBean);
        //sampleProcessingValidator.validateSupportingData(entity, sampleId);     // Same checks required here, as for Sample Processing.
        fileValidator.validateFileSet( entity, sampleId, REQUIRED_CHILD_FILES );
        validateDescendants(entity, sampleId);
    }

    private void validateDescendants( Entity entity, Long sampleId ) throws Exception {
        boolean validGenericAlignmentResult = true;
        Set<Entity> children = entityBean.getChildEntities( entity.getId() );
        for ( Entity child: children ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT ) ) {
                Set<Entity> nsprChildren = entityBean.getChildEntities( child.getId() );
                for ( Entity nsprChild: nsprChildren ) {
                    if ( nsprChild.getEntityTypeName().equals( EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION ) ) {
                        // Look down in the fragment collection.
                        Set<EntityData> entityDatas = nsprChild.getEntityData();
                        for ( EntityData entityData: entityDatas ) {
                            if ( entityData.getEntityAttrName().equals( EntityConstants.ATTRIBUTE_ENTITY ) ) {
                                Entity grandChild = entityBean.getEntityById( entityData.getChildEntity().getId() );
                                if ( grandChild.getEntityTypeName().equals( EntityConstants.TYPE_NEURON_FRAGMENT ) ) {
                                    // Need to test the NF assumptions.
                                    validGenericAlignmentResult = fileValidator.validateFileSet( grandChild, sampleId, REQUIRED_NF_CHILD_FILES );
                                }
                            }
                        }

                        break;
                    }
                }
                break;
            }
        }

        if ( entity.getName().equals(JBA_ALIGNMENT_ENTITY_NAME) ) {
            validationLogger.addCategory( MISSING_QI_CATEGORY );
            entityBean.loadLazyEntity(entity, false);

            String qiScore = null;
            boolean validJPA = true;
            Entity d3i = entity.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
            if ( d3i != null ) {
                qiScore = d3i.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_INCONSISTENCY_SCORE);
                validJPA = fileValidator.validateFileSet( d3i, sampleId, REQUIRED_JPA_CHILD_FILES );
            }

            if ( qiScore == null ) {
                Entity reportEntity = d3i;
                if ( reportEntity == null ) {
                    reportEntity = entity;
                }
                validationLogger.reportError(
                        sampleId, reportEntity, entity.getEntityTypeName(), MISSING_QI_CATEGORY,
                        String.format(
                                MISSING_QI_FMT, entity.getId(), entity.getName(), sampleId
                        )
                );
                validJPA = false;
            }

            if ( validJPA  &&  validationLogger.isToReportPositives() ) {
                validationLogger.reportSuccess( entity.getId(), JBA_ALIGNMENT_ENTITY_NAME  + " with valid QI Score and 3d-Image's sub-files." );
            }

        }
        else if ( validGenericAlignmentResult  &&  validationLogger.isToReportPositives() ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_ALIGNMENT_RESULT );
        }
    }

}
