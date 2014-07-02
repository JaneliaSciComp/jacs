package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates file nodes.
 * Created by fosterl on 6/27/14.
 */
public class FileValidator {
    public static final String MISSING = "Missing ";
    public static final String EMPTY = "Empty ";
    public static final String FILE_ERROR = "File Error ";
    private final String VAL_MISSING_CHILD = "Expected child entity missing: ";
    private final String VAL_EMPTY_CHILD = "Child entity contents empty: ";   // "Are you my mommy?"

    private ValidationLogger validationLogger;

    public FileValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    public void validateFileSet( Entity entity, Long sampleId, String[] requiredChildEntityTypes ) throws Exception {
        for ( String type: requiredChildEntityTypes ) {
            validationLogger.addCategory( MISSING + type );
            validationLogger.addCategory( EMPTY + type );
        }
        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData : entity.getEntityData() ) {
            entityNamesFound.add( entityData.getEntityAttrName() );
        }

        for ( String requiredChild: requiredChildEntityTypes ) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild(requiredChild, sampleId, entity);
            }
            else {
                String value = safeString(entity, requiredChild);
                validationLogger.addCategory( FILE_ERROR + value );
                if ( value.length() == 0 ) {
                    reportEmptyChild(requiredChild, sampleId, entity);
                }
                else {
                    validateFile( value, requiredChild, sampleId, entity, 10000L );
                }
            }
        }
    }

    public void validateFile( String filePath, String fileType, Long sampleId, Entity entity, Long minLength ) throws Exception {
        String prob = TypeValidationHelper.getFileError( filePath, fileType, minLength );
        if ( prob != null ) {
            validationLogger.reportError( entity.getId(), sampleId, FILE_ERROR + fileType, prob );
        }
    }

    private String safeString(Entity entity, String requiredChild) {
        String value = entity.getValueByAttributeName( requiredChild );
        if ( value == null ) {
            value = "";
        }
        else {
            value = value.trim();
        }
        return value;
    }

    private void reportMissingChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError( entity.getId(), sampleId, VAL_MISSING_CHILD + childAttribName, MISSING + childAttribName );
    }

    private void reportEmptyChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError( entity.getId(), sampleId, VAL_EMPTY_CHILD + childAttribName, EMPTY + childAttribName );
    }

}
