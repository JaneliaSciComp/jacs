package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;

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
		
		if (child.entityTypeName == TYPE_NEURON_SEPARATOR_PIPELINE_RESULT) {
			separations.add(child)
		}
		else if (child.entityTypeName == TYPE_FOLDER) {
			println child.name
			walkEntity(child)
		}
		else if (child.entityTypeName == TYPE_SAMPLE) {
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
			e.deleteEntityTreeById(sep.user.userLogin, sep.id)
			println "    Deleted neuron Separation "+sep.id
			numSeparationsDeleted++
		}
	}
}

println "Total samples: "+numSamples
println "Separations in use: "+numSeparationsInUse
println "Separations deleted: "+numSeparationsDeleted


