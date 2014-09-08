import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

/**
 * Finds Sample entities without Supporting Data folders, unlinks them, and deletes them. 
 */

boolean DEBUG = false
subjectKey = "user:nerna"
f = new JacsUtils(subjectKey, false)

int totalSamplesProcessed = 0
int totalSamplesRemoved = 0

println "Processing samples for "+subjectKey
f = new JacsUtils(subjectKey, false)

for(Entity sample : f.e.getUserEntitiesByTypeName(subjectKey, TYPE_SAMPLE)) {
	 
	f.loadChildren(sample)
	 
	Entity supportingData = EntityUtils.findChildWithName(sample, "Supporting Files")
	if (supportingData==null) {
		println("  WARNING: could not find supporting files in sample#"+sample.id)
		
		if (!DEBUG) f.e.deleteEntityTreeById(subjectKey, sample.id, true)
		
		totalSamplesRemoved++
	}

	totalSamplesProcessed++
}

println "Processed "+totalSamplesProcessed+" Samples"
println "Removed "+totalSamplesRemoved+" Samples"

