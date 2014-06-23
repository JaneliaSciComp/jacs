package org.janelia.it.jacs.compute.process_result_validation.content_checker.validatable;

import org.janelia.it.jacs.compute.process_result_validation.content_checker.FinderException;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.PrototypeValidatable;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.Validatable;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by fosterl on 6/18/14.
 */
public class FileValidatable implements Validatable {

    public static final int UNLIMITED_FILE_COUNT = Integer.MAX_VALUE;
    public static final String EMPTY_FILE_INVALID = "Empty File";
    public static final String TOO_MANY_FILES = "More than Expected Number of Files with Extension ";
    private PrototypeValidatable prototypeValidatable;
    private File parentPath;
    private String name;
    private boolean valid = false; // Default and overwrite.
    private String validityReason;
    private Long sampleId;
    private int maxFileCount;

    /**
     * Construct with all that is needed to validate immediately.
     *
     * @param sampleId for reporting afterward.
     * @param prototypeValidatable places this in its hierarchy.
     * @param parentPath directory housing a putative file (to test for).
     * @param maxFileCount set to UNLIMITED_FILE_COUNT if no such test is needed.  1 is a typical value.
     */
    public FileValidatable( Long sampleId, PrototypeValidatable prototypeValidatable, FileFinder fileFinder, File parentPath, int maxFileCount ) throws FinderException {
        this.prototypeValidatable = prototypeValidatable;
        this.parentPath = parentPath;
        this.sampleId = sampleId;
        this.maxFileCount = maxFileCount;

        checkValidity( fileFinder, parentPath );
    }

    @Override
    public PrototypeValidatable getProtypeValidatable() {
        return prototypeValidatable;
    }

    @Override
    public long getSampleId() {
        return sampleId;
    }

    @Override
    public String getId() {
        return parentPath.getAbsolutePath() + name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String getValidityReason() {
        return validityReason;
    }

    private void checkValidity( FileFinder fileFinder, File parentPath ) throws FinderException {
        StringBuilder validityReason = new StringBuilder();
        String category = prototypeValidatable.getValidationTypeCategory();

        File[] values = fileFinder.getFiles(parentPath, category);

        ValidatableHelper helper = new ValidatableHelper();
        if ( values == null || values.length == 0  ) {
            helper.addInstance(validityReason, Validatable.VALIDITY_REASON_MISSING);
        }
        else if ( values.length >= 1 ) {
            valid = true;
            StringBuilder allNames = new StringBuilder();
            for ( File value: values ) {
                allNames.append( value ).append(" ");
            }
            name = allNames.toString().trim();

            helper.addInstance(validityReason, Validatable.VALIDITY_REASON_OK);
            for ( File value: values ) {
                if ( value.length() == 0 ) {
                    valid = false;
                    helper.addInstance(validityReason, EMPTY_FILE_INVALID);
                }
            }

            if ( values.length > maxFileCount ) {
                helper.addInstance(validityReason, TOO_MANY_FILES + category);
            }
        }
        this.validityReason = validityReason.toString().trim();

    }
}
