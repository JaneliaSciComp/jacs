package org.janelia.it.jacs.compute.service.entity.sample;

import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;

/**
 * Decides which types of processing will be run for the sample. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ChooseSummaryPipelineStepsService extends AbstractDomainService {

    public void execute() throws Exception {

        data.putItem("RUN_SUMMARY", true);
    }
}
