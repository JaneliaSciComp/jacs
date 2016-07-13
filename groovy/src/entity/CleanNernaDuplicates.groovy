package entity

import com.google.common.base.Strings
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

boolean DEBUG = false

String ownerKey = "group:flylight";
f = new JacsUtils(ownerKey, false);

Multimap<String,Long> slideCodeToSampleIdMap = HashMultimap.<String,Long>create();
Multimap<String,String> slideCodeToSampleNameMap = HashMultimap.<String,String>create();
Multimap<Long,String> sampleIdToAnnotations = HashMultimap.<Long,String>create();
Multimap<String,String> slideCodeToSampleDeletionCandidates = HashMultimap.<String,String>create();
def annotationCountMap = [:]

registerSamples(slideCodeToSampleIdMap, slideCodeToSampleNameMap, f.e.getUserEntitiesByTypeName("group:flylight", TYPE_SAMPLE))
registerSamples(slideCodeToSampleIdMap, slideCodeToSampleNameMap, f.e.getUserEntitiesByTypeName("user:nerna", TYPE_SAMPLE))

for(String slideCode : slideCodeToSampleIdMap.keySet()) {
    def sampleIds = slideCodeToSampleIdMap.get(slideCode)
    if (sampleIds.size()>1) {

        def sampleNames = slideCodeToSampleNameMap.get(slideCode)

        // Skip samples which are left/right optic lobes
        if (sampleNames.size()==2 && hasBothOptics(sampleNames)) continue;

        println Strings.padStart("", 200, '-' as char)
        println(slideCode);

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

        def samples = f.e.getEntitiesById(null, new ArrayList<Long>(sampleIds))
        
        int numAnnotated = 0
        
        for(Entity sample in samples) {
            def annotations = sampleIdToAnnotations.get(sample.id)
            def dataSet = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
            def visited = "Visited".equals(sample.getValueByAttributeName(ATTRIBUTE_VISITED))
            
            int annotationCount = annotationCountMap.get(sample.id)
            if (annotationCount>0) numAnnotated++
            
            char s = ' '
            def countPad = Strings.padStart(annotationCount>0?""+annotationCount:"", 3, s)
            def visitedPad = visited?"visited":"       "
            def ownerPad = Strings.padEnd(sample.ownerKey, 15, s)
            def dataSetPad = Strings.padEnd(dataSet==null?"":dataSet, 40, s)
            def sampleNamePad = Strings.padEnd(sample.name, 60, s)
            
            print "    ("+countPad+") "+sample.id+"  "+visitedPad+"  "+ownerPad+"  "+dataSetPad+"  "+sampleNamePad+" "
            
            if (annotations.isEmpty()) {
                println " --- DELETE"
                slideCodeToSampleDeletionCandidates.put(slideCode, sample)
            }
            else {
                if (dataSet!=null) {
                    if (dataSet.endsWith("optic_central_border") && !sample.name.endsWith("Optic_Central_Border")) {
                        sample.name = sample.name+"-Optic_Central_Border"
                        print " --- Renaming sample to : "+sample.name
                        if (!DEBUG) f.e.saveOrUpdateEntity(sample.ownerKey, sample)
                    }
                    else if (dataSet.endsWith("optic_lobe_tile") && !sample.name.endsWith("Optic_Lobe")) {
                        print " --- Sample should be renamed but I don't know what to call it!"
                    }
                    else if (dataSet.endsWith("optic_lobe_left") && !sample.name.endsWith("Left_Optic_Lobe")) {
                        sample.name = sample.name+"-Left_Optic_Lobe"
                        print " --- Renaming sample to : "+sample.name
                        if (!DEBUG) f.e.saveOrUpdateEntity(sample.ownerKey, sample)
                    }
                    else if (dataSet.endsWith("optic_lobe_right") && !sample.name.endsWith("Right_Optic_Lobe")) {
                        sample.name = sample.name+"-Right_Optic_Lobe"
                        print " --- Renaming sample to : "+sample.name
                        if (!DEBUG) f.e.saveOrUpdateEntity(sample.ownerKey, sample)
                    }
                    else {
                        print " --- No action"
                    }
                }
                else {
                    print " --- No action since data set is null"
                }
                
                if (annotations) {
                    print "  --- "
                    int i = 0;
                    for(String a : annotations) {
                        if (i>0) print ", "
                        print a
                        i++
                        if (i>3) break
                    }
                    
                }
                
                println ""
            }
        }
            
        if (numAnnotated>1) {
            println "    Multiple annotated samples!"
        }
    }
}

println ""
println "DELETING SAMPLES:"
for(String slideCode : slideCodeToSampleDeletionCandidates.keySet()) {
    def samples = slideCodeToSampleDeletionCandidates.get(slideCode)
    for(Entity sample in samples) {
        def annotations = sampleIdToAnnotations.get(sample.id)
        def dataSet = sample.getValueByAttributeName(ATTRIBUTE_DATA_SET_IDENTIFIER)
        def visited = "Visited".equals(sample.getValueByAttributeName(ATTRIBUTE_VISITED))
        println slideCode+" ("+annotationCountMap.get(sample.id)+") "+sample.id+" "+sample.name+" "
        if (!DEBUG) f.e.deleteEntityTreeById(sample.ownerKey, sample.id)
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

def hasBothOptics(sampleNames) {
    boolean hasLeftOptic = false;
    boolean hasRightOptic = false;
    sampleNames.each {
        if (it.endsWith("Left_Optic_Lobe")) hasLeftOptic = true;
        if (it.endsWith("Right_Optic_Lobe")) hasRightOptic = true;
    }
    return hasLeftOptic && hasRightOptic
}
