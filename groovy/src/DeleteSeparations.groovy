
import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import java.text.SimpleDateFormat
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.FlyWorkstation.gui.util.PathTranslator;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.support.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.MCFOSamplePipelineTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
username = "leetlab"
f = new JacsUtils(username, false)
e = f.e
c = f.c
s = f.s

numSamples = 0
numSeparationsDeleted = 0
numSeparationsInUse = 0


id = 1759767174055985250
//id = 1775492670731321442
sampleFolder = e.getUserEntityById(username, id);
if (sampleFolder==null) {
	println "Folder does not exist with id "+id
	return
}
walkEntity(sampleFolder)


def walkEntity(Entity entity) {
	
	separations = []
	
	f.loadChildren(entity)
	for (Entity child : entity.children) {
		
		if (child.entityType.name == TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) {
			separations.add(child)
		}
		else if (child.entityType.name == TYPE_FOLDER) {
			println child.name
			walkEntity(child)
		}
		else if (child.entityType.name == TYPE_SAMPLE) {
			numSamples++
			walkEntity(child)
		}
		
	}

	if (!separations.isEmpty()) {
		println "  "+entity.name
		process(entity, separations)
	}
}

def process(Entity sample, List<Entity> separations) {
		
	if (separations.size()>1) {
		println "    MORE THAN ONE SEPARATION!"
	}
	
	EntityData defaultEd = sample.getEntityDataByAttributeName(ATTRIBUTE_DEFAULT_2D_IMAGE)
	if (defaultEd.value.contains("Consolidated")) {
		println "    Neuron Separation is in use. Resubmitting sample..."
		numSeparationsInUse++
			
//		Task task = new MCFOSamplePipelineTask(new HashSet<Node>(),
//				sample.user.userLogin, new ArrayList<Event>(), new HashSet<TaskParameter>(),
//				sample.id+"", true, true, null);
//		task.setJobName("Leet Sample Pipeline Task");
//		task = c.saveOrUpdateTask(task);
//		c.submitJob("LeetSamplePipeline", task.getObjectId());
	}
	else {
		
		for(Entity sep : separations) {
			e.deleteEntityTree(sep.user.userLogin, sep.id)
			println "    Deleted neuron Separation "+sep.id
			numSeparationsDeleted++
		}
	}
}

println "Total samples: "+numSamples
println "Separations in use: "+numSeparationsInUse
println "Separations deleted: "+numSeparationsDeleted


