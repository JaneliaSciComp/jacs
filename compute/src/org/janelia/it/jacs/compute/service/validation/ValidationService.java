package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.mbean.Validator;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.engine.ValidationEngine;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.Collection;
import java.util.List;

/**
 * Validation proceeds from here.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class ValidationService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(ValidationService.class);
    private final static String FILE_SEP = System.getProperty("file.separator");   // Ex: slash in linux or win.

    private Boolean nodebug;
    private Long startingId;
    private String parentName;
    private String label;

    @Override
    protected void execute() throws Exception {
        startingId = getGuidItem( "GUID" );
        nodebug = (Boolean) processData.getItem("NODEBUG");
        label = (String) processData.getItem( "LABEL" );

        // NOTE: the default value for any boolean through JMX (from JBoss, at least) is TRUE.
        //  Therefore, we want our most-common-case to match TRUE.  So TRUE-> do NOT issue debug info.
        logger.info(
                "Running validation, ownerKey=" + ownerKey +
                ", startingId=" + this.startingId +
                ", omitDebugInfo=" + nodebug +
                ", label=" + label + "."
        );

        if ( startingId == null ) {
            List<Entity> foundEntities = entityBean.getEntitiesByTypeName( EntityConstants.TYPE_DATA_SET );
            for ( Entity foundEntity : foundEntities ) {

                List<Entity> foundSubEntities = entityBean.getEntitiesByNameAndTypeName(
                        null, foundEntity.getName(), EntityConstants.TYPE_FOLDER
                );
                // We build up a parent name out of the entity name and its ID, but we escape any accidental file-seps.
                parentName = foundEntity.getName().replace(FILE_SEP, "__") + "_" + foundEntity.getId();
                for ( Entity subEntity: foundSubEntities ) {
                    traverseForSamples(subEntity);
                }
            }
        }
        else {
            Entity entity = entityBean.getEntityAndChildren(startingId);
            if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
                validateSample( startingId, entity );
            }
            else {
                traverseForSamples(entity);
            }
        }

    }

    private Long getGuidItem( String itemName ) {
        Long rtnVal = null;
        // We'll accept a long if offered, but fall back to String.
        Object guidObj = processData.getItem( itemName );
        if ( guidObj == null ) {
            rtnVal = null;
        }
        else if ( guidObj instanceof Long ) {
            Long guid = (Long) processData.getItem( itemName );
            if ( guid == null  ||  guid <= 0 ) {
                rtnVal = null;
            }
        }
        else {
            rtnVal = Long.parseLong( guidObj.toString() );
        }
        return rtnVal;
    }

    /**  Recursively search for samples within the given tree.  When one is found, traverse it for validation. */
    private void traverseForSamples(Entity entity) throws Exception {
        if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
            // At this point, launch a new copy of this service, providing it the sample ID as start-point.
            // This permits the collection of samples to be processed in parallel.
            Validator validator = new Validator();
            validator.runValidations(ownerKey, entity.getId(), label + FILE_SEP + parentName, nodebug);
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
            validationEngine.validateByType( child, sampleId );
            traverseForValidation( child.getId(), sampleId, validationEngine );
        }
    }

    private void validateSample(Long knownSampleId, Entity entity) throws Exception {
        ValidationEngine validationEngine = new ValidationEngine(
                entityBean, computeBean, annotationBean, (!nodebug), knownSampleId, label
        );

        // Do not look for samples under samples.  Do not recurse further here.  Instead, look for
        // other things to validate.
        traverseForValidation( entity.getId(), knownSampleId, validationEngine );

        // Signal to the engine that its work is complete.
        validationEngine.close();
    }
}
