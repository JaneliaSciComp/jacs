
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
username = "asoy"
f = new JacsUtils(username, false)
e = f.e
c = f.c
s = f.s

numProcessed = 0
numDeleted = 0

for(Entity run : e.getUserEntitiesByTypeName(username, TYPE_PIPELINE_RUN)) {
    f.loadChildren(run)
    numProcessed++;
    if (run.children.isEmpty()) {
        e.deleteEntityTree(username, run.id)
        numDeleted++;
    }
}

println "Total: "+numProcessed
println "Deleted: "+numDeleted


