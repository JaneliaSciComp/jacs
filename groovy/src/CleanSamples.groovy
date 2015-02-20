import javax.ejb.EntityContext;

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanSamplesScript {
	
	private static final boolean DEBUG = false;
    private String ownerKey = null;
    private final JacsUtils f;
	private String context;
	private int numCorrectedStatus;
    private int numMarkedSamples;
    
	public CleanSamplesScript() {
		f = new JacsUtils(ownerKey, false)
	}
	
	public void run() {
        if (ownerKey==null) {
            Set<String> subjectKeys = new TreeSet<String>();
            for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
                if (subjectKey.equals("group:dicksonlab")||subjectKey.equals("group:ditp")||subjectKey.equals("group:flylight")||subjectKey.equals("group:heberleinlab")) continue
                processSamples(subjectKey);
            }
        }
        else {
            processSamples(ownerKey);
        }
        println "Done"
        System.exit(0)
	}
    
    private void processSamples(String ownerKey) {
        this.numCorrectedStatus = 0
        this.numMarkedSamples = 0
        println "Processing samples for "+ownerKey
        for(Entity sample : f.e.getUserEntitiesByTypeName(ownerKey, "Sample")) {
            if (sample.name.contains("~")) continue;
            processSample(sample)
            sample.setEntityData(null)
        }
        println "Corrected status on "+numCorrectedStatus+" sub-samples for "+ownerKey
        println "Marked "+numMarkedSamples+" samples for rerun for "+ownerKey
    }
	
	private void processSample(Entity sample) {
		f.loadChildren(sample)
        List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
        
        boolean reprocess = false;
        
        if (childSamples.isEmpty()) {
            if (cleanPipelineRuns(sample)) {
                reprocess = true
            }
        }
        else {
            for(Entity childSample : childSamples) {
                f.loadChildren(childSample)
                if (cleanPipelineRuns(childSample)) {
                    reprocess = true
                }
            }
        }
        
        String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS);
        
        if ("Complete".equals(status)) {
            for(Entity childSample : childSamples) {
                if ("Processing".equals(childSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
                    //println "Correcting status for sample "+childSample.name
                    numCorrectedStatus++;
                    if (!DEBUG) {
                        f.e.setOrUpdateValue(childSample.ownerKey, childSample.id, EntityConstants.ATTRIBUTE_STATUS, "Complete")
                    }
                }
            }            
        }
        else if ("Error".equals(status)) {
            for(Entity childSample : childSamples) {
                if (allRunsError(childSample, 5)) {
                    reprocess = true;
                    break;
                }
            }
        }
        
        if (reprocess) {
            if (!EntityConstants.VALUE_DESYNC.equals(status) 
                    && !EntityConstants.VALUE_RETIRED.equals(status) 
                    && !EntityConstants.VALUE_BLOCKED.equals(status) 
                    && !EntityConstants.VALUE_ERROR.equals(status) 
                    && !EntityConstants.VALUE_MARKED.equals(status)) {
                numMarkedSamples++;
                println "  Marking sample for reprocessing: "+sample.name
                if (!DEBUG) {
                    f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED)
                }
            }
        }
        
	}
    
    private boolean cleanPipelineRuns(Entity sample) {
        boolean hasRemainingRuns = false;
        List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
        for(Entity run : runs) {
            f.loadChildren(run)
            
            boolean hasError = false;
            boolean hasResult = false;
            for(Entity child : run.getChildren()) {
                if (child.entityTypeName.equals("Error")) {
                    hasError = true
                }
                else {
                    hasResult = true
                }
            }
                        
            if (!hasError && !hasResult) {
                println "  Removing empty run (id="+run.id+") for sample: "+sample.name
                if (!DEBUG) {
                    f.e.deleteEntityTreeById(run.ownerKey, run.id)
                }
            }
            else {
                hasRemainingRuns = true
            }
        }
        return !hasRemainingRuns
    }
    
    private boolean allRunsError(Entity sample, int minErrors) {
        int numRuns = 0;
        int numErrors = 0;
        List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
        for(Entity run : runs) {
            f.loadChildren(run)
            numErrors += EntityUtils.getChildrenOfType(sample, "Error").isEmpty()?0:1;
            numRuns++;
        }
        if (numErrors==numRuns && numErrors>=minErrors) {
            println "  Sample has "+numErrors+": "+sample.name
            return true
        }
        else if (numErrors==numRuns && numErrors>=2) {
            println "  WARNING: All runs error, and there are "+numErrors+" which is less than the cut-off "+minErrors
        }
        else if (numErrors>=minErrors) {
            println "  WARNING: There are "+numErrors+" errors, but "+numRuns+" total runs. This should be cleaned up by the cleaning service."
        }
        return false
    }
}

CleanSamplesScript script = new CleanSamplesScript();
script.run();