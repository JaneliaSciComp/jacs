package org.janelia.it.jacs.compute.service.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;

/**
 * Removes redundant (unannotated, not final) results from Samples or Sub-Samples. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleCleaningService extends AbstractDomainService {

    public transient static final String PARAM_testRun = "is test run";
    
    private boolean isDebug = false;
    private SampleHelperNG sampleHelper;
    private int numSamples = 0;
    private int numRunsDeleted = 0;
    
    public void execute() throws Exception {

        String testRun = task.getParameter(PARAM_testRun);
        if (testRun!=null) {
        	isDebug = Boolean.parseBoolean(testRun);	
        }            

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        
        logger.info("Cleaning old results from samples for user: "+ownerKey);
        
        List<Sample> samples = domainDao.getDomainObjects(ownerKey, Sample.class);

        logger.info("Will process "+samples.size()+" samples...");
        
        for(Sample sample : samples) {
        	processSample(sample);
        	numSamples++;
        }
        
        logger.info("Considered "+numSamples+" samples. Deleted "+numRunsDeleted+" results.");
    }
    
    private void processSample(Sample sample) throws Exception {
    	
    	logger.info("Cleaning up sample "+sample.getName());
    	int runsDeleted = 0;
    	for(ObjectiveSample objectiveSample : sample.getObjectiveSamples()) {
    	    runsDeleted += processSample(objectiveSample);
    	}

        numRunsDeleted += runsDeleted;
        
    	if (runsDeleted>0 && !isDebug) {
    	    logger.info("Saving changes to sample: "+sample.getId());
    	    sampleHelper.saveSample(sample);
    	}
    }
    
    private int processSample(ObjectiveSample objectiveSample) throws Exception {
        
    	List<SamplePipelineRun> runs = objectiveSample.getPipelineRuns();
    	if (runs.isEmpty()) return 0;
    	
    	// Group by pipeline process
    	Map<String,List<SamplePipelineRun>> processRunMap = new HashMap<String,List<SamplePipelineRun>>();

        for(SamplePipelineRun pipelineRun : runs) {
            String process = pipelineRun.getPipelineProcess();
            if (process == null) process = "";
            List<SamplePipelineRun> areaRuns = processRunMap.get(process);
            if (areaRuns == null) {
                areaRuns = new ArrayList<>();
                processRunMap.put(process,areaRuns);
            }
            areaRuns.add(pipelineRun);
        }
    	
        int numDeleted = 0;
        for(String process : processRunMap.keySet()) {
            List<SamplePipelineRun> processRuns = processRunMap.get(process);
            if (processRuns.isEmpty()) continue;
            
            logger.info("  Processing pipeline runs for process: "+process);
            
            // Remove latest run, we don't want to touch it
            SamplePipelineRun lastRun = processRuns.remove(processRuns.size()-1);

            if (lastRun.hasError()) {
                logger.info("    Keeping last error run: "+lastRun.getId());
                // Last run had an error, let's keep that, but still try to find a good run to keep
                Collections.reverse(processRuns);
                Integer keeper = null;
                int curr = 0;
                for(SamplePipelineRun pipelineRun : processRuns) {
                    if (!pipelineRun.hasError()) {
                        keeper = curr;
                        break;
                    }
                    curr++;   
                }
                if (keeper!=null) {
                    SamplePipelineRun lastGoodRun = processRuns.remove(keeper.intValue());
                    logger.info("    Keeping last good run: "+lastGoodRun.getId());
                    
                }
                else {
                    logger.info("    Could not find a good run to keep.");
                }
            }
            else {
                logger.info("    Keeping last good run: "+lastRun.getId());
            }
            
            // Clean up everything else
            numDeleted += deleteUnannotated(objectiveSample, processRuns);
        }
        
        return numDeleted;
    }
    
    private int deleteUnannotated(ObjectiveSample objectiveSample, List<SamplePipelineRun> toDelete) throws ComputeException {
    	
    	Set<SamplePipelineRun> toReallyDelete = new HashSet<>();
    	for(SamplePipelineRun pipelineRun : toDelete) {
    	    
    		long numFound = getNumNeuronsAnnotated(pipelineRun);
			if (numFound>0) {    			
            	logger.info("    Rejecting candidate "+pipelineRun.getId()+" because it contains neurons with "+numFound+" annotations");
            	continue;
    		}
    		toReallyDelete.add(pipelineRun);
    	}

    	if (toReallyDelete.isEmpty()) return 0;
    	logger.info("    Found "+toReallyDelete.size()+" non-annotated results for deletion:");
		for(SamplePipelineRun child : toReallyDelete) {
	        objectiveSample.removeRun(child);
			logger.info("      Will delete pipeline run "+child.getId());
		}
    	logger.info("    Deleted "+toReallyDelete.size()+" pipeline runs");
    	return toReallyDelete.size();
    }

    private long getNumNeuronsAnnotated(SamplePipelineRun pipelineRun) {

        int numAnnotations = 0;
        for(PipelineResult result : pipelineRun.getResults()) {
            for(NeuronSeparation separation : result.getResultsOfType(NeuronSeparation.class)) {
                // TODO: is this going to be too slow? we should be able to go directly to the annotations. 
                List<DomainObject> neurons = domainDao.getDomainObjects(ownerKey, separation.getFragmentsReference());
                List<Reference> references = DomainUtils.getReferences(neurons);
                numAnnotations += domainDao.getAnnotations(null, references).size();
            }
        }
        
        return numAnnotations;
    }
    
}
