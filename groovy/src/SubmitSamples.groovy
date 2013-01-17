
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
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
f = new JacsUtils("leetlab", false)
e = f.e
c = f.c

id = "1759767174823542882";
sampleFolder = e.getEntityById(id);

f.loadChildren(sampleFolder)

numRefreshProcessing = 0
numRefreshAlignment = 0

GregorianCalendar cutoff = new GregorianCalendar(2012, Calendar.AUGUST, 31, 12, 0, 0, 0)


for (Entity sample : sampleFolder.children) {
	f.loadChildren(sample)
	sc = sample.getOrderedChildren()
	
	latestResults = []
	for(Entity child : sc) {
		if (child.entityType.name == TYPE_IMAGE_2D) continue
		GregorianCalendar date = new GregorianCalendar()
		date.setTime(child.creationDate)
		if (cutoff.before(date)) {
			
			latestResults.add(child)
		}
	}
	
	refreshProcessing = false
	refreshAlignment = false
	
	if (latestResults.size() == 0) {
		refreshProcessing = true
		refreshAlignment = true
	}
	else if (latestResults.size() == 1) {
		refreshAlignment = true
	}
	else if (latestResults.size() == 2) {
		// all good
	}
	else {
		throw new IllegalStateException("Unknown situation: "+sample.name)
	}
	
	if (refreshProcessing || refreshAlignment) {
	
		println sample.name
		
		if (refreshProcessing) {
			println "    + Refresh processing and alignment"
			numRefreshProcessing++;
			numRefreshAlignment++;
			
		}
		else if (refreshAlignment) {
			println "    + Refresh alignment"
			numRefreshAlignment++;
		}
			
		Task task = new MCFOSamplePipelineTask(new HashSet<Node>(),
				sample.getUser().getUserLogin(), new ArrayList<Event>(), new HashSet<TaskParameter>(),
				sample.id+"", refreshProcessing, refreshAlignment, null);
		task.setJobName("Leet Sample Pipeline Task");
		task = c.saveOrUpdateTask(task);
		c.submitJob("LeetSamplePipeline", task.getObjectId());
	}
}

println "Total: "+sampleFolder.children.size()
println "Refresh processing: "+numRefreshProcessing
println "Refresh alignment: "+numRefreshAlignment

