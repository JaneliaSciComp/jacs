import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask
import org.janelia.it.jacs.model.user_data.Node
import org.janelia.it.jacs.shared.utils.EntityUtils

class SampleCleaningScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String[] dataSetIdentifiers = [ "asoy_mb_polarity_case_1" , "asoy_mb_polarity_case_2", "asoy_mb_polarity_case_3", "asoy_mb_polarity_case_4", "asoy_mb_split_mcfo_case_1"  ]
	private int numSamples = 0
	private int numRunsDeleted = 0
	
	public SampleCleaningScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample") && !entity.name.endsWith("-Retired")) {
					processSample(entity, entity)
				}
				entity.setEntityData(null)	
			}
		}
        println "Considered "+numSamples+" samples. Deleted "+numRunsDeleted+" results."
		println "Done"
	}
	
	void processSample(Entity parentSample, Entity sample) {
		String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)
		f.loadChildren(sample)
		
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				f.loadChildren(it)
				processSample(it)
			}
		}
		else {
			processSample(sample)
		}
	}
	
	void processSample(Entity sample) throws Exception {
		
		println "Cleaning up sample "+sample.getName()
		numSamples++
		
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);
		if (runs.isEmpty()) return;
		
		// Group by pipeline process
		Map<String,List<Entity>> processRunMap = new HashMap<String,List<Entity>>();

		for(Entity pipelineRun : runs) {
			String process = pipelineRun.getValueByAttributeName(EntityConstants.ATTRIBUTE_PIPELINE_PROCESS);
			if (process == null) process = "";
			List<Entity> areaRuns = processRunMap.get(process);
			if (areaRuns == null) {
				areaRuns = new ArrayList<Entity>();
				processRunMap.put(process,areaRuns);
			}
			areaRuns.add(pipelineRun);
		}
		
		for(String process : processRunMap.keySet()) {
			List<Entity> processRuns = processRunMap.get(process);
			if (processRuns.isEmpty()) continue;
			
			println "  Processing pipeline runs for process: "+process
			
			// Remove latest run, we don't want to touch it
			Entity lastRun = processRuns.remove(processRuns.size()-1);
			f.loadChildren(lastRun);

			if (EntityUtils.getLatestChildOfType(lastRun, EntityConstants.TYPE_ERROR)!=null) {
				println "    Keeping last error run: "+lastRun.getId()
				// Last run had an error, let's keep that, but still try to find a good run to keep
				Collections.reverse(processRuns);
				Integer keeper = null;
				int curr = 0;
				for(Entity pipelineRun : processRuns) {
					f.loadChildren(pipelineRun);
					if (EntityUtils.getLatestChildOfType(pipelineRun, EntityConstants.TYPE_ERROR)==null) {
						keeper = curr;
						break;
					}
					curr++;
				}
				if (keeper!=null) {
					Entity lastGoodRun = processRuns.remove(keeper.intValue());
					println "    Keeping last good run: "+lastGoodRun.getId()
					
				}
				else {
					println "    Could not find a good run to keep."
				}
			}
			else {
				println "    Keeping last good run: "+lastRun.getId()
			}
			
			// Clean up everything else
			deleteUnannotated(processRuns);
		}
	}
	
	void deleteUnannotated(List<Entity> toDelete) throws ComputeException {
		
		Set<Entity> toReallyDelete = new HashSet<Entity>();
		for(Entity entity : toDelete) {
			long numFound = f.a.getNumDescendantsAnnotated(entity.getId());
			if (numFound>0) {
				println "    Rejecting candidate "+entity.getId()+" because it and its descendants have "+numFound+" annotations"
				continue;
			}
			toReallyDelete.add(entity);
		}

		if (toReallyDelete.isEmpty()) return;
		println "    Found "+toReallyDelete.size()+" non-annotated results for deletion:"
	
		int c = 0;
		for(Entity child : toReallyDelete) {
			
			if (!DEBUG) {
				f.e.deleteEntityTreeById(child.getOwnerKey(), child.getId());
			}
			else {
				println "      Deleting tree "+child.getId()
			}
			c++;
			numRunsDeleted++;
		}
	}
	
	
}

SampleCleaningScript script = new SampleCleaningScript();
script.run();
System.exit(0);