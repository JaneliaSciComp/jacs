import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator

// Add the target folderId, custom PrintWriter file for the SQL, whether to include the parent folder, and the target entity_actor_permission values below
// What folder's items are we sharing?
def folderId = 1993023180708511751L
// Who are we sharing the items with?
def targetEntityActor = "group:flylighttechnical";
// Where should we write the SQL strings to?
def file = new PrintWriter("insert_perms_nerna_polaritycase4_flylighttechnical.sql")
// Should the parent folder get the permissions so the new children get the same sharing?
def extendPermissionsToParentFolder = true;
// What privs are they getting?
def permissions = "r";

def idGen = new TimebasedIdentifierGenerator();
f = new JacsUtils(null, false)

int ROWS = 50000
int page = 0
int processed = 0

SolrQuery query = new SolrQuery("ancestor_ids:"+folderId)
query.setRows(ROWS)
query.setSortField("_docid_", ORDER.asc)
query.setFields("id")

long start = System.currentTimeMillis()
def newIds = idGen.generateIdList(1)
def newIdIndex = 0
if (extendPermissionsToParentFolder) {
    sql = "insert into entity_actor_permission values ("+(newIds.get(newIdIndex))+","+folderId+",'"+
            targetEntityActor+"','"+permissions+"');";
    file.println(sql)
}
while (true) {
    query.setStart(page*ROWS)
    SolrDocumentList results = f.s.search(null, query, false).response.results
    
    newIds = idGen.generateIdList(results.size()+1)
    newIdIndex = 0
    results.each {
        SolrDocument doc = (SolrDocument)it;
        def entityId = it.getFieldValue("id")
        def newId = newIds.get(newIdIndex++)
        sql = "insert into entity_actor_permission values ("+newId+","+entityId+",'"+
                targetEntityActor+"','"+permissions+"');";
        file.println(sql)
    }
    
    long elapsedSec = (System.currentTimeMillis()-start)/1000;
    println "Page "+page+", Size "+results.size()+", Elapsed "+elapsedSec+" sec"
    page++
    
    processed += results.size()
    if (processed >= results.numFound) {
        break
    }
}

file.close()
println "Done"
System.exit(0)
