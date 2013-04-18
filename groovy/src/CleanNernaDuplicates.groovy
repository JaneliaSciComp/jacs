import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

String ownerKey = "group:flylight";
f = new JacsUtils(ownerKey, false);

Multimap<String,Long> slideCodeToSampleIdMap = HashMultimap.<String,Long>create();
Multimap<String,String> slideCodeToSampleNameMap = HashMultimap.<String,String>create();

registerSamples(slideCodeToSampleIdMap, slideCodeToSampleNameMap, f.e.getUserEntitiesByTypeName("group:flylight", TYPE_SAMPLE))
registerSamples(slideCodeToSampleIdMap, slideCodeToSampleNameMap, f.e.getUserEntitiesByTypeName("user:nerna", TYPE_SAMPLE))

for(String slideCode : slideCodeToSampleIdMap.keys()) {
    def sampleIds = slideCodeToSampleIdMap.get(slideCode)
    if (sampleIds.size()>1) {

        def sampleNames = slideCodeToSampleNameMap.get(slideCode)

        // Skip duplicates which are left/right optic lobes

        boolean hasLeftOptic = false;
        boolean hasRightOptic = false;
        sampleNames.each {
            if (it.endsWith("Left_Optic_Lobe")) hasLeftOptic = true;
            if (it.endsWith("Right_Optic_Lobe")) hasRightOptic = true;
        }

        if (sampleNames.size()==2 && hasLeftOptic && hasRightOptic) continue;

        println(slideCode);

        Multimap<Long,String> sampleIdToAnnotations = HashMultimap.<Long,String>create();

        def annotationCountMap = [:]
        for(Long sampleId : sampleIds) {

            SolrQuery query = new SolrQuery("(id:"+sampleId+" OR ancestor_ids:"+sampleId+") AND all_annotations:*")
            SolrDocumentList results = f.s.search(null, query, false).response.results

            for(SolrDocument doc : results) {
                for(String fieldName : doc.getFieldNames()) {
                    if (fieldName.endsWith("_annotations")) {
                        sampleIdToAnnotations.putAll(sampleId, doc.getFieldValues(fieldName))
                    }
                }
            }

            int count = results.numFound
            //int count = f.a.getNumDescendantsAnnotated(sampleId);
            annotationCountMap.put(sampleId, count)
        }

        for(Entity sample in f.e.getEntitiesById(null, new ArrayList<Long>(sampleIds))) {
            def annotations = sampleIdToAnnotations.get(sample.id)
            def dataSet = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
            def visited = "Visited".equals(sample.getValueByAttributeName(ATTRIBUTE_VISITED))
            println "    ("+annotationCountMap.get(sample.id)+") "+sample.id+" visited="+visited+"\t"+sample.ownerKey+"\t"+dataSet+"\t"+sample.name+" "+annotations
        }

    }
}

println "Done"


def registerSamples(slideCodeToSampleMap, slideCodeToSampleNameMap, samples) {
    for(Entity sample : samples) {
        try {
            def m = sample.name =~ /(.*?)-(\d+_\d+_\w+)(-(.+?))?/
            def line = m[0][1]
            def slideCode = m[0][2]
//            println "name: "+sample.name
//            println "  line: "+line
//            println "  slideCode: "+slideCode
            slideCodeToSampleMap.put(slideCode, sample.id);
            slideCodeToSampleNameMap.put(slideCode, sample.name)
        }
        catch (Exception e) {
            println "ERROR: cannot parse sample name "+sample.name+": "+e.message
        }
    }
}