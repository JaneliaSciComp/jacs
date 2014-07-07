package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.engine.ValidationEngine;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Validation proceeds from here.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class ValidationService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(ValidationService.class);

    //private FileDiscoveryHelper fileHelper;
    private SampleHelper sampleHelper;
    private ValidationEngine validationEngine;

    private Long sampleId;

    @Override
    protected void execute() throws Exception {
        sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);
        validationEngine = new ValidationEngine(entityBean, computeBean, annotationBean);

        String dataSetName = (String) processData.getItem("DATA_SET_NAME");
        String sampleEntityIdStr = (String) processData.getItem("SAMPLE_ENTITY_ID");
        if ( sampleEntityIdStr != null  &&  sampleEntityIdStr.trim().length() > 0 ) {
            sampleId = Long.parseLong(sampleEntityIdStr);
        }
        logger.info("Running validation, ownerKey=" + ownerKey +
                ", dataSetName=" + dataSetName + ", sampleId=" + sampleId);

        //this.fileHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);

        if ( sampleId == null ) {
            Collection<Entity> dataSets = new ArrayList<>();
            if ( dataSetName == null || dataSetName.trim().length() == 0 ) {
                dataSets.addAll(sampleHelper.getDataSets());
            }
            else {
                dataSets.addAll(entityBean.getUserEntitiesByNameAndTypeName(ownerKey, dataSetName, EntityConstants.TYPE_DATA_SET));
            }

            // Iterate over all data sets.
            for ( Entity dataSetEntity: dataSets ) {
                // Look for all the datasets' samples.
                Collection<Entity> sampleChildEntities = entityBean.getChildEntities(dataSetEntity.getId());
                for ( Entity sampleChild: sampleChildEntities ) {
                    traverse(sampleChild.getId());
                }
            }
        }
        else {
            traverse(sampleId);
        }
        // Follow up with category list.  This may be modified at some point, as reports lines are cached
        // within the validating engine, and grouped by their categories on report-back.
        validationEngine.writeCategories();
    }

    /** Recursive descent of entity by ID. */
    private void traverse( Long parentId ) throws Exception {
        Collection<Entity> children = entityBean.getChildEntities( parentId );
        for ( Entity child: children ) {
            validationEngine.validateByType( child, sampleId );
            traverse(child.getId());
        }
    }
}
