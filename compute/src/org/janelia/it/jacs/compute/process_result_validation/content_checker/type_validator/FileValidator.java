package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by fosterl on 6/27/14.
 */
public class FileValidator {
    private ValidationLogger validationLogger;
    public FileValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    public void validateFileSet( Entity entity, Long sampleId, String[] requiredChildEntityTypes ) throws Exception {
        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData : entity.getEntityData() ) {
            entityNamesFound.add( entityData.getEntityAttrName() );
        }

        for ( String requiredChild: requiredChildEntityTypes ) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild(requiredChild, sampleId, entity);
            }
            else {
                String value = entity.getValueByAttributeName( requiredChild );
                if ( value == null  ||  value.trim().length() == 0 ) {
                    reportEmptyChild(requiredChild, sampleId, entity);
                }
                else {
                    validateFile( value, requiredChild, 10000L );
                }
            }
        }
    }

    public void validateFile( String filePath, String fileType, Long minLength ) throws Exception {
        String prob = TypeValidationHelper.getFileError( filePath, fileType, minLength );
        if ( prob != null ) {
            validationLogger.reportError( prob );
        }
    }

    private void reportMissingChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError(
                String.format(
                        "Expected child entity for %s, under entity %s/%d, belonging to sample %d not found.",
                        childAttribName, entity.getName(), entity.getId(), sampleId
                )
        );
    }

    private void reportEmptyChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError(
                String.format(
                        "Child Entity %s, under entity %s/%d, belonging to sample %d is null or empty.",
                        childAttribName, entity.getName(), entity.getId(), sampleId
                )
        );
    }

}
