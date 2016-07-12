package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

class CleanSampleStatusScript {
	
	private static final boolean DEBUG = false;
    private String ownerKey = null;
    private final JacsUtils f;
	private String context;
	private int numCorrectedStatus;
    
	public CleanSampleStatusScript() {
		f = new JacsUtils(ownerKey, !DEBUG)
	}
	
	public void run() {
        if (ownerKey==null) {
            Set<String> subjectKeys = new TreeSet<String>();
            for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
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
        println "Processing samples for "+ownerKey
        for(Entity sample : f.e.getUserEntitiesByTypeName(ownerKey, "Sample")) {
            if (sample.name.contains("~")) continue;
            processSample(sample)
            sample.setEntityData(null)
        }
        println "Completed processing for "+ownerKey
        println "  Corrected status on "+numCorrectedStatus+" samples"
    }

	
	private void processSample(Entity sample) {
		if (sample.name.endsWith("-Retired")) {
			String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
			if (!status.equals(EntityConstants.VALUE_RETIRED)) {
				if (!DEBUG) {
					f.e.setOrUpdateValue(sample.ownerKey, sample.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_RETIRED)
				}
				numCorrectedStatus++;
			}
		}
	}
    
}

CleanSampleStatusScript script = new CleanSampleStatusScript();
script.run();