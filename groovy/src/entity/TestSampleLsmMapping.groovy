package entity

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.janelia.it.jacs.compute.api.support.MappedId
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

import static org.janelia.it.jacs.model.entity.EntityConstants.TYPE_LSM_STACK

boolean DEBUG = false

String ownerKey = "user:nerna";
f = new JacsUtils(ownerKey, false);
e = f.e

Multimap<String,Long> slideCodeToSampleIdMap = HashMultimap.<String,Long>create();
Multimap<String,String> slideCodeToSampleNameMap = HashMultimap.<String,String>create();
Multimap<Long,String> sampleIdToAnnotations = HashMultimap.<Long,String>create();
Multimap<String,String> slideCodeToSampleDeletionCandidates = HashMultimap.<String,String>create();
def annotationCountMap = [:]

for(Entity lsm : e.getUserEntitiesByTypeName("user:nerna", TYPE_LSM_STACK)) {
   
    println ""+lsm.name+" "

    Entity sample = e.getAncestorWithType(null, lsm.getId(), EntityConstants.TYPE_SAMPLE);
    if (sample!=null) {
        f.loadChildren(sample);
        Entity supportingData = EntityUtils.getSupportingData(sample);
        f.loadChildren(supportingData);
        if (supportingData != null) {
            for(Entity imageTile : supportingData.getChildren()) {
                println "    "+imageTile.name
                f.loadChildren(imageTile);
                for(Entity siblingLsm : imageTile.getChildren()) {
                    println "        "+siblingLsm.name
                }
            }
        }
    }
    
}