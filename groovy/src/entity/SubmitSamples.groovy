package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.Task
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask

import static org.janelia.it.jacs.model.entity.EntityConstants.*

// Globals
subject = "user:asoy"
f = new JacsUtils(subject, false)
e = f.e
c = f.c

int numSamples = 0
int numSamplesReprocessed = 0

for (Entity sample : e.getUserEntitiesByTypeName(subject, TYPE_SAMPLE)) {

	def dataSet = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
    def default3d = sample.getValueByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE)

    boolean reprocess = false;
    if (dataSet?.contains("asoy_mb_polarity_63x_case_3") && !default3d?.contains("Aligned")) {
        reprocess = true;
    }

    if (reprocess) {
        println "Reprocess "+sample.name+" with dataSet="+dataSet+" and default3d="+default3d
        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        taskParameters.add(new TaskParameter("sample entity id", sample.id.toString(), null));
        taskParameters.add(new TaskParameter("reuse processing", "true", null));
        Task task = new GenericTask(new HashSet<Node>(), subject, new ArrayList<Event>(),
                taskParameters, "sampleAllPipelines", "Sample All Pipelines");
        task = c.saveOrUpdateTask(task);
        c.submitJob("GSPS_CompleteSamplePipeline", task.getObjectId());
        numSamplesReprocessed++;
    }
    numSamples++;
}

println "Num samples processed: "+numSamples
println "Num samples reprocessed: "+numSamplesReprocessed


