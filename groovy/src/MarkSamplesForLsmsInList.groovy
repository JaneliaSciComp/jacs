
import static org.janelia.it.jacs.model.entity.EntityConstants.*
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

import com.google.common.base.Charsets
import com.google.common.io.Files

class MarkSamplesForLsmsInListScript {
    
    private static final filename = "all_changed_lsms.txt";
    private static final username = "user:rokickik"
    private static final boolean DEBUG = true;
    
    private final JacsUtils f;
    
    private Set<Long> markedSampleIds = new HashSet<Long>();
    
    public MarkSamplesForLsmsInListScript() {
        f = new JacsUtils(username, !DEBUG)
    }
    
    public void run() {
        
        println "Processing "+filename
        
        List<String> lines = Files.readLines(new File(filename), Charsets.UTF_8);
        lines.each {

            String[] cols = it.split("\t")
            
            if (cols.length==2) {
                String sageId = cols[0]
                String lsmName = cols[1]
                if (!"id".equals(sageId)) {
                    processLsm(sageId)
                }
            }
        }
        
        println "Completed processing"
        println "  Marked "+markedSampleIds.size()+" samples for rerun"
    }
    
    private void processLsm(String sageId) {
        
        Entity sample = null
        
        List<Entity> lsms = f.e.getEntitiesWithAttributeValue(null, ATTRIBUTE_SAGE_ID, sageId)
        if (lsms.isEmpty()) {
            println "ERROR: could not find LSM with SAGE Id "+sageId
            return;
        }
        if (lsms.size()>1) println "WARNING: More than one LSM with SAGE Id "+sageId
        Entity lsm = lsms.get(0)
        sample = f.e.getAncestorWithType(null, lsm.id, TYPE_SAMPLE)    
        if (sample.name.contains("~")) {
            sample = f.e.getAncestorWithType(null, sample.id, TYPE_SAMPLE)
        }
        
        processSample(sample)
    }
    
    private void processSample(Entity sample) {
        
        if (markedSampleIds.contains(sample.id)) {
            println "Already marked sample: "+sample.id
            return;
        }
        
        String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
        
        if (!EntityConstants.VALUE_DESYNC.equals(status)
                && !EntityConstants.VALUE_RETIRED.equals(status)
                && !EntityConstants.VALUE_BLOCKED.equals(status)
                && !EntityConstants.VALUE_MARKED.equals(status)
                && !sample.name.startsWith("NO_CONSENSUS")) {
            println "Marking sample for reprocessing: "+sample.name+" (status: "+status+", owner: "+sample.ownerKey+")"
            if (!DEBUG) {
                f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_MARKED)
            }
            markedSampleIds.add(sample.id)
        }
    }
}

MarkSamplesForLsmsInListScript script = new MarkSamplesForLsmsInListScript();
script.run();
System.exit(0);
