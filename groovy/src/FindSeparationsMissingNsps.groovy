import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBEntityLoader
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityType
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.StringUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT
import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_SAMPLE

boolean DEBUG = false

final JacsUtils f = new JacsUtils(null, false)

Set<String> subjectKeys = new HashSet<String>();
//for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
//    subjectKeys.add(dataSet.getOwnerKey());
//}
//subjectKeys.add("user:asoy")
//subjectKeys.add("group:flylight")
//subjectKeys.add("user:korffw")
//subjectKeys.add("group:leetlab")
subjectKeys.add("user:nerna")
//subjectKeys.add("user:nerna")
//subjectKeys.add("user:taes")
//subjectKeys.add("group:heberleinlab")

EntityType myersType = f.e.getEntityTypeByName(EntityConstants.TYPE_MYERS_NEURON_SEPARATION_FILE)

println "Found users with data sets: "+subjectKeys
for(String subjectKey : subjectKeys) {
    println "Processing "+subjectKey;
    int totalSubjectAnnots = 0

    for(Entity separation : f.e.getUserEntitiesByTypeName(subjectKey, TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {

        f.loadChildren(separation)
        Entity supportingFiles = EntityUtils.getSupportingData(separation)
        if (supportingFiles==null) continue;
        f.loadChildren(supportingFiles)
        
        if (EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_MYERS_NEURON_SEPARATION_FILE).isEmpty()) {

            boolean found = false;
            for(Entity image : EntityUtils.getChildrenOfType(supportingFiles, EntityConstants.TYPE_IMAGE_3D)) {
                if (image.name.endsWith(".nsp")) {
                    found = true;
                    if (!DEBUG) {
                        image.setEntityType(myersType)
                        f.save(image)
                    }
                }
            }

            if (!found) {
                println separation.id+" missing NSP"
//                int numAnnots = f.a.getNumDescendantsAnnotated(separation.id)
//                println separation.id+" missing NSP has "+numAnnots+" annotations"
//                totalSubjectAnnots+=numAnnots
            }
            else {
                println separation.id+" OK (needed to change type)"
            }
        }
        else {
            println separation.id+" OK"
        }
        
        separation.setEntityData(null)
    }
    
    println "Done "+subjectKey+", would orphan "+totalSubjectAnnots+" annotations"
}

