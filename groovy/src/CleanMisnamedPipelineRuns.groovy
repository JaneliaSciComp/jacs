import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanMislabeledPipelineRunsScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "dicksonlab_vt_gal4_screen", "dicksonlab_vt_lexa_screen", "ditp_initial_splits", "knappj_ihc_optimizing" ]
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
			println "  Updated "+numUpdated+" JBA results for "+dataSetIdentifier
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
			f.loadChildren(run)
			for(Entity result : EntityUtils.getChildrenForAttribute(run, "Result")) {
				if (!result.entityTypeName.equals("Alignment Result")) continue
				
				f.loadChildren(result)
				Entity sd = EntityUtils.getSupportingData(result)
				if (sd==null) {
					println "Missing supporting data for "+result.id+" owned by "+result.ownerKey
					continue;
				}
				
				f.loadChildren(sd)
				Entity qiScoreCsv = EntityUtils.findChildWithName(sd, "QiScore.csv")
				
				if (qiScoreCsv==null) {
					println "Missing QiScore.csv for "+result.id+" owned by "+result.ownerKey
					continue;
				}
				
				if (!DEBUG) {
					result.setName("JBA Alignment")
					f.e.saveOrUpdateEntity(result.ownerKey, result)
				}
				numUpdated++;
			}
		}
	}
}

CleanMislabeledPipelineRunsScript script = new CleanMislabeledPipelineRunsScript();
script.run();