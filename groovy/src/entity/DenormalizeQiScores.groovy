package entity

import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

class Globals {
	static int BATCH_SIZE = 1000
	static boolean DEBUG = false
}

final JacsUtils f = new JacsUtils(null, false)

Set<String> subjectKeys = new HashSet<String>();
for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
    subjectKeys.add(dataSet.getOwnerKey());
}

println "Found users with data sets: "+subjectKeys
for(String subjectKey : subjectKeys) {
    println "Processing "+subjectKey;

 	Map<Long,String> scoreBatch = new HashMap<Long,String>();

    for(Entity result : f.e.getUserEntitiesByNameAndTypeName(subjectKey, "JBA Alignment", EntityConstants.TYPE_ALIGNMENT_RESULT)) {
        f.loadChildren(result)
        Entity image = result.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE)
		String score = image.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE)
		if (score!=null) {
			scoreBatch.put(result.id, score)
            if (scoreBatch.size()>Globals.BATCH_SIZE) {
                processBatch(f, scoreBatch)
                scoreBatch.clear()
            }
        }
        result.setEntityData(null);
    }

    if (!scoreBatch.isEmpty()) {
        processBatch(f, scoreBatch)
    }
}

println "Done"


def processBatch(JacsUtils f, Map<Long,String> scoreBatch) {

	List<String> upMapping = new ArrayList<String>();
	upMapping.add(EntityConstants.TYPE_PIPELINE_RUN);
	upMapping.add(EntityConstants.TYPE_SAMPLE);
	List<String> downMapping = new ArrayList<String>();
	downMapping.add(EntityConstants.TYPE_SUPPORTING_DATA);
	downMapping.add(EntityConstants.TYPE_IMAGE_TILE);
	downMapping.add(EntityConstants.TYPE_LSM_STACK);
	
	List<Long> entityIds = new ArrayList<Long>();
	entityIds.addAll(scoreBatch.keySet());

	for(MappedId mappedId : f.e.getProjectedResults(null, entityIds, upMapping, downMapping)) {
		
		Entity lsm = f.e.getEntityById(null, mappedId.getMappedId())
        String area = lsm.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)

		if ("Brain".equals(area)) {
			String score = scoreBatch.get(mappedId.getOriginalId())
			if (Globals.DEBUG) {
				println "Setting Qi score for LSM "+lsm.id+" to "+score
			}
			else {
				f.e.setOrUpdateValue(lsm.ownerKey, lsm.id, EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, score)
			}
		}
		
	}
}

