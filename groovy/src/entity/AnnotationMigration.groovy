package entity

import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.StringUtils

class AnnotationMigrationScript {
    static final OWNER = "nerna"
    static final OWNER_KEY = "user:"+OWNER
    static final OUTPUT_FILE = "annot_migration.txt"
    static final ADD_ATTRS = true

    PrintStream file = new PrintStream(OUTPUT_FILE)
    JacsUtils f = new JacsUtils(SampleReportConstants.OWNER_KEY, true)
    List<MigrationCase> allMcs = new ArrayList<>()

    def main() {

        try {
            def samples = f.e.getUserEntitiesByTypeName(SampleAnnotationsConstants.OWNER_KEY, "Sample")
            println "Ordering "+samples.size()+" samples..."

            List<Entity> orderedSamples = new ArrayList<Entity>(samples);
            Collections.sort(orderedSamples, new Comparator<Entity>() {
                int compare(Entity o1, Entity o2) {
                    return o1.name.compareTo(o2.name)
                }
            })

            println "Processing "+orderedSamples.size()+" samples..."
            for(Entity sample : orderedSamples) {
                processSample(sample)
            }
        }
        catch (Throwable t) {
            t.printStackTrace()
        }

        generateReport(true, false)
        generateReport(false, false)
        generateReport(true, true)
        generateReport(false, true)

        System.exit(0)
    }

    def generateReport(boolean annotatedSource, boolean aligned) {

        int total = 0
        int totalProb = 0
        int numWarped = 0
        int numMapped = 0
        for(MigrationCase mc : allMcs) {
            if (aligned && (!mc.source.separationName.startsWith("Aligned") && !mc.target.separationName.startsWith("Aligned"))) continue
            if (!aligned && (mc.source.separationName.startsWith("Aligned") || mc.target.separationName.startsWith("Aligned"))) continue
            if (annotatedSource && mc.source.numAnnotated<=0) continue
            if (!annotatedSource && mc.source.numAnnotated>0) continue
            if (mc.target.warped) {
                numWarped++
            }
            else if (mc.target.hasMapping) {
                numMapped++
            }
            totalProb += mc.successProbability
            total++
        }

        def annotationStr = annotatedSource ? "Annotated" : "Unannotated"
        def alignmentStr = aligned ? "Aligned" : "Unaligned"

        file.println "-------------------------------------------------------"
        file.println annotationStr+" "+alignmentStr+" Separation Migration Report"
        file.println "Total separations: "+total
        file.println "Warped separations: "+numWarped
        file.println "Mapped separations: "+numMapped
        if (total>0) {
            file.println "Average success probability: "+String.format('%.2f',100*totalProb/total)+"%"
        }
    }

    def processSample(Entity sample) {
        println "Processing "+sample.name
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

        List<MigrationCase> mcs = new ArrayList<>()

        file.println()
        file.println(sample.name)

        def lastUnalignedSepForRun = null

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
                    s.separationName = separation.name
                    s.sampleName = sample.name
                    s.dataSetName = data_set
                    s.numNeurons = neuronCollection.children.size()
                    s.numAnnotated = annotatedNeurons.size()
                    s.mapping = getMapping(separation)
                    s.warped = getWarped(separation)
                    s.hasMapping = (s.mapping != null)

                    int mapCount = s.mapping==null?0:s.mapping.size()

                    def attrs = []
                    if (s.numNeurons>0) {
                        attrs.add("total="+s.numNeurons)
                    }
                    if (s.numAnnotated>0) {
                        attrs.add("annotated="+s.numAnnotated)
                    }
                    if (mapCount>0) {
                        attrs.add("mapped="+mapCount)
                    }
                    if (s.warped) {
                        attrs.add("warped=true")
                    }

                    file.println "            "+separation.name+" ("+StringUtils.getCommaDelimited(attrs)+")"

                    resultSeps.add(s)

                    if (result.entityTypeName==EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT) {
                        lastUnalignedSep = s
                        if (lastUnalignedSepForRun!=null) {
                            // Migrate between unaligned separations of multiple runs
                            MigrationCase mc = new MigrationCase()
                            mc.source = lastUnalignedSepForRun
                            mc.target = lastUnalignedSep
                            mcs.add(mc)
                        }
                    }
                }

                if (resultSeps.size()>1) {
                    for(int i=1; i<resultSeps.size()-1; i++) {
                        // Migrate between multiple separations of the same result
                        MigrationCase mc = new MigrationCase()
                        mc.source = resultSeps[i-1]
                        mc.target = resultSeps[i]
                        mcs.add(mc)
                    }
                }

                if (result.entityTypeName==EntityConstants.TYPE_ALIGNMENT_RESULT) {
                    if (lastUnalignedSep!=null) {
                        for(Separation als : resultSeps) {
                            // Migrate between unaligned and aligned separations in the same run
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

                lastUnalignedSepForRun = lastUnalignedSep
            }
        }

        file.println "Migrations:"
        for(MigrationCase mc : mcs) {

            if (ADD_ATTRS) {
                f.e.addEntityToParent(OWNER_KEY, mc.target.id, mc.source.id, null, EntityConstants.ATTRIBUTE_SOURCE_SEPARATION)
                if (mc.target.warped) {
                    f.e.setOrUpdateValue(OWNER_KEY, mc.target.id, EntityConstants.ATTRIBUTE_IS_WARPED_SEPARATION, EntityConstants.ATTRIBUTE_IS_WARPED_SEPARATION)
                }
            }

            if (mc.target.warped) {
                if (mc.source.numNeurons<=mc.target.numNeurons) {
                    mc.successProbability = mc.target.numNeurons==0?1:(mc.source.numNeurons / mc.target.numNeurons)
                }
                else {
                    mc.successProbability = mc.source.numNeurons==0?1:(mc.target.numNeurons / mc.source.numNeurons)
                }
            }
            else {
                int mapCount = mc.target.mapping==null?0:+mc.target.mapping.size()
                mc.successProbability =  mc.source.numNeurons==0?1:(mapCount / mc.source.numNeurons)
            }

            file.println mc.source.separationName+" -> "+mc.target.separationName+" ("+String.format('%.2f',mc.successProbability*100)+"%)"
        }

        for(MigrationCase mc : mcs) {
            mc.source.mapping = null // Save memory
            mc.target.mapping = null
            allMcs.add(mc)
        }
    }

    def Map<Integer,Integer> getMapping(Entity separation) {
        String dir = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
        File mappingFile = new File(dir,"mapping_issues.txt")
        Scanner scanner = null

        try {
            File cachedFile = SessionMgr.getCachedFile(mappingFile.getAbsolutePath(), false)
            scanner = new Scanner(cachedFile).useDelimiter("\\n")
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
            return mapping
        }
        catch (Exception e) {
            println "  Could not read mapping file: "+e.getMessage()
            return null
        }
        finally {
            if (scanner!=null) scanner.close()
        }
    }

    def boolean getWarped(Entity separation) {
        String dir = separation.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH)
        Scanner scanner = null

        try {
            File configFile = new File(dir+"/sge_config","neuSepConfiguration.1")
            File cachedFile = SessionMgr.getCachedFile(configFile.getAbsolutePath(), false)
            scanner = new Scanner(cachedFile).useDelimiter("\\n")
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line =~ /NeuronAligned/) {
                    return true
                }
            }
        }
        catch (Exception e) {
            println "  Could not read config file: "+e.getMessage()
        }
        finally {
            if (scanner!=null) scanner.close()
        }
        return false
    }

    class Separation {
        Long id
        String sampleName
        String separationName
        String dataSetName
        int numNeurons
        int numAnnotated
        boolean warped = false
        boolean hasMapping = false
        Map<Integer,Integer> mapping = null
    }

    class MigrationCase {
        Separation source
        Separation target
        double successProbability
    }
}

new AnnotationMigrationScript().main()