import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanCertainPipelineRunsScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private int numDeleted;
	private int numAvoided;
	
	public CleanCertainPipelineRunsScript() {
		f = new JacsUtils(null, !DEBUG)
	}
	
	public void run() {
				
		Entity folder = f.e.getEntityById(null, 2135471309109330065L)
		f.loadChildren(folder)

		for(Entity sample : EntityUtils.getChildrenOfType(folder, "Sample")) {
			processSample(sample)
		}
		
		println "Deleted "+numDeleted+" trees"
		println "Avoided "+numAvoided+" samples"
	}
	
	private void processSample(Entity sample) {
		f.loadChildren(sample)
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				processSample(it)
			}
			return
		}
		
		println "Processing "+sample.name
		List<Entity> pipelineRuns = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
		
		
		boolean hasGoodRun = false;
		for(Entity pipelineRun : pipelineRuns) {
			if (pipelineRun.name.startsWith("Paired Sample Pipeline")) {
				hasGoodRun = true;
			}
		}
		
		if (hasGoodRun) { 
			for(Entity pipelineRun : pipelineRuns) {
				if (shouldDelete(pipelineRun)) {
					println "  Deleting "+pipelineRun.name
					if (!DEBUG) {
						f.e.deleteEntityTreeById(pipelineRun.ownerKey, pipelineRun.id, true)
					}
					numDeleted++
				}
			}
		}
		else {
			numAvoided++
			println "  has no desired run, cannot delete other runs, avoiding sample entirely."
		}
	}
	
	private boolean shouldDelete(Entity pipelineRun) {
		return (pipelineRun.name.startsWith("Polarity Pipeline") || pipelineRun.name.startsWith("Projection Pipelin"));
	}
}

CleanCertainPipelineRunsScript script = new CleanCertainPipelineRunsScript();
script.run();
println "Done"