package org.janelia.it.jacs.compute.process_result_validation.content_checker.type_validator;

import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.process_result_validation.ValidationLogger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Validate contents of a pipeline run.
 *
 * Created by fosterl on 6/27/14.
 */
public class PipelineRunValidator implements TypeValidator {
    private static final ValidationLogger.Category ERRORED_PIPELINE_RUN = new ValidationLogger.Category("Error Entity under Pipeline Run");

    private EntityBeanLocal entityBean;
    private ValidationLogger validationLogger;

    public PipelineRunValidator(  ValidationLogger logger, EntityBeanLocal entityBean ) {
        this.entityBean = entityBean;
        this.validationLogger = logger;
        validationLogger.addCategory( ERRORED_PIPELINE_RUN );
    }
    /**
     * Check: all expected children on hand?
     *
     * @param entity check this pipeline run.
     * @param sampleId ...under this sample--for identification purposes.
     * @throws Exception for called methods.
     */
    @Override
    public void validate(Entity entity, Long sampleId) throws Exception {
        // ValidationLogger logger, SubEntityValidator subEntityValidator, EntityBeanLocal entityBean
        boolean reportableSuccess = true;
        Entity pipelineRun = entityBean.getEntityAndChildren( entity.getId() );
        for ( Entity child: pipelineRun.getChildren() ) {
            if ( child.getName().equals( "Error" ) ) {
                validationLogger.reportError(
                        sampleId,
                        pipelineRun,
                        ERRORED_PIPELINE_RUN,
                        "An entity child named Error appears under this pipeline run."
                );
                reportableSuccess = false;
            }
        }

        if ( validationLogger.isToReportPositives()  &&  reportableSuccess ) {
            validationLogger.reportSuccess( entity.getId(), EntityConstants.TYPE_PIPELINE_RUN + " : has no error entity child.");
        }

    }
}
