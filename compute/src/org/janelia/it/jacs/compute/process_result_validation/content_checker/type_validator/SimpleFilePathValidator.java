package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;


import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Validates a file type for anything that bears a file path, and needs no other validation.  Applies to Image2d.
 * Created by fosterl on 6/27/14.
 */
public class SimpleFilePathValidator implements TypeValidator {
    private String attributeType;
    private Long minLength;
    private FileValidator fileValidator;

    public SimpleFilePathValidator(ValidationLogger validationLogger, String attributeType, Long minLength) {
        this.attributeType = attributeType;
        this.minLength = minLength;
        fileValidator = new FileValidator(validationLogger);
    }

    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        String filePath = entity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        fileValidator.validateFile(filePath, attributeType, sampleId, entity, minLength);

    }

}
