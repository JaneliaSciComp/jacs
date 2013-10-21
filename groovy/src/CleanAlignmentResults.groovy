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

for(Entity folder : f.e.getUserEntitiesByNameAndTypeName(ownerKey, "Pan Lineage 40x", "Folder")) {
    f.loadChildren(folder)
    EntityVistationBuilder.create(f.getEntityLoader())
            .startAt(folder)
            .childrenOfType(EntityConstants.TYPE_SAMPLE)
            .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
            .childrenOfType(EntityConstants.TYPE_ALIGNMENT_RESULT)
            .run(new EntityVisitor() {
        public void visit(Entity alignmentEntity) throws Exception {
            SolrQuery query = new SolrQuery("(id:"+alignmentEntity.id+" OR ancestor_ids:"+alignmentEntity.id+") AND all_annotations:*")
            SolrDocumentList results = f.s.search(null, query, false).response.results
            int count = results.numFound

            numProcessed++
            if (count==0) {
                toDelete.add(alignmentEntity)
            }
            println "  "+count+"\tAlignment Result "+alignmentEntity.id+" ("+toDelete.size()+" will be nuked of "+numProcessed+")"

        }
    });
}

println "Processed "+numProcessed+" samples. Found "+toDelete.size()+" candidate Alignment Results for deletion"
if (!DEBUG) {
    println "DELETING SAMPLES:"
    for(Entity alignmentResult in toDelete) {
//        f.e.deleteSmallEntityTree(alignmentResult.ownerKey, alignmentResult.id, true)
    }
}

println "Done"

