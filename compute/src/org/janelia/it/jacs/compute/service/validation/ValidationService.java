package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.ComputeException;
import org.janelia.it.jacs.compute.mbean.Validator;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.engine.ValidationEngine;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.validation.ValidationRunNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
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

    private Boolean nodebug;
    private Long startingId;
    private String parentName;
    private String[] specificTypes;
    private String unprocessedTypesStr;
    private String label;
    private ValidationRunNode validationRunNode;

    @Override
    protected void execute() throws Exception {
        startingId = getGuidItem( "GUID" );
        nodebug = (Boolean) processData.getItem("NODEBUG");
        label = (String) processData.getItem( "LABEL" );
        unprocessedTypesStr = (String) processData.getItem("TYPES");
        if ( unprocessedTypesStr != null  &&  unprocessedTypesStr.trim().length() > 0 ) {
            specificTypes = unprocessedTypesStr.split(",");
        }

        getOrCreateResultNode();

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

                List<Entity> foundSubEntities = getLikeNamedFolderEntities(foundEntity);

                // We build up a parent name out of the entity name and its ID, but we escape any accidental file-seps.
                parentName = foundEntity.getName().replace(File.separator, "__") + "_" + foundEntity.getId();
                for ( Entity subEntity: foundSubEntities ) {
                    traverseForSamples(subEntity);
                }
            }
        }
        else {
            Entity entity = entityBean.getEntityAndChildren(startingId);
            parentName = entity.getName();
            if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_SAMPLE ) ) {
                validateSample( startingId, entity );
            }
            else {
                traverseForSamples(entity);
            }
        }

    }

    /**
     * In order to traverse Data Sets for samples, one must first locate a same-named folder, and traverse that.
     *
     * @param foundEntity some data set (only)
     * @return list of folders with identical names as the found entity.
     * @throws ComputeException from called methods.
     */
    private List<Entity> getLikeNamedFolderEntities(Entity foundEntity) throws ComputeException {
        return entityBean.getEntitiesByNameAndTypeName(
                            null, foundEntity.getName(), EntityConstants.TYPE_FOLDER
                    );
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
            validator.runChildValidations(task.getObjectId(), ownerKey, entity.getId(), label + File.separator + parentName, unprocessedTypesStr, nodebug);
        }
        else if ( entity.getEntityTypeName().equals( EntityConstants.TYPE_DATA_SET ) ) {
            Collection<Entity> traversableEntities = getLikeNamedFolderEntities( entity );
            for ( Entity traversableEntity: traversableEntities ) {
                // Case of folder that may contain samples, is just another part of containing if..else clause.
                traverseForSamples( traversableEntity );
            }
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

    /**
     * Find a result node defined against either this node's parent.  If none such exists, create one.
     * NOTE: this method relies on a two-level hierarchy among these tasks.  If that changes, this method breaks.
     *
     * @return ID of the validation run node.
     * @throws DaoException thrown by called methods.
     * @throws IOException thrown by called methods.
     */
    private Long getOrCreateResultNode() throws DaoException, IOException {
        Long parentTaskId = task.getParentTaskId();
        if ( parentTaskId == null ) {
            // Here, need establish the file node to hold the directory location.
            validationRunNode = new ValidationRunNode(
                    User.SYSTEM_USER_LOGIN,
                    task,
                    label,                           // name
                    "Validation Run: " + label,      // description
                    Node.VISIBILITY_PUBLIC,          // visibility
                    Node.DIRECTORY_DATA_TYPE,        // data type
                    null                             // relative session path
            );
            computeBean.saveOrUpdateNode(validationRunNode);
            FileUtil.ensureDirExists(validationRunNode.getDirectoryPath());
            FileUtil.cleanDirectory(validationRunNode.getDirectoryPath());

        }
        else {
            validationRunNode = (ValidationRunNode)computeBean.getResultNodeByTaskId( parentTaskId );
        }

        return validationRunNode.getObjectId();
    }

    private void validateSample(Long knownSampleId, Entity entity) throws Exception {

        try (
            ValidationEngine validationEngine = new ValidationEngine(
                entityBean, computeBean, annotationBean, (!nodebug), knownSampleId, validationRunNode.getDirectoryPath(), specificTypes, label
            )
        ) {
            validationEngine.validateByType( entity, entity.getId() );
            // Do not look for samples under samples.  Do not recurse further here.  Instead, look for
            // other things to validate.
            traverseForValidation( entity.getId(), knownSampleId, validationEngine );
        }

    }
}
