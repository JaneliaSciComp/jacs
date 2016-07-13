package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList


f = new JacsUtils("user:rokickik", false)

SolrQuery query = new SolrQuery("common_root_txt:*")
query.setStart(0)
query.setRows(10000)
query.setSortField("_docid_", ORDER.asc)

SolrDocumentList results = f.s.search(null, query, false).response.results

results.each {
    SolrDocument doc = (SolrDocument)it;
    
    SolrQuery subquery = new SolrQuery("ancestor_ids:"+doc.getFieldValue("id"))
    subquery.setStart(0)
    subquery.setRows(1)
    SolrDocumentList subresults = f.s.search(null, subquery, false).response.results
    treeSize = subresults.numFound
        
    println doc.getFieldValue("username")+"\t"+doc.getFieldValue("id")+"\t"+doc.getFieldValue("name")+"\t"+treeSize
}
