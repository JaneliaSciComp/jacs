import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;

def folderId = 1943163501170130951L
def idGen = new TimebasedIdentifierGenerator();

def file = new PrintWriter("insert_perms_tanya_seeligj.sql")

f = new JacsUtils("user:rokickik", false)

int ROWS = 50000
int page = 0
int processed = 0

SolrQuery query = new SolrQuery("ancestor_ids:"+folderId)
query.setRows(ROWS)
query.setSortField("_docid_", ORDER.asc)
query.setFields("id")

long start = System.currentTimeMillis()

while (true) {
    
    query.setStart(page*ROWS)
    SolrDocumentList results = f.s.search(null, query, false).response.results
    
    def newIds = idGen.generateIdList(results.size())
    def newIdIndex = 0
    
    results.each {
        SolrDocument doc = (SolrDocument)it;
        def entityId = it.getFieldValue("id")
        def newId = newIds.get(newIdIndex++)
        sql = "insert into entity_actor_permission values ("+newId+","+entityId+",'user:seeligj','r');";
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