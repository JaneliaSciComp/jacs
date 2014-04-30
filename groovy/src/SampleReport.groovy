import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.model.entity.EntityConstants

class SampleReportConstants {
    static final OWNER = "nerna"
    static final GROUP = "flylight"
    static final OWNER_KEY = "user:"+OWNER
    static final GROUP_KEY = "group:"+GROUP
    static final OUTPUT_HTML = true
	static final CREATE_FOLDERS = true
    static final COLOR_RETIRED = "aaf"
    static final COLOR_ACTIVE = "faa"
    static final OUTPUT_FILE = "/Users/rokickik/retired_prod." + (OUTPUT_HTML?"html":"txt")
	static final OUTPUT_ROOT_NAME = "Retired Duplicates"
}

def file = null
if (SampleReportConstants.OUTPUT_HTML) {
    file = new PrintWriter(SampleReportConstants.OUTPUT_FILE)
}
else {
    file = System.out
}

f = new JacsUtils(SampleReportConstants.OWNER_KEY, true)

Multimap<String, Entity> sampleMap = HashMultimap.<String,Entity>create();
Set<Long> retiredSampleSet = new HashSet<Long>()

addSamples(sampleMap, f.e.getUserEntitiesByTypeName(SampleReportConstants.OWNER_KEY, "Sample"))
addSamples(sampleMap, f.e.getUserEntitiesByTypeName(SampleReportConstants.GROUP_KEY, "Sample"))

addRetiredSamples(f, retiredSampleSet, f.getRootEntity(SampleReportConstants.OWNER_KEY, "Retired Data"))
addRetiredSamples(f, retiredSampleSet, f.getRootEntity(SampleReportConstants.GROUP_KEY, "Retired Data"))

List<String> keys = new ArrayList<String>(sampleMap.keySet())
Collections.sort(keys);

int numSlideCodes = 0
int numRetiredSamples = 0
int numStitchingErrors = 0
int numDuplications = 0

if (SampleReportConstants.OUTPUT_HTML) {
    file.println("<html><body><head><style>" +
            "td { font: 8pt sans-serif; vertical-align:top; border: 0px solid #aaa;} table { border-collapse: collapse; } " +
            "</style></head>")
    file.println("<h3>"+SampleReportConstants.OWNER+" Retired Samples</h3>")
    file.println("<table>")
    file.println("<tr><td>Owner</td><td>Sample Name</td><td>Data Set</td><td>Matching Active Sample</td><td>Fragments</td><td>Annotations</td></tr>")
}

List<Entity> samplesForDeletion = new ArrayList<Entity>();

def rootFolder = null
if (SampleReportConstants.CREATE_FOLDERS) {
	rootFolder = f.getRootEntity(SampleReportConstants.OUTPUT_ROOT_NAME)
    if (rootFolder!=null) {
        println "Deleting root folder "+SampleReportConstants.OUTPUT_ROOT_NAME+". This may take a while!"
        f.deleteEntityTreeById(rootFolder.id)
    }
	rootFolder = f.createRootEntity(SampleReportConstants.OUTPUT_ROOT_NAME)
}

int index = 0

for(String key : keys) {

    Collection<Entity> samples = sampleMap.get(key);

    boolean hasRetired = false;

    for(Entity sample : samples) {
        if (retiredSampleSet.contains(sample.id)) {
            hasRetired = true;
            break;
        }
    }

    if (hasRetired) {

        index++
        numSlideCodes++
		
		Entity keyFolder = null
		if (SampleReportConstants.CREATE_FOLDERS) {
			keyFolder = f.verifyOrCreateChildFolder(rootFolder, key)
		}
		
        if (SampleReportConstants.OUTPUT_HTML) {
            file.println("<tr><td colspan=6 style='background-color:#aaa'>"+key+"</td></tr>");
            println("Processing slide code "+key+" ("+index+")") // to see progress
        }
        else {
            file.println()
            file.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------")
            file.println(key)
            file.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------")
        }

        List<Entity> activeSamples = new ArrayList<Entity>()
        List<Entity> retiredSamples = new ArrayList<Entity>()
        for(Entity sample : samples) {
            if (retiredSampleSet.contains(sample.id)) {
                retiredSamples.add(sample);
            }
            else {
                activeSamples.add(sample);
            }
        }

        Multimap<Entity, Entity> transferMap = HashMultimap.<Entity,Entity>create();
        for(Entity retiredSample : retiredSamples) {
            for(Entity activeSample : activeSamples) {
                if (lsmSetsMatch(f, retiredSample, activeSample)) {
                    transferMap.put(retiredSample, activeSample);
                }
            }
        }

        List<Entity> orderedSamples = new ArrayList<Entity>();
        orderedSamples.addAll(retiredSamples);
        orderedSamples.addAll(activeSamples);

        Collections.sort(orderedSamples, new Comparator<Entity>() {

            public int compare(Entity o1, Entity o2) {
                def retired1 = retiredSampleSet.contains(o1.id)
                def retired2 = retiredSampleSet.contains(o2.id)
                if (retired1 && !retired2) {
                    return -1;
                }
                if (retired2 && !retired1) {
                    return 1;
                }
                int c = o1.ownerKey.compareTo(o2.ownerKey)
                if (c==0) {
                    return o1.name.compareTo(o2.name)
                }
                return c
            }
        })

        Set<String> situations = new HashSet<String>()

        for(Entity sample : orderedSamples) {

            f.loadChildren(sample)

            annotations = getAnnotations(f, sample.id)
            data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
            retired = (retiredSampleSet.contains(sample.id)?"Retired":"")

            SampleReportNeuronCounter counter = new SampleReportNeuronCounter(f.getEntityLoader())
            counter.count(sample)

            Set<Entity> transferSamples = transferMap.get(sample)
            StringBuilder transferSamplesSb = new StringBuilder()
            if (transferSamples!=null) {
                transferSamples.each {
                    if (transferSamplesSb.length()>0) transferSamplesSb.append(",")
                    transferSamplesSb.append(it.name)
                }
            }

            if (retired) {
                // Retired sample
                numRetiredSamples++
                if (sample.name.startsWith(transferSamplesSb.toString())) {
                    situations.add('duplication')
                }
            }
            else {
                // Active sample
                Set annotSet = new HashSet<String>(annotations)
                if (annotSet.contains("Stitching_error") || annotSet.contains("something_wrong")) {
                    situations.add('stitching')
                    samplesForDeletion.add(sample)
                    println("  Stitching error detected. Will delete this sample later.")
                }
            }

			if (SampleReportConstants.CREATE_FOLDERS) {
				f.addToParent(keyFolder, sample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
			}
			
            if (SampleReportConstants.OUTPUT_HTML) {
                def annots = annotations.toString()
                def color = (retired?SampleReportConstants.COLOR_RETIRED:SampleReportConstants.COLOR_ACTIVE)
                file.println("<tr><td>"+sample.ownerKey.replaceAll("group:","").replaceAll("user:","")+"</td>")
                file.println("<td style='background-color:#"+color+"'><nobr><b>"+sample.name+"</b></nobr></td>")
                file.println("<td>"+data_set+"</td>")
                if (transferSamplesSb.length()>0) {
                    file.println("<td style='background-color:#"+SampleReportConstants.COLOR_ACTIVE+"'><nobr><b>"+transferSamplesSb+"</b></nobr></td>")
                }
                else {
                    file.println("<td></td>");
                }
                file.println("<td>"+counter.numFragments+"</td>")
                file.println("<td>"+annots.substring(1,annots.length()-1)+"</td></tr>")
            }
            else {
                transfer = transferSamplesSb.length()>0?"--> "+transferSamplesSb:""
                file.println padRight(sample.ownerKey, 16) + padRight(sample.name, 65) + padRight(data_set, 40) + " " + transfer + " " + counter.numFragments + " "+ annotations

            }

            StringBuilder lsb = new StringBuilder();
            Entity supportingData = EntityUtils.getSupportingData(sample)
            f.loadChildren(supportingData)
            if (supportingData != null) {
                for(Entity imageTile : EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)) {

                    if (SampleReportConstants.OUTPUT_HTML) {
                        lsb.append("&nbsp&nbsp"+imageTile.name+"<br>")
                    }
                    else {
                        file.println "                  "+imageTile.name
                    }

                    f.loadChildren(imageTile)
                    for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                        if (SampleReportConstants.OUTPUT_HTML) {
                            lsb.append("&nbsp&nbsp&nbsp&nbsp"+lsm.name+"<br>")
                        }
                        else {
                            file.println "                      "+lsm.name
                        }
                    }
                }
            }

            if (SampleReportConstants.OUTPUT_HTML) {
                file.println("<tr><td></td><td>"+lsb+"</td><td colspan=4></td></tr>")
            }

            // free memory
            sample.setEntityData(null)
        }

        if (SampleReportConstants.OUTPUT_HTML) {
            def situation = ""
            def color = "fff"

            if (situations.contains("stitching") && situations.contains("duplication")) {
                color = "afa"
                situation = "Retired sample duplicated with stitching error";
                numStitchingErrors++
            }
            else if (situations.contains("duplication")) {
                color = "aff"
                situation = "Retired sample duplicated";
                numDuplications++
            }
            else {
                situation = situations.toString()
            }
            file.println("<tr><td colspan=6 style='background-color:#"+color+"; text-align:right'>"+situation+"</td></tr>")
            file.println("<tr><td height=40 colspan=6>&nbsp;</td></td>")
        }
    }
}

println("Deleting unwanted samples...")
for(Entity sample : samplesForDeletion) {
    println("Unlinking and deleting "+sample.name)
    f.e.deleteSmallEntityTree(sample.ownerKey, sample.id, true)
}

if (SampleReportConstants.OUTPUT_HTML) {
    file.println("</table>")
    file.println("<br>Slide codes: "+numSlideCodes)
    file.println("<br>Retired samples: "+numRetiredSamples)
    file.println("<br>Stitching errors: "+numStitchingErrors)
    file.println("<br>Duplications: "+numDuplications)
    file.println("</body></html>")
}
else {
    file.println("Slide codes: "+numSlideCodes)
    file.println("Retired samples: "+numRetiredSamples)
    file.println("Stitching errors: "+numStitchingErrors)
    file.println("Duplications: "+numDuplications)

}

file.close()


def padRight(String s, int n) {
    return String.format("%1\$-" + n + "s", s)
}

def padLeft(String s, int n) {
    return String.format("%1\$" + n + "s", s)
}

def addSamples(Multimap<String, Entity> sampleMap, Collection<Entity> samples) {
    for(Entity sample : samples) {
        SampleReportSampleInfo info = new SampleReportSampleInfo(sample)
        sampleMap.put(info.slide_code, sample);
    }
}

def addRetiredSamples(JacsUtils f, Set<Long> retiredSampleSet, Entity retiredSampleFolder) {
    f.loadChildren(retiredSampleFolder)
    for(Entity child : retiredSampleFolder.children) {
        retiredSampleSet.add(child.id)
    }
}

def getAnnotations(JacsUtils f, Long sampleId) {
    SolrQuery query = new SolrQuery("(id:"+sampleId+" OR ancestor_ids:"+sampleId+") AND all_annotations:*")
    SolrDocumentList results = f.s.search(null, query, false).response.results
    List<String> annotations = new ArrayList<String>()
    results.each {
        def all = it.getFieldValues(SampleReportConstants.OWNER+"_annotations")
        if (all!=null) annotations.addAll(all)
    }
    return annotations
}

def lsmSetsMatch(JacsUtils f, Entity sample1, Entity sample2) {
    Set<String> set1 = getLsmSet(f, sample1)
    Set<String> set2 = getLsmSet(f, sample2)
    return set1.containsAll(set2) && set2.containsAll(set1)
}

def getLsmSet(JacsUtils f, Entity sample) {
    Set<String> lsmSet = new HashSet<String>();
    f.loadChildren(sample)
    Entity supportingData = EntityUtils.getSupportingData(sample)
    f.loadChildren(supportingData)
    if (supportingData != null) {
        for(Entity imageTile : EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)) {
            f.loadChildren(imageTile)
            for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                lsmSet.add(lsm.name)
            }
        }
    }
    return lsmSet
}

class SampleReportSampleInfo {
    def sampleName = "";
    def line = "";
    def slide_code = "";
    def objective = "";

    SampleReportSampleInfo(Entity sample) {

        sampleName = sample.name;
        def parts = sampleName.split("~")

        if (parts.length>1) {
            sampleName = parts[0]
            objective = parts[1]
        }

        sampleName = sampleName.replaceFirst("-Retired", "")
        int startOfSlideCode = sampleName.lastIndexOf("-20");
        if (startOfSlideCode<0) {
            // Try to find typo'd slide codes
            startOfSlideCode = sampleName.lastIndexOf("-10");
        }

        if (startOfSlideCode>0) {
            line = sampleName.substring(0,startOfSlideCode)
            slide_code = sampleName.substring(startOfSlideCode+1)
            def matcher = (slide_code =~ /(\d{8}_\d+_\w{2}).*/)
            if (matcher.matches()) {
                slide_code = matcher[0][1]
            }
        }
        else {
            line = ""
            slide_code = sampleName
        }
    }
}

class SampleReportNeuronCounter {

    AbstractEntityLoader loader
    int numFragments;

    public SampleReportNeuronCounter(AbstractEntityLoader loader) {
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
