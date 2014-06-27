package org.janelia.it.jacs.compute.service.validation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.PrototypeValidatable;
import org.janelia.it.jacs.compute.process_result_validation.content_checker.SimpleVHF;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.compute.service.entity.sample.SampleHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Validation proceeds from here.
 *
 * Created by fosterl on 6/17/14.
 */
@SuppressWarnings("unused")
public class ValidationService extends AbstractEntityService {
    private Logger logger = Logger.getLogger(ValidationService.class);

    private FileDiscoveryHelper fileHelper;
    private SampleHelper sampleHelper;

    @Override
    protected void execute() throws Exception {
        String dataSetName = (String) processData.getItem("DATA_SET_NAME");
        Long sampleId = (Long)processData.getItem("SAMPLE_ID");
        logger.info("Running validation, ownerKey=" + ownerKey +
                ", dataSetName=" + dataSetName + ", sampleId=" + sampleId);

        this.fileHelper = new FileDiscoveryHelper(entityBean, computeBean, ownerKey, logger);
        this.sampleHelper = new SampleHelper(entityBean, computeBean, annotationBean, ownerKey, logger);

        Map<String,PrototypeValidatable> validationMap = new SimpleVHF().getValidatables();

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
                    traverse(sampleChild.getId(), validationMap);
                }
            }
        }
        else {
            traverse(sampleId, validationMap);
        }
    }

    /** Recursive descent of entity by ID. */
    private void traverse( Long parentId, Map<String,PrototypeValidatable> validationMap ) throws Exception {
        Collection<Entity> children = entityBean.getChildEntities( parentId );
        for ( Entity child: children ) {
            PrototypeValidatable pv = validationMap.get( child.getEntityTypeName() );
            if ( pv != null ) {
            }
        }
    }
}
