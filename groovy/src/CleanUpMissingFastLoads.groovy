
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
username = "system"
f = new JacsUtils(username, false)
e = f.e
c = f.c

int numTotal = 0
int numDeleted = 0

for(Entity fastLoad : e.getUserEntitiesByName(null, "Fast Load")) {
	
	println "Processing "+fastLoad.id
	f.loadChildren(fastLoad)
	
	numTotal++;
	if (fastLoad.children.size()!=28) {
		println separation.id+" "+fastLoad.children.size()
		f.e.deleteSmallEntityTree(username, fastLoad.id)
		numDeleted++;
	}

	fastLoad.entityData = null
}

println "Total: "+numTotal
println "Deleted: "+numDeleted

