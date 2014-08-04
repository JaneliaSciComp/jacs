package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import java.io.File;

/**
 * This does generic tests against "stuff" found during validation.
 *
 * Created by fosterl on 6/27/14.
 */
public class TypeValidationHelper {
    private static final String MISSING_FILE_PATH = "File path missing for type %s";
    private static final String MISSING_FILE = "File %s, of type %s, does not exist";
    private static final String FILE_TOO_SHORT = "File %s, at %d, is less than minimum of %d";
    public static String getFileError( String fullFilePath, String fileType, long minFileLength ) {
        String rtnVal = null;
        if ( fullFilePath == null ) {
            rtnVal = String.format( MISSING_FILE_PATH, fileType );
        }
        else {
            File checkFile = new File( fullFilePath );
            if ( ! checkFile.exists() ) {
                rtnVal = String.format( MISSING_FILE, fullFilePath, fileType );
            }
            else if ( minFileLength > 0    &&   checkFile.length() < minFileLength ) {
                rtnVal = String.format( FILE_TOO_SHORT, fullFilePath, checkFile.length(), minFileLength );
            }
        }

        return rtnVal;
    }
}
