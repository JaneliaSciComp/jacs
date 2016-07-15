package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

/**
 * Finds Sample entities without Supporting Data folders, unlinks them, and deletes them. 
 */

boolean DEBUG = true
subjectKey = "user:nerna"
f = new JacsUtils(subjectKey, false)

int totalSamplesProcessed = 0
int totalSamplesRemoved = 0

println "Processing samples for "+subjectKey

for(Entity sample : f.e.getUserEntitiesByTypeName(subjectKey, TYPE_SAMPLE)) {
	 
	f.loadChildren(sample)
	 
	Entity supportingData = EntityUtils.findChildWithName(sample, "Supporting Files")
	if (supportingData==null) {
		num = f.a.getNumDescendantsAnnotated(sample.id)
		println("  WARNING: could not find supporting files in sample#"+sample.id+" "+sample.name+" ("+num+" annotations)")
		
		if (!DEBUG) f.e.deleteEntityTreeById(subjectKey, sample.id, true)
		
		totalSamplesRemoved++
	}

	totalSamplesProcessed++
	sample.setEntityData(null)
}

println "Processed "+totalSamplesProcessed+" Samples"
println "Removed "+totalSamplesRemoved+" Samples"
