package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Put's the sample's data set identifier list into DATA_SET_IDENTIFIER.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GetSampleDataSetsService extends AbstractDomainService {

    public void execute() throws Exception {

        SampleHelperNG sampleHelper = new SampleHelperNG(ownerKey, logger, contextLogger);
        Sample sample = sampleHelper.getRequiredSample(data);
        
        contextLogger.info("Retrieved sample: "+sample.getName()+" (id="+sample.getId()+")");

        String dataSetStr = sample.getDataSet();

        if (dataSetStr==null) {
            logger.warn("Sample is not part of a dataset, id="+sample.getId());
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
