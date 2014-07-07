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

    private EntityBeanLocal entityBean;
    private SubEntityValidator subEntityValidator;

    private FileValidator fileValidator;
    private SampleProcessingValidator sampleProcessingValidator;

    public AlignmentResultValidator( ValidationLogger validationLogger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean ) {
        this.subEntityValidator = subEntityValidator;
        this.entityBean = entityBean;
        this.fileValidator = new FileValidator( validationLogger );
        sampleProcessingValidator = new SampleProcessingValidator( validationLogger, subEntityValidator, entityBean );
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByEntityType(entity, sampleId, REQUIRED_CHILD_ENTITY_TYPES, entityBean);
        sampleProcessingValidator.validateSupportingData(entity, sampleId);     // Same checks required here, as for Sample Processing.
        fileValidator.validateFileSet( entity, sampleId, REQUIRED_CHILD_FILES );
        validateDescendants(entity, sampleId);
    }

    private void validateDescendants( Entity entity, Long sampleId ) throws Exception {
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
                                    fileValidator.validateFileSet( grandChild, sampleId, REQUIRED_NF_CHILD_FILES );
                                }
                            }
                        }

                        break;
                    }
                }
                break;
            }
        }
    }
}
