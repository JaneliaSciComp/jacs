package org.janelia.it.jacs.compute.process_result_validation.content_checker.engine;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.omg.CORBA._ValueDefStub;

import java.util.HashMap;
import java.util.Map;

/**
 * This can validate things by type.
 * Created by fosterl on 6/27/14.
 */
public class ValidationEngine {
    private static Logger logger = Logger.getLogger(ValidationEngine.class);
    private ValidationLogger validationLogger;

    private Map<String,TypeValidator> validatorMap;

    public ValidationEngine() {
        createValidatorMap();
    }

    public void validateByType( Entity entity, Long sampleId) {
        String entityType = entity.getEntityTypeName();
        TypeValidator validator = validatorMap.get( entityType );
        if ( validator != null ) {
            try {
                validator.validate( entity, sampleId );
            } catch ( Exception ex ) {
                validationLogger.reportError("Exception while trying to validate: " + entity.getName() + "/" + entity.getId() + " under sample " + sampleId + "::" + ex.getMessage());
            }
        }
    }

    private void createValidatorMap() {
        validationLogger = new ValidationLogger( logger );
        validatorMap = new HashMap<>();
        validatorMap.put( EntityConstants.TYPE_SAMPLE, new SampleValidator(validationLogger) );
        validatorMap.put( EntityConstants.TYPE_NEURON_FRAGMENT, new NeuronFragmentValidator( validationLogger ) );
        validatorMap.put( EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, new NeuronSeparatorPipelineResultValidator(validationLogger));

        validatorMap.put( EntityConstants.TYPE_IMAGE_2D, new SimpleFilePathValidator(validationLogger, EntityConstants.TYPE_IMAGE_2D, 1000L) );
        validatorMap.put( EntityConstants.TYPE_MOVIE, new SimpleFilePathValidator(validationLogger, EntityConstants.TYPE_MOVIE, 1000L) );
        validatorMap.put( EntityConstants.TYPE_IMAGE_3D, new SimpleFilePathValidator(validationLogger, EntityConstants.TYPE_IMAGE_2D, 1000L) );
    }

}
