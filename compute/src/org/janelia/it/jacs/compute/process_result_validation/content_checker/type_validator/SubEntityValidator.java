package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

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

    public SubEntityValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    public void validateSubEntities(Entity entity, Long sampleId, String[] requiredChildEntityTypes) throws Exception {
        for ( String type: requiredChildEntityTypes ) {
            validationLogger.addCategory( MISSING_SUBENTITY + type );
        }

        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData: entity.getEntityData() ) {
            entityNamesFound.add( entityData.getEntityAttrName() );
        }

        for ( String requiredChild: requiredChildEntityTypes ) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild( requiredChild, sampleId, entity );
            }
        }
    }

    private void reportMissingChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        String message = String.format(
                "Expected child entity for %s, under entity %s/%d, belonging to sample %d not found.",
                childAttribName, entity.getName(), entity.getId(), sampleId
        );
        validationLogger.reportError( sampleId, entity.getId(), MISSING_SUBENTITY + childAttribName, message );
    }

}
