package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanMislabeledPipelineRunsScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "asoy_mb_polarity_case_4", "dicksonlab_trial_case_4", "nerna_polarity_case_4", "trautmane_test_polarity_case_4" ]
	private int numUpdated;
	
	public CleanMislabeledPipelineRunsScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			numUpdated = 0
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample")) {
					processSample(entity);
				}
				entity.setEntityData(null)	
			}
			println "  Updated "+numUpdated+" pipeline runs for "+dataSetIdentifier
		}
		println "Done"
	}
	
	public void processSample(Entity sample) {
		f.loadChildren(sample)
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				processSample(it)
			}
			return
		}
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
		for(Entity run : runs) {
			String pp = run.getValueByAttributeName("Pipeline Process")
			if ("YoshiMacroPolarityCase4".equals(pp) || "YoshiMBPolarityCase4".equals(pp)) {
				// This is good
			}
			else if ("YoshiMBPolarityCase3".equals(pp)) {
				//println "Updating "+sample.name+" run="+run.id
				numUpdated++;
				if (!DEBUG) {
					f.e.setOrUpdateValue(run.ownerKey, run.id, "Pipeline Process", "YoshiMBPolarityCase4")
				}
			}	
			else {
				println "  Unexpected pipeline process: "+pp
			}
		}
	}
}

CleanMislabeledPipelineRunsScript script = new CleanMislabeledPipelineRunsScript();
script.run();