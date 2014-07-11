package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
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
    private Long guid;

    @Override
    protected void execute() throws Exception {
        // We'll accept a long if offered, but fall back to String.
        Object guidObj = processData.getItem("GUID");
        if ( guidObj == null ) {
            this.guid = null;
        }
        else if ( guidObj instanceof Long ) {
            Long guid = (Long) processData.getItem("GUID");
            if ( guid == null  ||  guid <= 0 ) {
                this.guid = null;
            }
        }
        else {
            this.guid = Long.parseLong( guidObj.toString() );
        }

        nodebug = (Boolean) processData.getItem("NODEBUG");

        // NOTE: the default value for any boolean through JMX (from JBoss, at least) is TRUE.
        //  Therefore, we want our most-common-case to match TRUE.  So TRUE-> do NOT issue debug info.
        logger.info(
                "Running validation, ownerKey=" + ownerKey +
                ", guid=" + this.guid +
                ", debug=" + nodebug + "."
        );

        //this.fileHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        //SampleHelper sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        traverseForSamples(this.guid);

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

    /**  Recursively search for samples within the given tree.  When one is found, traverse it for validation. */
    private void traverseForSamples(Long guid) throws Exception {
        Entity entity = entityBean.getEntityAndChildren( guid );
        if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {

            ValidationEngine validationEngine;
            validationEngine = new ValidationEngine(entityBean, computeBean, annotationBean, (!nodebug), guid);

            // Do not look for samples under samples.  Do not recurse further here.  Instead, look for
            // other things to validate.
            traverseForValidation( entity.getId(), guid, validationEngine );

            // Follow up with category list.  This may be modified at some point, as reports lines are cached
            // within the validating engine, and grouped by their categories on report-back.
            validationEngine.writeCategories();

        }
        else {
            for ( Entity child: entity.getChildren() ) {
                traverseForSamples( child.getId() );
            }
        }
    }
}
