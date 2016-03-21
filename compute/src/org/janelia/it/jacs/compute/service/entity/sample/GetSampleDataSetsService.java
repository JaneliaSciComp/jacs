package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Put's the sample's data set identifier list into DATA_SET_IDENTIFIER.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetSampleDataSetsService extends AbstractDomainService {

    public void execute() throws Exception {

        String sampleEntityId = (String)processData.getItem("SAMPLE_ENTITY_ID");
        if (sampleEntityId == null || "".equals(sampleEntityId)) {
            throw new IllegalArgumentException("SAMPLE_ENTITY_ID may not be null");
        }

        Sample sample = domainDao.getDomainObject(ownerKey, Sample.class, new Long(sampleEntityId));
        if (sample == null) {
            throw new IllegalArgumentException("Sample entity not found with id="+sampleEntityId);
        }

        contextLogger.info("Retrieved sample: "+sample.getName()+" (id="+sampleEntityId+")");

        String dataSetStr = sample.getDataSet();

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
