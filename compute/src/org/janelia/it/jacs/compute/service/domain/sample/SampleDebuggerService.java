package org.janelia.it.jacs.compute.service.domain.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.tasks.TaskParameter;

/**
 * Print the sample being processed.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleDebuggerService extends AbstractDomainService {

	private static final Logger logger = Logger.getLogger(SampleDebuggerService.class);

	public void execute() throws Exception {
		
		contextLogger.info("----------------------------------------------------------");
		contextLogger.info("Task Id: "+task.getObjectId());
		contextLogger.info("Task Name: "+task.getDisplayName());
		contextLogger.info("Task Parameters: ");
		for (TaskParameter parameter : task.getTaskParameterSet()) {
			contextLogger.info("    "+parameter.getName()+": "+parameter.getValue());
		}

        SampleHelperNG sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        Sample sample = sampleHelper.getRequiredSample(data);
	
        contextLogger.info("    Sample Id: "+sample.getId());
        contextLogger.info("    Name: "+sample.getName());
		contextLogger.info("    Data Set: "+sample.getDataSet());
		contextLogger.info("    Objectives: "+sample.getObjectives());
    }
}
