package entity

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
    private int numErrorRunsDeleted;
    private int numEmptyRunsDeleted;
    private int numCorrectedStatusEds;
    
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
                //if (!subjectKey.equals("group:leetlab")) continue
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
        println "Completed processing for "+ownerKey
        println "  Corrected status on "+numCorrectedStatus+" sub-samples"
        println "  Deleted "+numEmptyRunsDeleted+" empty pipeline runs"
        println "  Deleted "+numErrorRunsDeleted+" error pipeline runs"
        println "  Deleted "+numCorrectedStatusEds+" duplicate statuses"
        println "  Marked "+numMarkedSamples+" samples for rerun"
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
        
        List<EntityData> statusEds = EntityUtils.getOrderedEntityDataForAttribute(sample, EntityConstants.ATTRIBUTE_STATUS);
        if (statusEds.size()>1) {
            int i = 0
            for(EntityData ed : statusEds) {
                if (i>0) {
                    sample.getEntityData().remove(ed)
                    if (!DEBUG) {
                        f.e.deleteEntityData(ed.ownerKey, ed.getId())
                    }
                    numCorrectedStatusEds++;
                }
                i++
            }
        }
        String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
        
        if ("Complete".equals(status)) {
            for(Entity childSample : childSamples) {
                if ("Processing".equals(childSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS))) {
                    if (!DEBUG) {
                        f.e.setOrUpdateValue(childSample.ownerKey, childSample.id, EntityConstants.ATTRIBUTE_STATUS, "Complete")
                    }
                    numCorrectedStatus++;
                }
            }            
        }
        else if ("Error".equals(status)) {
            for(Entity childSample : childSamples) {
                if (allRunsError(childSample, 3)) {
                    for(Entity run : EntityUtils.getChildrenOfType(sample, "Pipeline Run")) {
                        if (!DEBUG) {
                            f.e.deleteEntityTreeById(run.ownerKey, run.id)
                        }
                        numErrorRunsDeleted++;
                    }
                    reprocess = true;
                    break;
                }
            }
        }
        
        if (reprocess) {
            if (!EntityConstants.VALUE_DESYNC.equals(status) 
                    && !EntityConstants.VALUE_RETIRED.equals(status) 
                    && !EntityConstants.VALUE_BLOCKED.equals(status)
                    && !EntityConstants.VALUE_MARKED.equals(status)
                    && !sample.name.startsWith("NO_CONSENSUS")) {
                println "  Marking sample for reprocessing: "+sample.name
                if (!DEBUG) {
                    f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED)
                }
                numMarkedSamples++;
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
                numEmptyRunsDeleted++;
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
            numErrors += EntityUtils.getChildrenOfType(run, "Error").isEmpty()?0:1;
            numRuns++;
        }
        if (numErrors==numRuns && numErrors>=minErrors) {
            //println "  Sample has "+numErrors+" errors, it will be cleaned and marked for rerun: "+sample.name
            return true
        }
        else if (numErrors==numRuns && numErrors>=2) {
            println "  WARNING: All runs error, and there are "+numErrors+" which is less than the cut-off "+minErrors+": "+sample.name
        }
        else if (numErrors>=minErrors) {
            println "  WARNING: There are "+numErrors+" errors, but "+numRuns+" total runs. This sample should be cleaned up by the cleaning service "+sample.name 
        }
        else {
            //println "  INFO: There are "+numErrors+" errors, and "+numRuns+" total runs."
        }
        return false
    }
}

CleanSamplesScript script = new CleanSamplesScript();
script.run();