package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.workstation.api.entity_model.management.*
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.StringUtils
import org.janelia.it.jacs.shared.utils.entity.*

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap


owner = "user:nerna"
f = new JacsUtils(Constants.OWNER_KEY, true)

int allIsWell = 0
int noFastLoad = 0
int addedFastReference = 0
int addedFastSignal = 0

for(Entity separation : f.e.getUserEntitiesByTypeName(owner, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {

    f.loadChildren(separation)

    println "Processing "+separation.id

    Entity supportingData = EntityUtils.getSupportingData(separation)
    if (supportingData==null) {
        separation.setEntityData(null)
        continue
    }
    f.loadChildren(supportingData)

    Entity signal = null
    Entity reference = null

    if (supportingData!=null) {
        for(Entity image3d : EntityUtils.getChildrenOfType(supportingData, EntityConstants.TYPE_IMAGE_3D)) {
            if (image3d.getName().startsWith("Reference.")) {
                reference = image3d;
            }
            if (image3d.getName().startsWith("ConsolidatedSignal.")) {
                signal = image3d;
            }
        }
    }

    if (signal==null || reference==null) {
        println "  BAD: could not find both signal and reference stacks"
        separation.setEntityData(null)
        continue
    }

    f.loadChildren(signal);
    f.loadChildren(reference);

    if (signal.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)!=null && reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)!=null) {
        println "  All is well"
        allIsWell++
        separation.setEntityData(null)
        continue
    }

    Entity fastLoad = EntityUtils.findChildWithNameAndType(supportingData, "Fast Load", EntityConstants.TYPE_FOLDER)

    if (fastLoad==null) {
        println "  No fast load"
        noFastLoad++
        separation.setEntityData(null)
        continue
    }

    f.loadChildren(fastLoad);
    Entity fastReference = EntityUtils.findChildWithNameAndType(fastLoad, "Reference2_100.mp4", EntityConstants.TYPE_MOVIE)
    Entity fastSignal = EntityUtils.findChildWithNameAndType(fastLoad, "ConsolidatedSignal2_25.mp4", EntityConstants.TYPE_MOVIE)

    if (fastReference!=null && reference.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)==null) {
        println("Separation has fast load folder, but reference has no fast 3d image")
        f.addToParent(reference, fastReference, null, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)
        addedFastReference++
    }

    if (fastSignal!=null && signal.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)==null) {
        println("Separation has fast load folder, but signal has no fast 3d image")
        f.addToParent(signal, fastSignal, null, EntityConstants.ATTRIBUTE_DEFAULT_FAST_3D_IMAGE)
        addedFastSignal++
    }

    separation.setEntityData(null)
}

println "allIsWell="+allIsWell
println "noFastLoad="+noFastLoad
println "addedFastSignal="+addedFastSignal
println "addedFastReference="+addedFastReference