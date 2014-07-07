package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.HashSet;
import java.util.Set;

/**
 * Generic code to look at sub entities by type, to assure all listed types are present.
 *
 * Created by fosterl on 7/1/14.
 */
public class SubEntityValidator {

    private ValidationLogger validationLogger;
    private static final String MISSING_SUBENTITY = "Missing subentity: ";
    private static final String MISSING_CHILD_OF_TYPE = "Missing child of type: ";

    public SubEntityValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    public boolean validateSubEntitiesByAttributeName(Entity entity, Long sampleId, String[] requiredAttributeNames) throws Exception {
        boolean rtnVal = true;
        for ( String name: requiredAttributeNames) {
            validationLogger.addCategory( MISSING_SUBENTITY + name );
        }

        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData: entity.getEntityData() ) {
            entityNamesFound.add( entityData.getEntityAttrName() );
        }

        for ( String requiredChild: requiredAttributeNames) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild( MISSING_SUBENTITY, requiredChild, sampleId, entity );
                rtnVal = false;
            }
        }
        return rtnVal;
    }

    public boolean validateSubEntitiesByEntityType(Entity entity, Long sampleId, String[] requiredEntityType, EntityBeanLocal entityBean) throws Exception {
        boolean rtnVal = true;
        for ( String name: requiredEntityType) {
            validationLogger.addCategory( MISSING_CHILD_OF_TYPE + name );
        }

        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData: entity.getEntityData() ) {
            if ( entityData.getChildEntity() != null ) {
                Entity attachedEntity = entityBean.getEntityById( entityData.getChildEntity().getId() );
                entityNamesFound.add( attachedEntity.getEntityTypeName() );
            }
        }

        for ( String requiredChild: requiredEntityType) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild( MISSING_CHILD_OF_TYPE, requiredChild, sampleId, entity );
                rtnVal = false;
            }
        }
        return rtnVal;
    }

    private void reportMissingChild( String prefix, String childAttribName, Long sampleId, Entity entity ) throws Exception {
        String message = String.format(
                "Expected child entity for %s, under entity %s/%d, belonging to sample %d not found.",
                childAttribName, entity.getName(), entity.getId(), sampleId
        );
        validationLogger.reportError(
                sampleId, entity.getId(), entity.getEntityTypeName(), new ValidationLogger.Category(prefix + childAttribName), message
        );
    }

}
