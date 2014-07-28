package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityData;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates file nodes.
 * Created by fosterl on 6/27/14.
 */
public class FileValidator {
    private final String VAL_MISSING_CHILD = "Expected child entity missing: ";
    private final String VAL_EMPTY_CHILD = "Child entity contents empty: ";   // "Are you my mommy?"

    private ValidationLogger validationLogger;

    public FileValidator( ValidationLogger validationLogger ) {
        this.validationLogger = validationLogger;
    }

    public boolean validateFileSet( Entity entity, Long sampleId, String[] requiredChildEntityTypes ) throws Exception {
        boolean rtnVal = true;
        for ( String type: requiredChildEntityTypes ) {
            validationLogger.addCategory( ValidationLogger.MISSING + type );
            validationLogger.addCategory( ValidationLogger.EMPTY + type );
            validationLogger.addCategory( ValidationLogger.FILE_ERROR + type );
        }
        Set<String> entityNamesFound = new HashSet<>();
        for ( EntityData entityData : entity.getEntityData() ) {
            entityNamesFound.add( entityData.getEntityAttrName() );
        }

        for ( String requiredChild: requiredChildEntityTypes ) {
            if ( ! entityNamesFound.contains( requiredChild ) ) {
                reportMissingChild(requiredChild, sampleId, entity);
                rtnVal = false;
            }
            else {
                String value = safeString(entity, requiredChild);
                if ( value.length() == 0 ) {
                    reportEmptyChild(requiredChild, sampleId, entity);
                    rtnVal = false;
                }
                else {
                    Long minSize = validationLogger.getMinSize( requiredChild );
                    if ( minSize > 0 ) {
                        validationLogger.addCategory( ValidationLogger.MIN_SIZE + requiredChild + " " + minSize );
                    }
                    if ( ! validateFile( value, requiredChild, sampleId, entity ) )
                        rtnVal = false;
                }
            }
        }

        return rtnVal;
    }

    public boolean validateFile( String filePath, String fileType, Long sampleId, Entity entity ) throws Exception {
        boolean rtnVal = true;
        validationLogger.addCategory( ValidationLogger.FILE_ERROR + fileType );
        Long minLength = validationLogger.getMinSize( fileType );
        if ( minLength > 0 ) {
            validationLogger.addCategory( ValidationLogger.MIN_SIZE + fileType + " " + minLength );
        }
        String prob = TypeValidationHelper.getFileError( filePath, fileType, minLength );
        if ( prob != null ) {
            validationLogger.reportError( sampleId, entity, entity.getEntityTypeName(), new ValidationLogger.Category(ValidationLogger.FILE_ERROR + fileType), prob);
            rtnVal = false;
        }
        return rtnVal;
    }

    private String safeString(Entity entity, String requiredChild) {
        String value = entity.getValueByAttributeName(requiredChild);
        if ( value == null ) {
            value = "";
        }
        else {
            value = value.trim();
        }
        return value;
    }

    private void reportMissingChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError( sampleId, entity, new ValidationLogger.Category(ValidationLogger.MISSING + childAttribName), VAL_MISSING_CHILD + childAttribName );
    }

    private void reportEmptyChild( String childAttribName, Long sampleId, Entity entity ) throws Exception {
        validationLogger.reportError( sampleId, entity, new ValidationLogger.Category(ValidationLogger.EMPTY + childAttribName), VAL_EMPTY_CHILD + childAttribName );
    }

}
