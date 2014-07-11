package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.mbean.Validator;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.engine.ValidationEngine;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Collection;

/**
 * Validation proceeds from here.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class ValidationService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(ValidationService.class);

    private Boolean nodebug;
    private Long startingId;

    @Override
    protected void execute() throws Exception {
        // We'll accept a long if offered, but fall back to String.
        Object guidObj = processData.getItem("GUID");
        if ( guidObj == null ) {
            this.startingId = null;
        }
        else if ( guidObj instanceof Long ) {
            Long guid = (Long) processData.getItem("GUID");
            if ( guid == null  ||  guid <= 0 ) {
                this.startingId = null;
            }
        }
        else {
            this.startingId = Long.parseLong( guidObj.toString() );
        }

        nodebug = (Boolean) processData.getItem("NODEBUG");

        // NOTE: the default value for any boolean through JMX (from JBoss, at least) is TRUE.
        //  Therefore, we want our most-common-case to match TRUE.  So TRUE-> do NOT issue debug info.
        logger.info(
                "Running validation, ownerKey=" + ownerKey +
                ", startingId=" + this.startingId +
                ", omitDebugInfo=" + nodebug + "."
        );

        //this.fileHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        //SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        Entity entity = entityBean.getEntityAndChildren(startingId);
        if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
            validateSample( startingId, entity );
        }
        else {
            traverseForSamples(entity);
        }

    }

    /**  Recursively search for samples within the given tree.  When one is found, traverse it for validation. */
    private void traverseForSamples(Entity entity) throws Exception {
        if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
            // At this point, launch a new copy of this service, providing it the sample ID as start-point.
            // This permits the collection of samples to be processed in parallel.
            Validator validator = new Validator();
            validator.runValidations(ownerKey, entity.getId(), nodebug);
        }
        else {
            for ( Entity child: entity.getChildren() ) {
                Entity childEntity = entityBean.getEntityAndChildren( child.getId() );
                traverseForSamples( childEntity );
            }
        }
    }

    /** Recursive descent of entity by ID. */
    private void traverseForValidation( Long parentId, Long sampleId, ValidationEngine validationEngine ) throws Exception {
        Collection<Entity> children = entityBean.getChildEntities( parentId );
        for ( Entity child: children ) {
            if ( child.getId() == 1803764205405347938L) {
                System.out.println("Found our broken sample.");
            }
            validationEngine.validateByType( child, sampleId );
            traverseForValidation( child.getId(), sampleId, validationEngine );
        }
    }

    private void validateSample(Long knownSampleId, Entity entity) throws Exception {
        ValidationEngine validationEngine = new ValidationEngine(entityBean, computeBean, annotationBean, (!nodebug), knownSampleId);

        // Do not look for samples under samples.  Do not recurse further here.  Instead, look for
        // other things to validate.
        traverseForValidation( entity.getId(), knownSampleId, validationEngine );

        // Follow up with category list.  This may be modified at some point, as reports lines are cached
        // within the validating engine, and grouped by their categories on report-back.
        validationEngine.writeCategories();
    }
}
