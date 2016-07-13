package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList


f = new JacsUtils("user:rokickik", false)

int ROWS = 50000
int page = 0
int processed = 0
int tc = 0
int td = 0

SolrQuery query = new SolrQuery("doc_type:ENTITY")
query.setRows(ROWS)
query.setSortField("_docid_", ORDER.asc)

long start = System.currentTimeMillis()

while (true) {
    
    query.setStart(page*ROWS)
    SolrDocumentList results = f.s.search(null, query, false).response.results
    
    processed += results.size()
    if (processed >= results.numFound) {
        break
    }
    
    results.each {
        SolrDocument doc = (SolrDocument)it;
        def ancestors = doc.getFieldValues("ancestor_ids");
        if (ancestors!=null) {
            depth = ancestors.size()
            tc += depth
        }
        else {
//            println "Null ancestors for "+doc
//            for(String key : doc.getFieldNames()) {
//                println "  "+key+": "+doc.getFieldValue(key)
//            }
        }
    }
    
    long now = System.currentTimeMillis()
    long elapsedSec = (now-start)/1000;
    
    println elapsedSec+"\t"+page+"\t"+processed+"\t"+tc
    
    page++
}
