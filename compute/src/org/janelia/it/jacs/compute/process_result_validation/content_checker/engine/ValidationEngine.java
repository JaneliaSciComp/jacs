package org.janelia.it.jacs.compute.process_result_validation.content_checker.engine;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * This can validate things by type.
 * Created by fosterl on 6/27/14.
 */
public class ValidationEngine {
    private static Logger logger = Logger.getLogger(ValidationEngine.class);
    private ValidationLogger validationLogger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;

    private Map<String,TypeValidator> validatorMap;

    @SuppressWarnings("unused")
    public ValidationEngine(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, Boolean debug) throws IOException {
        this( entityBean, computeBean, annotationBean, debug, null );
    }

    public ValidationEngine(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, Boolean debug, Long loggerId) throws IOException {
        validationLogger = new ValidationLogger( logger );
        PrintWriter pw = new PrintWriter(
                new FileWriter(
                        File.createTempFile(
                                this.getClass().getSimpleName(),
                                (loggerId == null ? "" : "." + loggerId) + ".report.tsv",
                                new File( System.getProperty("user.home") )
                        )
                )
        );
        validationLogger.setPrintWriter(pw);
        validationLogger.setToReportPositives( debug );
        this.entityBean = entityBean;
        this.computeBean = computeBean;
        this.annotationBean = annotationBean;
        createValidatorMap();
    }

    public void validateByType( Entity entity, Long sampleId ) {
        String entityType = entity.getEntityTypeName();
        TypeValidator validator = validatorMap.get( entityType );
        if ( validator != null ) {
            try {
                validator.validate( entity, sampleId );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                validationLogger.reportError(entity.getId(), sampleId, EntityConstants.TYPE_SAMPLE, ValidationLogger.GENERAL_CATEGORY_EXCEPTION, "Exception: " + ex.getMessage());
                throw new RuntimeException("Halting");
            }
        }
    }

    public void writeCategories() {
        validationLogger.close();
    }

    private void createValidatorMap() {
        validationLogger.setMinSize( EntityConstants.TYPE_IMAGE_2D, 50L );
        validationLogger.setMinSize( EntityConstants.TYPE_MOVIE, 1000L );
        validationLogger.setMinSize( EntityConstants.TYPE_IMAGE_3D, 5000L );
        validationLogger.setMinSize( EntityConstants.TYPE_V3D_ANO_FILE, 100000L );

        SubEntityValidator subEntityValidator = new SubEntityValidator( validationLogger );
        validatorMap = new HashMap<>();
        validatorMap.put( EntityConstants.TYPE_SAMPLE, new SampleValidator(validationLogger, subEntityValidator, entityBean ) );
        validatorMap.put( EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT, new SampleProcessingValidator(validationLogger, subEntityValidator, entityBean));
        validatorMap.put( EntityConstants.TYPE_ALIGNMENT_RESULT, new AlignmentResultValidator(validationLogger, subEntityValidator, entityBean) );
        validatorMap.put( EntityConstants.TYPE_NEURON_FRAGMENT, new NeuronFragmentValidator(validationLogger) );
        validatorMap.put( EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT, new NeuronSeparatorPipelineResultValidator(validationLogger, entityBean) );

        validatorMap.put( EntityConstants.TYPE_IMAGE_2D, new SimpleFilePathValidator( validationLogger, EntityConstants.TYPE_IMAGE_2D) );
        validatorMap.put( EntityConstants.TYPE_MOVIE, new SimpleFilePathValidator( validationLogger, EntityConstants.TYPE_MOVIE) );
        validatorMap.put( EntityConstants.TYPE_IMAGE_3D, new SimpleFilePathValidator( validationLogger, EntityConstants.TYPE_IMAGE_2D) );

        validatorMap.put( EntityConstants.TYPE_IMAGE_TILE, new ImageTileValidator(validationLogger, subEntityValidator, entityBean) );
        validatorMap.put( EntityConstants.TYPE_LSM_STACK, new LsmValidator(validationLogger ) );
    }

}
