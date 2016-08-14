package org.janelia.it.jacs.compute.service.domain.sample;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.tasks.Task;

/**
 * Returns all the samples for the task owner which match the parameters. Parameters must be provided in the ProcessData:
 *   OUTVAR_ENTITY_ID (The output variable to populate with a List of Entities)
 *   RUN_MODE (Mode to use for including entities)
 *     NONE - Don't run any samples.
 *     NEW - Include Samples which have no Pipeline Runs. 
 *     ERROR - Include Samples which have errors in their latest Pipeline Runs.
 *     MARKED - Include Samples which have been Marked for Rerun by their owner.
 *     ALL - Include every Sample.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTraversalService extends AbstractDomainService {

	public static final String RUN_MODE_NONE = "NONE";
	public static final String RUN_MODE_ALL = "ALL";
	public static final String RUN_MODE_NEW = "NEW";
	public static final String RUN_MODE_ERROR = "ERROR";
	public static final String RUN_MODE_MARKED = "MARKED";

    protected boolean includeAllSamples = false;
    protected boolean includeNewSamples = false;
    protected boolean includeErrorSamples = false;
    protected boolean includeMarkedSamples = false;

    public void execute() throws Exception {
    	
    	boolean outputObjects = false;
    	String outvar = (String)processData.getItem("OUTVAR_ENTITY_ID");
    	if (outvar == null) {
        	outvar = (String)processData.getItem("OUTVAR_ENTITY");
        	outputObjects = true;
        	if (outvar == null) {
        		throw new IllegalArgumentException("Both OUTVAR_ENTITY_ID and OUTVAR_ENTITY may not be null");
        	}
    	}

        logger.info("Traversing samples owned by "+ownerKey+", with rules:");
        
        List<Object> outObjects = new ArrayList<>();
        
        String dataSetName = (String) processData.getItem("DATA_SET_NAME");
        logger.info("    dataSetName="+dataSetName);
        
        String runModeList = (String)processData.getItem("RUN_MODE");
        logger.info("    runMode="+runModeList);
        
        for(String runMode : Task.listOfStringsFromCsvString(runModeList)) {
            if (RUN_MODE_NEW.equals(runMode)) {
                includeNewSamples = true;
            } 
            else if (RUN_MODE_ERROR.equals(runMode)) {
                includeErrorSamples = true;
            }
            else if (RUN_MODE_MARKED.equals(runMode)) {
                includeMarkedSamples = true;
            }
            else if (RUN_MODE_ALL.equals(runMode)) {
                includeAllSamples = true;
            }
            else if (RUN_MODE_NONE.equals(runMode)) {
                // No samples will be selected

                logger.info("Putting empty list ids in "+outvar);
                processData.putItem(outvar, outObjects);
                return;
            }
            else {
                throw new IllegalStateException("Illegal mode: "+runMode);    
            }
        }
    
        List<Sample> samples;
        if (dataSetName == null) {
            samples = domainDao.getUserDomainObjects(ownerKey, Sample.class);
        } 
        else {
            List<DataSet> dataSets = domainDao.getUserDomainObjectsByName(ownerKey, DataSet.class, dataSetName);
            if (dataSets.size() == 1) {
                DataSet dataSet = dataSets.get(0);
                samples = domainDao.getActiveSamplesForDataSet(ownerKey, dataSet.getIdentifier());
            } 
            else {
                throw new IllegalArgumentException("found " + dataSets.size() + " entities for " + ownerKey
                        + " data set '" + dataSetName + "' when only one is expected");
            }
        }

		logger.info("Found " + samples.size() + " Samples. Filtering with rules:");
		logger.info("    includeNewSamples="+includeNewSamples);
		logger.info("    includeErrorSamples="+includeErrorSamples);
		logger.info("    includeAllSamples="+includeAllSamples);
		
    	for(Sample sample : samples) {
    	    if (isIncludedSample(sample)) {
    	        outObjects.add(outputObjects ? sample : sample.getId().toString()); 
    	    }
    	}

		logger.info("Putting "+outObjects.size()+" ids in "+outvar);
    	processData.putItem(outvar, outObjects);
    }
    
    private boolean isIncludedSample(Sample sample) throws Exception {
        
        String status = sample.getStatus();

        if (includeAllSamples) {
            logger.info("Included " + sample.getName() + " (id=" + sample.getId() + ") - all");
            return true;
        }
        
        if (includeMarkedSamples && isMarked(status)) {
            logger.info("Included " + sample.getName() + " (id=" + sample.getId() + ") - marked");
            return true;
        }
        
        if (isBlocked(status)) {
            logger.info("Excluded " + sample.getName() + " (id=" + sample.getId() + ") - blocked");
            return false;
        }

        if (isDesync(status) || !sample.getSageSynced()) {
            logger.info("Excluded " + sample.getName() + " (id=" + sample.getId() + ") - desync");
            return false;
        }
        
        if (isRetired(status)) {
            logger.info("Excluded " + sample.getName() + " (id=" + sample.getId() + ") - retired");
            return false;
        }
        
        for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
            if (includeSample(objectiveSample)) {
                logger.info("Included " + sample.getName() + " (id=" + sample.getId() + ") - incomplete");
                return true;
            }
        }

        logger.info("Excluded " + sample.getName() + " (id=" + sample.getId() + ")");
        return false;
    }

    private boolean includeSample(ObjectiveSample objectiveSample) throws Exception {

        if (includeNewSamples && objectiveSample.getLatestRun()==null) {
            return true;
        }

        if (includeErrorSamples) {
            SamplePipelineRun run = objectiveSample.getLatestRun();
            if (run!=null) {
                if (run.getError()!=null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isMarked(String status) {
        return (status != null && DomainConstants.VALUE_MARKED.equals(status));
    }

    private boolean isBlocked(String status) {
        return (status != null && DomainConstants.VALUE_BLOCKED.equals(status));
    }

    private boolean isDesync(String status) {
        return (status != null && DomainConstants.VALUE_DESYNC.equals(status));
    }
    
    private boolean isRetired(String status) {
        return (status != null && DomainConstants.VALUE_RETIRED.equals(status));
    }
}
