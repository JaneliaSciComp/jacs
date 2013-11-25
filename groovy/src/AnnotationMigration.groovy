import com.google.common.collect.Iterables
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.FlyWorkstation.shared.util.WorkstationFile
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class AnnotationMigrationScript {
    static final OWNER = "nerna"
    static final OWNER_KEY = "user:"+OWNER
    static final OUTPUT_FILE = "annot_migration.txt"

    //System.out//
    PrintStream file = new PrintStream(OUTPUT_FILE)
    JacsUtils f = new JacsUtils(SampleReportConstants.OWNER_KEY, true)
    List<MigrationCase> allMcs = new ArrayList<>()

    def main() {

//        processSample(f.e.getEntityById(OWNER_KEY, 1903189566282530914L))
//        processSample(f.e.getEntityById(OWNER_KEY, 1679282453644050530L))
//        processSample(f.e.getEntityById(OWNER_KEY, 1889491941918244962L))
//        processSample(f.e.getEntityById(OWNER_KEY, 1679282498829287522L))


        def samples = f.e.getUserEntitiesByTypeName(SampleAnnotationsConstants.OWNER_KEY, "Sample")
        List<Entity> orderedSamples = new ArrayList<Entity>(samples);
        Collections.sort(orderedSamples, new Comparator<Entity>() {
            int compare(Entity o1, Entity o2) {
                return o1.name.compareTo(o2.name)
            }
        })

        for(Entity sample : orderedSamples) {
            processSample(sample)
        }

        generateReport(true)
        generateReport(false)

        System.exit(0)
    }

    def generateReport(boolean annotatedSource) {

        int total = 0
        int totalProb = 0
        int numWarped = 0
        int numMapped = 0
        for(MigrationCase mc : allMcs) {
            if (annotatedSource && mc.source.numAnnotated<=0) continue
            if (!annotatedSource && mc.source.numAnnotated>0) continue
            if (mc.target.warped) {
                numWarped++
            }
            else if (mc.target.mapping!=null) {
                numMapped++
            }
            totalProb += mc.successProbability
            total++
        }

        def name = annotatedSource ? "Annotated" : "Unannotated"

        file.println "-------------------------------------------------------"
        file.println name+" Separation Migration Report"
        file.println "Total separations: "+total
        file.println "Warped separations: "+numWarped
        file.println "Mapped separations: "+numMapped
        file.println "Average success probability: "+String.format('%.2f',100*totalProb/total)+"%"
    }

    def processSample(Entity sample) {
        f.loadChildren(sample)

        if (sample.name.contains("~")) return

        def data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
        def subsamples = EntityUtils.getChildrenOfType(sample, "Sample");

        if (!subsamples.isEmpty()) {
            subsamples.each {
                f.loadChildren(it)
                processTrueSample(data_set, it);
            }
        }
        else {
            processTrueSample(data_set, sample);
        }

        // free memory
        sample.setEntityData(null)
    }

    def processTrueSample(String data_set, Entity sample) {

//        def annotations = getAnnotations(sample.id)
//        if (annotations.isEmpty()) return // Annotated samples only

        List<MigrationCase> mcs = new ArrayList<>()

        file.println()
        file.println(sample.name)

        for(Entity pipelineRun : EntityUtils.getChildrenOfType(sample, "Pipeline Run")) {
            f.loadChildren(pipelineRun)

            file.println("    "+pipelineRun.name)

            def lastUnalignedSep = null

            for(Entity result : EntityUtils.getChildrenForAttribute(pipelineRun, EntityConstants.ATTRIBUTE_RESULT)) {
                f.loadChildren(result)

                file.println("        "+result.name)

                // Ignore VNC results. This is a bit of a hack, since we have no easy way to match results to alignments.
                def area = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
                if (area!=null && area.startsWith("VNC")) continue

                def resultSeps = []

                for(Entity separation : EntityUtils.getChildrenOfType(result, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
                    f.loadChildren(separation)

                    Entity neuronCollection = EntityUtils.findChildWithType(separation, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION)
                    f.loadChildren(neuronCollection)

                    List<Entity> neuronAnnotations = f.a.getAnnotationsForChildren(neuronCollection.ownerKey, neuronCollection.id)
                    Set<String> annotatedNeurons = new HashSet<>()
                    for(Entity annotation : neuronAnnotations) {
                        String targetId = annotation.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID)
                        if (targetId!=null) annotatedNeurons.add(targetId)
                    }

                    Separation s = new Separation()
                    s.id = separation.id
                    s.sampleName = sample.name
                    s.separationName = separation.name
                    s.dataSetName = data_set
                    s.numNeurons = neuronCollection.children.size()
                    s.numAnnotated = annotatedNeurons.size()
                    s.mapping = getMapping(separation)
                    s.warped = getWarped(separation)

                    int mapCount = s.mapping==null?0:s.mapping.size()
                    def isWarped = s.warped?", warped":""
                    file.println("            "+separation.name+" (total="+s.numNeurons+", annotated="+s.numAnnotated+", mapped="+mapCount+isWarped+")")

                    resultSeps.add(s)

                    if (result.entityType.name==EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT) {
                        lastUnalignedSep = s
                    }
                }

                if (resultSeps.size()>1) {
                    for(int i=1; i<resultSeps.size()-1; i++) {
                        MigrationCase mc = new MigrationCase()
                        mc.source = resultSeps[i-1]
                        mc.target = resultSeps[i]
                        mcs.add(mc)
                    }
                }

                if (result.entityType.name==EntityConstants.TYPE_ALIGNMENT_RESULT) {
                    if (lastUnalignedSep!=null) {
                        for(Separation als : resultSeps) {
                            MigrationCase mc = new MigrationCase()
                            mc.source = lastUnalignedSep
                            mc.target = als
                            mcs.add(mc)
                        }
                    }
                    else {
                        file.println "Alignment has no source: "+result.id
                    }
                }
            }
        }

        file.println "Migrations:"
        for(MigrationCase mc : mcs) {
            if (mc.target.warped) {
                if (mc.source.numNeurons<=mc.target.numNeurons) {
                    mc.successProbability = mc.source.numNeurons / mc.target.numNeurons
                }
                else {
                    mc.successProbability = mc.target.numNeurons / mc.source.numNeurons
                }
            }
            else {
                int mapCount = mc.target.mapping==null?0:+mc.target.mapping.size()
                mc.successProbability = mapCount / mc.source.numNeurons
            }

            file.println mc.source.separationName+" -> "+mc.target.separationName+" ("+String.format('%.2f',mc.successProbability*100)+"%)"
        }

        allMcs.addAll(mcs)
    }

    def Map<Integer,Integer> getMapping(Entity separation) {
        String dir = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
        File mappingFile = new File(dir,"mapping_issues.txt")
        WorkstationFile file = new WorkstationFile(mappingFile.absolutePath)
        try {
            file.get()
            if (file.statusCode!=200) return null
            InputStream is = file.getStream()
            Scanner scanner = new Scanner(is).useDelimiter("\\n")
            Map<Integer,Integer> mapping = new HashMap<>()

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                def m = (line =~ /^Previous index (.*?) : Mapped index (.*?)$/)
                if (m.matches()) {
                    String prevIndex = m[0][1]
                    String currIndex = m[0][2]
                    try {
                        mapping.put(Integer.parseInt(prevIndex), Integer.parseInt(currIndex))
                    }
                    catch (NumberFormatException e) {
                        //System.out.println("            Could not format indexes: "+prevIndex+"->"+currIndex);
                    }
                }
            }
            scanner.close()
            is.close()
            return mapping
        }
        catch (Exception e) {
            System.out.println("            Could not read mapping file: "+e.getMessage());
            return null
        }
    }

    def boolean getWarped(Entity separation) {
        String dir = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
        File configFile = new File(dir+"/sge_config","neuSepConfiguration.1")

        WorkstationFile file = new WorkstationFile(configFile.absolutePath)
        try {
            file.get()
            InputStream is = file.getStream()
            Scanner scanner = new Scanner(is).useDelimiter("\\n")
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line =~ /NeuronAligned/) {
                    return true
                }
            }
        }
        catch (Exception e) {
            System.out.println("            Could not read config file: "+e.getMessage());
        }
        return false
    }

    class Separation {
        String sampleName
        String separationName
        String dataSetName
        Long id
        int numNeurons
        int numAnnotated
        Map<Integer,Integer> mapping = null
        boolean warped = false
    }

    class MigrationCase {
        Separation source
        Separation target
        double successProbability
    }
}

new AnnotationMigrationScript().main()