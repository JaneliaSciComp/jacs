package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Set;

/**
 * Check that all stuff that must reside in a sample actually IS in it.
 * Created by fosterl on 6/27/14.
 */
public class SampleProcessingValidator implements TypeValidator {
    private static final String[] REQUIRED_ATTRIBUTE_NAMES = new String[] { EntityConstants.ATTRIBUTE_SUPPORTING_FILES, };
    private static final String[] REQUIRED_FILE_PATTERNS = new String[] { ".lsm.metadata", "stitched-" };

    private ValidationLogger validationLogger;
    private SubEntityValidator subEntityValidator;
    private EntityBeanLocal entityBean;
    private FileValidator fileValidator;

    public SampleProcessingValidator(ValidationLogger logger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean) {
        this.validationLogger = logger;
        this.subEntityValidator = subEntityValidator;
        this.entityBean = entityBean;
        this.fileValidator = new FileValidator(logger);

        for ( String pattern: REQUIRED_FILE_PATTERNS ) {
            validationLogger.addCategory( ValidationLogger.MISSING + pattern );
        }
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        subEntityValidator.validateSubEntitiesByAttributeName(entity, sampleId, REQUIRED_ATTRIBUTE_NAMES);
        validateSupportingData(entity, sampleId);
    }

    /**
     * This does some more complex delving to find what's needed.
     */
    public void validateSupportingData(Entity entity, Long sampleId) throws Exception {
        Set<Entity> children = entityBean.getChildEntities( entity.getId() );
        boolean[] subEntityFound = new boolean[ REQUIRED_FILE_PATTERNS.length ];
        for ( Entity child: children ) {
            if ( child.getEntityTypeName().equals( EntityConstants.TYPE_SUPPORTING_DATA ) ) {
                // Look down in the supporting data.
                Set<EntityData> entityDatas = child.getEntityData();
                for ( EntityData entityData: entityDatas ) {
                    if ( entityData.getEntityAttrName().equals( EntityConstants.ATTRIBUTE_ENTITY ) ) {
                        Entity grandChild = entityBean.getEntityById( entityData.getChildEntity().getId() );
                        for ( int i = 0; i < REQUIRED_FILE_PATTERNS.length; i++ ) {
                            String pattern = REQUIRED_FILE_PATTERNS[ i ];
                            if ( grandChild.getName().contains( pattern ) ) {
                                subEntityFound[ i ] = true;
                                // Now check the file
                                String filePath = grandChild.getValueByAttributeName( EntityConstants.ATTRIBUTE_FILE_PATH );
                                String fileType = EntityConstants.TYPE_TEXT_FILE;
                                if ( filePath.endsWith( "v3dpbd" ) ) {
                                    fileType = EntityConstants.TYPE_V3D_ANO_FILE;
                                }
                                fileValidator.validateFile( filePath, fileType, sampleId, entity );
                            }
                        }
                    }
                }
                for ( int i = 0; i < subEntityFound.length; i++ ) {
                    if ( ! subEntityFound[ i ] ) {
                        validationLogger.reportError(
                                sampleId, entity.getId(),
                                entity.getEntityTypeName(),
                                new ValidationLogger.Category(ValidationLogger.MISSING + REQUIRED_FILE_PATTERNS[ i ]),
                                String.format(
                                        "Failed to find a %s under Sample Processing entity %d.",
                                        REQUIRED_FILE_PATTERNS[ i ],
                                        entity.getId()
                                ));
                    }
                }

                break;
            }
        }
    }

}
