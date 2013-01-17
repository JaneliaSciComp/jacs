
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

int numDeleted = 0

GregorianCalendar cutoff = new GregorianCalendar(2012, Calendar.AUGUST, 31, 12, 0, 0, 0)

sps = e.getUserEntitiesByTypeName(username, TYPE_SAMPLE_PROCESSING_RESULT)

for(Entity sp : sps) {
	
	GregorianCalendar date = new GregorianCalendar()
	date.setTime(sp.creationDate)
	if (cutoff.after(date)) continue;
	
	f.loadChildren(sp)
	Entity sd = EntityUtils.getSupportingData(sp)
	f.loadChildren(sd)
	
	foundStitched = false
	stitchedMips = []
	
	for(Entity file : sd.children) {
		if (file.name.startsWith("stitched-")) {
			if (file.entityType.name == TYPE_IMAGE_3D) {
				foundStitched = true
			}
			else if (file.entityType.name == TYPE_IMAGE_2D) {
				stitchedMips.add(file)
			}
		}
	}

	if (!foundStitched && !stitchedMips.isEmpty()) {
		println sp.id+" has orphan MIPs: "
		for(Entity mip : stitchedMips) {
			println "    "+mip.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
			e.deleteEntityTree(mip.user.userLogin, mip.id)
			numDeleted++
		}
	}
		
}



println "Total: "+sps.size()
println "Deleted: "+numDeleted

