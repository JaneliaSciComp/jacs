package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.janelia.it.jacs.compute.api.SolrBeanRemote
import org.janelia.it.jacs.compute.api.support.MappedId;
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants;

// Globals
username = "leetlab"
JacsUtils f = new JacsUtils(username, false)
e = f.e
c = f.c
SolrBeanRemote s = f.s

samples = [:]
sample2lsm = [:]
lsm2sample = [:]
sample2stitch = [:]
stitch2sample = [:]
sample2path = [:]

SolrQuery query = new SolrQuery("+44F03 +BLM +entity_type:Sample +username:leetlab")
query.rows = 1000
List<Entity> results = s.search(query, true).resultList
for(Entity sample : results) {
    samples[sample.id] = sample
}

println "Found "+samples.size()+" samples"

upMapping = []
downMapping = [TYPE_PIPELINE_RUN, TYPE_SAMPLE_PROCESSING_RESULT]
mappedIds = e.getProjectedResults(samples.keySet().toList(), upMapping, downMapping);
for (MappedId mappedId : mappedIds) {
    if (sample2stitch.containsKey(mappedId.originalId)) continue
    sample2stitch[mappedId.originalId] = mappedId.mappedId
    stitch2sample[mappedId.mappedId] = mappedId.originalId
}

stitch2sample.keySet().each {
    stitchId = it
    SolrQuery query2 = new SolrQuery("id:"+stitchId)
    Iterator<SolrDocument> i = s.search(query2, true).response.results.iterator();
    while (i.hasNext()) {
        SolrDocument doc = i.next();
        sampleId = stitch2sample.get(stitchId)
        sample2path[sampleId] = doc.get("default_3d_image_txt")[0]
    }  
}

upMapping = []
downMapping = [TYPE_SUPPORTING_DATA, TYPE_IMAGE_TILE, TYPE_LSM_STACK]
mappedIds = e.getProjectedResults(samples.keySet().toList(), upMapping, downMapping);
for (MappedId mappedId : mappedIds) {
    if (sample2lsm.containsKey(mappedId.originalId)) continue
    sample2lsm[mappedId.originalId] = mappedId.mappedId
    lsm2sample[mappedId.mappedId] = mappedId.originalId
}

println "Sample Name\tStage Basis\tHeat Shock Hour\tStitched Stack"

lsm2sample.keySet().each {
    lsmId = it
    SolrQuery query2 = new SolrQuery("id:"+lsmId)
    Iterator<SolrDocument> i = s.search(query2, true).response.results.iterator();
    while (i.hasNext()) {
        SolrDocument doc = i.next();
        sampleId = lsm2sample.get(lsmId)
        sample = samples.get(sampleId)
        sage_heat_shock_hour = doc.get("sage_heat_shock_hour_l")
        sage_stage_basis = doc.get("sage_stage_basis_t")
        default_3d_image = sample2path[sampleId]
        println sample.name+"\t"+sage_stage_basis+"\t"+sage_heat_shock_hour+"\t"+default_3d_image
    }
}

