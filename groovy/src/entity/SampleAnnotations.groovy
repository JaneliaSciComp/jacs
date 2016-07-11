package entity

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.model.entity.EntityConstants

class SampleAnnotationsConstants {
    static final OWNER = "nerna"
    static final OWNER_KEY = "user:"+OWNER
    static final OUTPUT_FILE = "/Users/rokickik/samples." + (OUTPUT_HTML?"html":"txt")
    static final OUTPUT_HTML = false;
}

def f = new JacsUtils(SampleAnnotationsConstants.OWNER_KEY, true)
def file = new PrintWriter(SampleAnnotationsConstants.OUTPUT_FILE)

if (SampleAnnotationsConstants.OUTPUT_HTML) {
    file.println("<html><body><head><style>" +
            "td { font: 8pt sans-serif; vertical-align:top; border: 0px solid #aaa;} table { border-collapse: collapse; } " +
            "</style></head>")
    file.println("<h3>"+SampleAnnotationsConstants.OWNER+" Sample Annotation Report</h3>")
    file.println("<table>")
    file.println("<tr><td>Sample Name</td><td>Data Set</td><td>Fragments</td><td>Annotations</td></tr>")
}
else {
    file.println("GUID\tSample Name\tData Set\tFragments\tAnnotations");
}

def samples = f.e.getUserEntitiesByTypeName(SampleAnnotationsConstants.OWNER_KEY, "Sample")
List<Entity> orderedSamples = new ArrayList<Entity>(samples);
Collections.sort(orderedSamples, new Comparator<Entity>() {
    int compare(Entity o1, Entity o2) {
        return o1.name.compareTo(o2.name)
    }
})

for(Entity sample : orderedSamples) {

    annotations = getAnnotations(f, sample.id)
    data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)

    SampleAnnotationsNeuronCounter counter = new SampleAnnotationsNeuronCounter(f.getEntityLoader())
    counter.count(sample)

    def annots = annotations.toString()

    if (SampleAnnotationsConstants.OUTPUT_HTML) {
        file.println("<tr>");
        file.println("<td><nobr><b>"+sample.name+"</b></nobr></td>")
        file.println("<td><nobr>"+data_set+"</nobr></td>")
        file.println("<td>"+counter.numFragments+"</td>")
        file.println("<td>"+annots.substring(1,annots.length()-1)+"</td>")
        file.println("</tr>");
    }
    else {
        file.println(sample.id+"\t"+sample.name+"\t"+data_set+"\t"+counter.numFragments+"\t"+annots.substring(1,annots.length()-1));
    }

    // free memory
    sample.setEntityData(null)
}

if (SampleAnnotationsConstants.OUTPUT_HTML) {
    file.println("</table>")
    file.println("</body></html>")
}

file.close()

def getAnnotations(JacsUtils f, Long sampleId) {
    SolrQuery query = new SolrQuery("(id:"+sampleId+" OR ancestor_ids:"+sampleId+") AND all_annotations:*")
    SolrDocumentList results = f.s.search(null, query, false).response.results
    List<String> annotations = new ArrayList<String>()
    results.each {
        def all = it.getFieldValues(SampleAnnotationsConstants.OWNER+"_annotations")
        if (all!=null) annotations.addAll(all)
    }
    return annotations
}

class SampleAnnotationsNeuronCounter {

    AbstractEntityLoader loader
    int numFragments;

    public SampleAnnotationsNeuronCounter(AbstractEntityLoader loader) {
        this.loader = loader
        this.numFragments = 0
    }

    def count(Entity sample) {
        EntityVistationBuilder.create(loader).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
                .childrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).last()
                .childOfName("Neuron Fragments")
                .run(new EntityVisitor() {
            public void visit(Entity fragmentCollection) throws Exception {
                numFragments = EntityUtils.getChildrenForAttribute(fragmentCollection, EntityConstants.ATTRIBUTE_ENTITY).size()
            }
        });
    }
}
