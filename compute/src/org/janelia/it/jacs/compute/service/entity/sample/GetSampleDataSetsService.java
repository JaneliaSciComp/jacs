package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Put's the sample's data set identifier list into DATA_SET_IDENTIFIER.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetSampleDataSetsService extends AbstractEntityService {

    public void execute() throws Exception {

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (sampleEntityId == null || "".equals(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }

        Entity sampleEntity = entityBean.getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }

        if (!EntityConstants.TYPE_SAMPLE.equals(sampleEntity.getEntityTypeName())) {
            throw new IllegalArgumentException("Entity is not a sample: "+sampleEntityId);
        }

        contextLogger.info("Retrieved sample: "+sampleEntity.getName()+" (id="+sampleEntityId+")");

        String dataSetStr = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER);
        
        if (dataSetStr==null) {
            logger.warn("Sample is not part of a dataset, id="+sampleEntityId);
        }
        else {
            List<String> dataSetList = new ArrayList<String>();
            for (String dataSetIdentifier : dataSetStr.split(",")) {
                dataSetList.add(dataSetIdentifier);
            }
            contextLogger.info("Putting ("+Task.csvStringFromCollection(dataSetList)+") in DATA_SET_IDENTIFIER");
            processData.putItem("DATA_SET_IDENTIFIER", dataSetList);
        }
    }
}
