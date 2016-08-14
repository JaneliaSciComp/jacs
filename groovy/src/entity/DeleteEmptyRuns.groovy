package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

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
        e.deleteEntityTreeById(username, run.id)
        numDeleted++;
    }
}

println "Total: "+numProcessed
println "Deleted: "+numDeleted


