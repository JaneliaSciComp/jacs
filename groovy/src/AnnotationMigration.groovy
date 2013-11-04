import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class AnnotationMigrationScript {
    static final OWNER = "nerna"
    static final OWNER_KEY = "user:"+OWNER
    static final OUTPUT_FILE = "annot_migration_issues.txt"

    PrintWriter file = new PrintWriter(OUTPUT_FILE)
    JacsUtils f = new JacsUtils(SampleReportConstants.OWNER_KEY, true)

    def main() {

        def samples = f.e.getUserEntitiesByTypeName(SampleAnnotationsConstants.OWNER_KEY, "Sample")
        List<Entity> orderedSamples = new ArrayList<Entity>(samples);
        Collections.sort(orderedSamples, new Comparator<Entity>() {
            int compare(Entity o1, Entity o2) {
                return o1.name.compareTo(o2.name)
            }
        })

        for(Entity sample : orderedSamples) {
            f.loadChildren(sample)

            if (sample.name.contains("~")) continue

            def data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
            def subsamples = EntityUtils.getChildrenOfType(sample, "Sample");

            if (!subsamples.isEmpty()) {
                subsamples.each {
                    f.loadChildren(it)
                    processSample(data_set, it);
                }
            }
            else {
                processSample(data_set, sample);
            }

            // free memory
            sample.setEntityData(null)
        }
    }

    def processSample(String data_set, Entity sample) {

        def annotations = getAnnotations(sample.id)
        if (annotations.isEmpty()) return // Annotated samples only

        file.println()
        file.println(sample.name)

        for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, "Pipeline Run")) {
            f.loadChildren(pipelineRun)

            file.println("    "+pipelineRun.name)

            for(Entity result : EntityUtils.getChildrenForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT)) {
                f.loadChildren(result)

                file.println("        "+result.name)

                for(Entity separation : EntityUtils.getChildrenOfType(result, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                    f.loadChildren(separation)

                    Entity neuronCollection = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)
                    f.loadChildren(neuronCollection)
                    def numNeurons = neuronCollection.children.size()
                    List<Entity> neuronAnnotations = f.a.getAnnotationsForChildren(neuronCollection.ownerKey, neuronCollection.id)
                    file.println("            "+separation.name+" ("+neuronAnnotations.size()+"/"+numNeurons+")")
                }
            }

        }
    }

    def getAnnotations(Long sampleId) {
        SolrQuery query = new SolrQuery("(id:" + sampleId + " OR ancestor_ids:" + sampleId + ") AND all_annotations:*")
        SolrDocumentList results = f.s.search(null, query, false).response.results
        List<String> annotations = new ArrayList<String>()
        results.each {
            def all = it.getFieldValues(SampleReportConstants.OWNER + "_annotations")
            if (all != null) annotations.addAll(all)
        }
        return annotations
    }


}

new AnnotationMigrationScript().main()