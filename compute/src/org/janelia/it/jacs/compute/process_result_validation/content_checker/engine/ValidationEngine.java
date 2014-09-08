package org.janelia.it.jacs.compute.process_result_validation.content_checker.engine;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.engine.launcher.LauncherException;
import org.janelia.it.jacs.compute.engine.util.JmsUtil;
import org.janelia.it.jacs.compute.jtc.AsyncMessageInterface;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator.*;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.validation.ValidationRunNode;

import java.io.*;
import java.util.*;

/**
 * This can validate things by type.
 * Created by fosterl on 6/27/14.
 */
public class ValidationEngine implements Closeable {
    public static final String REPORT_DESCRIPTION_PREFIX = "Validation Report for";
    public static final int PUTATIVE_MAX_LOG_SIZE = 300000;
    public static final String REPORT_FILE_EXTENSION = ".report.tsv";
    public static final String TYPE_OCC_DELIM = "^";
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    private static Logger logger = Logger.getLogger(ValidationEngine.class);
    private ValidationLogger validationLogger;
    protected EntityBeanLocal entityBean;
    protected ComputeBeanLocal computeBean;
    protected AnnotationBeanLocal annotationBean;

    private Map<String,TypeValidator> validatorMap;
    private CharArrayWriter caw;
    private File directory;
    private Long loggerId;

    @SuppressWarnings("unused")
    public ValidationEngine(EntityBeanLocal entityBean, ComputeBeanLocal computeBean, AnnotationBeanLocal annotationBean, Boolean debug) throws IOException, ComputeException {
        this( entityBean, computeBean, annotationBean, debug, null, null, null, "generic" );
    }

    public ValidationEngine(
            EntityBeanLocal entityBean,
            ComputeBeanLocal computeBean,
            AnnotationBeanLocal annotationBean,
            Boolean debug,
            Long loggerId,
            String nodeDirectory,
            String[] specificTypes,
            String label
    ) throws IOException, ComputeException {
        Entity loggerEntity = entityBean.getEntityAndChildren( loggerId );
        List<Entity> annotations = annotationBean.getAnnotationsForEntity( loggerEntity.getOwnerKey(), loggerEntity.getId() );
        String metaData = String.format(
                REPORT_DESCRIPTION_PREFIX + " %s Named %s with ID %d, having %d annotations, owned by %s\nProduced by %s",
                loggerEntity.getEntityTypeName(),
                loggerEntity.getName(),
                loggerId,
                annotations.size(),
                loggerEntity.getOwnerKey(),
                this.getClass().getName()
        );
        validationLogger = new ValidationLogger( logger, metaData );
        caw = new CharArrayWriter( PUTATIVE_MAX_LOG_SIZE );
        directory = getBaseDirectory(label, nodeDirectory);
        this.loggerId = loggerId;

        PrintWriter pw = new PrintWriter( caw );

        validationLogger.setPrintWriter(pw);
        validationLogger.setToReportPositives(debug);
        this.entityBean = entityBean;
        this.computeBean = computeBean;
        this.annotationBean = annotationBean;
        createValidatorMap();
        if ( specificTypes != null  &&  specificTypes.length > 0 ) {
            Map<String,TypeValidator> smallValidatorMap = new HashMap<>();
            StringBuilder rejectedTypes = new StringBuilder();
            for ( String singleType: specificTypes ) {
                TypeValidator validator = validatorMap.get( singleType );
                if ( validator != null ) {
                    smallValidatorMap.put( singleType, validator );
                }
                else {
                    if ( rejectedTypes.length() > 0 )
                        rejectedTypes.append(TYPE_OCC_DELIM);
                    rejectedTypes.append(singleType);
                }
            }
            this.validatorMap = smallValidatorMap;

            if ( rejectedTypes.length() > 0 ) {
                throw new IllegalArgumentException(
                        "Type indicated with " + label + " has unknown types. Specifically " + rejectedTypes + "."
                );
            }
        }

    }

    public void validateByType( Entity entity, Long sampleId ) {
        String entityType = entity.getEntityTypeName();
        TypeValidator validator = validatorMap.get(entityType);
        if ( validator != null ) {
            validationLogger.addValidatedType(entityType);
            try {
                validator.validate( entity, sampleId );
            } catch ( Exception ex ) {
                ex.printStackTrace();
                validationLogger.reportError(sampleId, entity, EntityConstants.TYPE_SAMPLE, ValidationLogger.GENERAL_CATEGORY_EXCEPTION, "Exception: " + ex.getMessage());
                throw new RuntimeException("Halting validation run for " + sampleId);
            }
        }
        else {
            validationLogger.addUnvalidatedType(entityType);
        }
    }

    @Override
    public void close() throws IOException {
        validationLogger.close();

        // Dispose of collected information.
        String fileSectionName = this.getClass().getSimpleName() +
                (loggerId == null ? "" : "." + loggerId) + ValidationLogger.SAMPLE_BREAK_TAG;
        File reportFile = new File(
                directory,
                directory.getName() + "." + validationLogger.getFinalStatus() + REPORT_FILE_EXTENSION
        );
        Map<String,String> map = new HashMap<>();
        map.put( ValidationLogger.FILE_SECTION_NAME_PARAM, fileSectionName );
        map.put( ValidationLogger.REPORT_FILE_PATH_PARAM, reportFile.getAbsolutePath() );
        map.put( ValidationLogger.FILE_CONTENT_PARAM, caw.toString() );
        AsyncMessageInterface messageInterface = JmsUtil.createAsyncMessageInterface();

        try {
            JmsUtil.sendMessageToQueue( messageInterface, map, ValidationLogger.NON_CONCURRENT_WRITE_QUEUE );
        } catch ( LauncherException le ) {
            throw new IOException( le );
        }

    }

    /**
     * Establishes the directory location into which to put the output log.
     *
     * @param label used in establishing a meaningfully-named container directory.
     * @return directory place to write log.
     */
    private File getBaseDirectory(String label, String nodeDirectory) {
        // Find the top-level task for this validation.
        File directory = null;
        if ( nodeDirectory == null ) {
            directory = new File( System.getProperty("user.home") );
        }
        else {
            directory = new File( nodeDirectory );
        }

        // Using this approach to file construction allows the label to contain sub directories.
        if ( label == null ) {
            label = "validation";
        }
        directory = new File( directory.getAbsolutePath() + FILE_SEPARATOR + ValidationRunNode.sanitizeDirName( label ) );
        if ( ! directory.exists() ) {
            if ( ! directory.mkdirs() ) {
                logger.warn( "Failed to create directory hierarchy. " + directory.getName() );
            }
        }
        return directory;
    }

    private void createValidatorMap() {
        validationLogger.setMinSize( EntityConstants.TYPE_IMAGE_2D, 50L );
        validationLogger.setMinSize( EntityConstants.TYPE_MOVIE, 1000L );
        validationLogger.setMinSize( EntityConstants.TYPE_IMAGE_3D, 5000L );
        validationLogger.setMinSize( ".mask", 92L );  // 10 longs and 3 floats.
        validationLogger.setMinSize( ".chan", 1L );
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
        validatorMap.put( EntityConstants.TYPE_IMAGE_3D, new SimpleFilePathValidator( validationLogger, EntityConstants.TYPE_IMAGE_3D) );

        validatorMap.put( EntityConstants.TYPE_IMAGE_TILE, new ImageTileValidator(validationLogger, subEntityValidator, entityBean) );
        validatorMap.put( EntityConstants.TYPE_LSM_STACK, new LsmValidator(validationLogger ) );
        validatorMap.put( EntityConstants.TYPE_PIPELINE_RUN, new PipelineRunValidator( validationLogger, entityBean ) );
    }

}
