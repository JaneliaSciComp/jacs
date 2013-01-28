
import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import java.text.SimpleDateFormat
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOSamplePipelineTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
subject = "group:leetlab"
f = new JacsUtils(subject, false)
e = f.e
c = f.c

id = 1805889121739079778L
sampleFolder = e.getEntityById(subject, id);
f.loadChildren(sampleFolder)

int numSamples = 0;
int numSamplesReprocessed = 0;

for (Entity sample : sampleFolder.children) {
	f.loadChildren(sample)
	sc = EntityUtils.getOrderedEntityDataForAttribute(sample,ATTRIBUTE_ENTITY)
	
    int numDefaultImages = 0;
    for(EntityData pipelineRunEd : sc) {
        if (pipelineRunEd.childEntity.getValueByAttributeName(ATTRIBUTE_DEFAULT_2D_IMAGE)!=null) {
            numDefaultImages++;
        }
    }
    
    boolean reprocess = false;
    if (sc.size()>=2) {
        // skip
        if (numDefaultImages<2) {
            reprocess = true;
        }
    }
    else if (sc.size()==1) {
        reprocess = true;
    }
    else {
        println "UNKNOWN sample: "+sample.name
        for(EntityData child : sc) {
            println "    "+child.childEntity.name
        }
        reprocess = true;
    }	
    
    if (reprocess) {
        println "Reprocess "+sample.name+" for "+sample.ownerKey
        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        taskParameters.add(new TaskParameter("sample entity id", sample.id.toString(), null));
        taskParameters.add(new TaskParameter("reuse processing", "false", null));
        Task task = new GenericTask(new HashSet<Node>(), "leetlab", new ArrayList<Event>(),
                taskParameters, "flylightSampleAllPipelines", "Flylight Sample All Pipelines");
        task.setJobName("Flylight Sample All Pipelines Task");
        task = c.saveOrUpdateTask(task);
        c.submitJob("GSPS_CompleteSamplePipeline", task.getObjectId());
        numSamplesReprocessed++;
    }
    numSamples++;
}

println "numSamples: "+numSamples
println "numSamplesReprocessed: "+numSamplesReprocessed


