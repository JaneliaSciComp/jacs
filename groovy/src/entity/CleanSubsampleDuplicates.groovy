package entity

import com.google.common.base.Strings
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

import static org.janelia.it.jacs.model.entity.EntityConstants.*

boolean DEBUG = false

String ownerKey = "user:asoy";
f = new JacsUtils(ownerKey, false);

treesToDelete = []

for(Entity sample : f.e.getEntitiesByTypeName(ownerKey, TYPE_SAMPLE)) {

    if (sample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE)!=null) continue

    f.loadChildren(sample)

    boolean saw20xSample = false
    boolean saw63xSample = false

    List<Entity> subsamples = EntityUtils.getChildrenOfType(sample, TYPE_SAMPLE)

    if (!subsamples.isEmpty()) {
        println ""+sample.name
        Collections.reverse(subsamples)
        for(Entity subsample : subsamples) {
            if ("20x".equals(subsample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE))) {
                if (saw20xSample) {
                    println "    DELETE: "+subsample.name
                    treesToDelete.add(subsample.id)
                }
                else {
                    println "    SAVE: "+subsample.name
                    saw20xSample = true;
                }
            }
            else if ("63x".equals(subsample.getValueByAttributeName(ATTRIBUTE_OBJECTIVE))) {
                if (saw63xSample) {
                    println "    DELETE: "+subsample.name
                    treesToDelete.add(subsample.id)
                }
                else {
                    println "    SAVE: "+subsample.name
                    saw63xSample = true;
                }
            }
        }
    }


    sample.setEntityData(null)
}

println ""
println "DELETING SAMPLES:"
for(Long sampleId : treesToDelete) {
    println "Delete "+sampleId
    if (!DEBUG) f.e.deleteEntityTreeById(ownerKey, sampleId)
}

println "Done"
