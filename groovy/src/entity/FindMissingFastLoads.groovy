package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*


import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils

// Globals
username = "system"
f = new JacsUtils(username, false)
e = f.e
c = f.c

int numTotal = 0
int numMissing = 0

for(Entity separation : e.getUserEntitiesByTypeName(null, TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
	
    userKey = separation.ownerKey
	println "Processing "+separation.id+" ("+userKey+")"
	f.loadChildren(separation)
	
    for(Entity child : separation.children) {
        println "    "+child.name
    }
    
    Entity supportingData = EntityUtils.findChildWithName(separation, "Supporting Files")
    if (supportingData==null) {
        println "ERROR: missing supporting data"
        continue;
    }
    
    f.loadChildren(supportingData)
    Entity fastLoad = EntityUtils.findChildWithName(supportingData, "Fast Load")
    
    if (fastLoad == null) {
        println "    Missing fast load"
        numMissing++
        missingCountByUser[userKey]++
    }
    
    numTotal++
    missingCountByUser[userKey]++
    
    // Free memory
	separation.entityData = null
}

println "SubjectKey\tTotal\tMissing"
totalCountByUser.keySet().each {
    total = totalCountByUser[it]
    missing = missingCountByUser[it]
    println it+"\t"+total+"\t"+missing
}


println ""
println "Total separations: "+numTotal
println "Missing fast load: "+numMissing

