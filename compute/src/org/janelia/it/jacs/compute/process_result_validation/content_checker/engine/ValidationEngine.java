package org.janelia.it.jacs.compute.process_result_validation.content_checker.engine;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.*;
import java.util.*;

/**
 * This can validate things by type.
 * Created by fosterl on 6/27/14.
 */
public class ValidationEngine implements Closeable {
    public static final String REPORT_FILE_EXTENSION = ".report.tsv";
    private static Logger logger = Logger.getLogger(ValidationEngine.class);
    private ValidationLogger validationLogger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;

    private File reportFile;
    private Map<String,TypeValidator> validatorMap;

    @SuppressWarnings("unused")
    public ValidationEngine(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, Boolean debug) throws IOException, ComputeException {
        this( entityBean, computeBean, annotationBean, debug, null, null, "generic" );
    }

    public ValidationEngine(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, Boolean debug, Long loggerId, Long startingId, String label) throws IOException, ComputeException {
        Entity loggerEntity = entityBean.getEntityAndChildren( loggerId );
        String metaData = String.format(
                "Validation Report for %s Named %s with ID %d\nProduced by %s",
                loggerEntity.getEntityTypeName(),
                loggerEntity.getName(),
                loggerId,
                this.getClass().getName()
        );
        validationLogger = new ValidationLogger( logger, metaData );
        File directory = new File(System.getProperty("user.home"));
        if ( startingId == null ) {
            directory = new File( directory, "Validation_All" );
        }
        else {
            directory = new File( directory, "Validation_" + label );
        }
        if ( ! directory.exists() ) {
            directory.mkdirs();
        }
        reportFile = File.createTempFile(
                this.getClass().getSimpleName(),
                (loggerId == null ? "" : "." + loggerId) + REPORT_FILE_EXTENSION,
                directory
        );
        PrintWriter pw = new PrintWriter(
                new FileWriter(
                        reportFile
                )
        );
        validationLogger.setPrintWriter(pw);
        validationLogger.setToReportPositives(debug);
        this.entityBean = entityBean;
        this.computeBean = computeBean;
        this.annotationBean = annotationBean;
        createValidatorMap();
    }

    public void validateByType( Entity entity, Long sampleId ) {
        String entityType = entity.getEntityTypeName();
        TypeValidator validator = validatorMap.get( entityType );
        if ( validator != null ) {
            validationLogger.addValidatedType(entityType);
            try {
                validator.validate( entity, sampleId );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                validationLogger.reportError(sampleId, entity, EntityConstants.TYPE_SAMPLE, ValidationLogger.GENERAL_CATEGORY_EXCEPTION, "Exception: " + ex.getMessage());
                throw new RuntimeException("Halting");
            }
        }
        else {
            validationLogger.addUnvalidatedType(entityType);
        }
    }

    public void close() {
        validationLogger.close();
        renameToReflectStatus(validationLogger.getFinalStatus());
    }

    private void renameToReflectStatus(ValidationLogger.Status status) {
        String beforeExt = reportFile.getName().substring( 0 , reportFile.getName().length() - REPORT_FILE_EXTENSION.length() );
        reportFile.renameTo( new File( reportFile.getParent(), beforeExt + "." + status + REPORT_FILE_EXTENSION ) );
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
