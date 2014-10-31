import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

boolean DEBUG = false

String ownerKey = "user:asoy"
f = new JacsUtils(ownerKey, false)

int numProcessed = 0
toDelete = []

println "INSPECTING SAMPLES:"

for(Entity folder : f.e.getUserEntitiesByNameAndTypeName(ownerKey, "Retired Data", "Folder")) {
    f.loadChildren(folder)
    for(Entity sample : EntityUtils.getChildrenOfType(folder, "Sample")) {

        sampleId = sample.id
        SolrQuery query = new SolrQuery("(id:"+sampleId+" OR ancestor_ids:"+sampleId+") AND all_annotations:*")
        SolrDocumentList results = f.s.search(null, query, false).response.results
        int count = results.numFound

        println "  "+count+"\t"+sample.name+" "

        if (count==0) {
            toDelete.add(sample)
        }

        numProcessed++
    }
}

println "Processed "+numProcessed+" samples. Found "+toDelete.size()+" candidate samples for deletion"

if (!DEBUG) {
    println "DELETING SAMPLES:"
    for(Entity sample in toDelete) {
        f.e.deleteEntityTreeById(sample.ownerKey, sample.id, true)
    }
}

println "Done"

