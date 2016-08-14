package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder

boolean DEBUG = false

String ownerKey = "group:leetlab"
final JacsUtils f = new JacsUtils(ownerKey, false)

int numProcessed = 0
final toDelete = []

println "INSPECTING SAMPLES:"

for(Entity folder : f.e.getUserEntitiesByNameAndTypeName(ownerKey, "Pan Lineage RQZ", "Folder")) {
    f.loadChildren(folder)
    EntityVistationBuilder.create(f.getEntityLoader())
            .startAt(folder)
            .childrenOfType(EntityConstants.TYPE_SAMPLE)
            .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
            .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT)
            .childrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)
            .run(new EntityVisitor() {
        public void visit(Entity separationEntity) throws Exception {
            SolrQuery query = new SolrQuery("(id:"+separationEntity.id+" OR ancestor_ids:"+separationEntity.id+") AND all_annotations:*")
            SolrDocumentList results = f.s.search(null, query, false).response.results
            int count = results.numFound

            numProcessed++
            if (count==0) {
                toDelete.add(separationEntity)
            }
            println "  "+count+"\tSeparation Result "+separationEntity.id+" ("+toDelete.size()+" will be nuked of "+numProcessed+")"

        }
    });
}

println "Processed "+numProcessed+" samples. Found "+toDelete.size()+" candidate Separation Results for deletion"
if (!DEBUG) {
    println "DELETING SEPARATION:"
    for(Entity separationResult in toDelete) {
        // Unlink multiple parents boolean is the other signature for the method below.  USE WITH EXTREME CAUTION!!!!
        // Messing that up will delete entities even if others have references to them.
        f.e.deleteSmallEntityTree(separationResult.ownerKey, separationResult.id)
    }
}

println "Done"

