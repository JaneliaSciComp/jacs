package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*;
import static org.janelia.it.jacs.shared.utils.EntityUtils.*;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.janelia.it.jacs.compute.api.SolrBeanRemote;
import org.janelia.it.jacs.shared.solr.SolrResults;
import org.janelia.it.jacs.model.entity.Entity;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

// Globals
filename = "ALlvSampleList.txt";
username = "leetlab";
JacsUtils f = new JacsUtils(username, false);
e = f.e;
c = f.c;
SolrBeanRemote s = f.s;


List<String> lines = Files.readLines(new File(filename), Charsets.UTF_8);

lines.each {
    
    print it+"\t";
    SolrQuery query = new SolrQuery("+"+it);
    
    SolrResults solrResults = s.search(null, query, true);
    List<Entity> results = solrResults.resultList;
    
    if (results.isEmpty()) {
        println ""
    }
    else {
        for(Entity result : results) {
        
            aligned = result.getValueByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE);
            stitched = "";
            
            f.loadChildren(result)
            for (Entity run : result.children) {
                if (run.entityTypeName == TYPE_PIPELINE_RUN) {
                    f.loadChildren(run)
                    for (Entity entity : run.children) {
                        if (entity.entityTypeName == TYPE_SAMPLE_PROCESSING_RESULT) {
                            stitched = entity.getValueByAttributeName(ATTRIBUTE_DEFAULT_3D_IMAGE);
                        }   
                    }
                }
            }
            
            print stitched+"\t";
            print aligned+"\n";
        }
    }
}
