import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask
import org.janelia.it.jacs.model.user_data.Node
import org.janelia.it.jacs.shared.utils.EntityUtils

class ResubmitSamplesWithMacroErrorsScript {
	
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String context;
	private String[] dataSetIdentifiers = [ "asoy_mb_polarity_case_1", "asoy_mb_polarity_case_2", "asoy_mb_polarity_case_3", "asoy_mb_polarity_case_4", "asoy_mb_split_mcfo_case_1"  ]
	private int numResubmitted = 0
	private int totalNumResubmitted = 0
	
	public ResubmitSamplesWithMacroErrorsScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
				
		for(String dataSetIdentifier : dataSetIdentifiers) {
			numResubmitted = 0
			println "Processing "+dataSetIdentifier
			for(Entity entity : f.e.getEntitiesWithAttributeValue(null, EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER, dataSetIdentifier)) {
				if (entity.entityTypeName.equals("Sample") && !entity.name.endsWith("-Retired")) {
					if (processSample(entity, entity)) {
						resubmitSample(entity)
					}
				}
				entity.setEntityData(null)	
			}
			totalNumResubmitted += numResubmitted
			println "Resubmitted "+numResubmitted+" samples for "+dataSetIdentifier
		}
		println "Resubmitted "+totalNumResubmitted+" samples "
		println "Done"
	}
	
	public boolean processSample(Entity parentSample, Entity sample) {
		boolean problem = false
		String objective = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE)
		
		f.loadChildren(sample)
		List<Entity> childSamples = EntityUtils.getChildrenOfType(sample, "Sample")
		if (!childSamples.isEmpty()) {
			childSamples.each {
				if (processSample(sample, it)) {
					problem = true
				}
			}
			return problem
		}
		boolean found = false
		List<Entity> runs = EntityUtils.getChildrenOfType(sample, "Pipeline Run")
		for(Entity run : runs) {
			String pp = run.getValueByAttributeName("Pipeline Process")
			if (pp.startsWith("YoshiMacro")) {
				f.loadChildren(run)
				List<Entity> errors = EntityUtils.getChildrenOfType(run, "Error")
				if (!errors.isEmpty()) {
					//println "  Macro error for sample: "+parentSample.name;
					problem = true
				}
				found = true
			}
		}
		
		if (!found && "20x".equals(objective)) {
			//println "  No 20x macro results found for sample: "+parentSample.name;
			problem = true
		}
		
		return problem
	}
	
	public void resubmitSample(Entity sample) {
		
		String ds = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
		String process = "";
		if ("asoy_mb_polarity_case_1".equals(ds)) {
			process = "PipelineConfig_YoshiMacroPolarityCase1";
		}
		else if ("asoy_mb_polarity_case_2".equals(ds)) {
			process = "PipelineConfig_YoshiMacroPolarityCase3";
		}
		else if ("asoy_mb_polarity_case_3".equals(ds)) {
			process = "PipelineConfig_YoshiMacroPolarityCase3";
		}
		else if ("asoy_mb_polarity_case_4".equals(ds)) {
			process = "PipelineConfig_YoshiMacroPolarityCase4";
		}
		else if ("asoy_mb_split_mcfo_case_1".equals(ds)) {
			process = "PipelineConfig_YoshiMacroMCFOCase1";
		}
		
		println "Submitting "+sample.name+" ("+ds+") for reprocessing with "+process
		numResubmitted++
		
		if (!DEBUG) {
			String displayName = "Apply Process To Sample";
			HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
			taskParameters.add(new TaskParameter("sample entity id", ""+sample.id, null));
			String user = sample.getOwnerKey();
			GenericTask task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), taskParameters, process, displayName);
			task = f.c.saveOrUpdateTask(task);
			f.c.submitJob(task.getTaskName(), task.getObjectId());
		}
	}
}

ResubmitSamplesWithMacroErrorsScript script = new ResubmitSamplesWithMacroErrorsScript();
script.run();