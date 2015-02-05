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
	static final WRITE_DATABASE = false
    static final COLOR_RETIRED = "FAA755"
    static final COLOR_ACTIVE = "67CF55"
    static final OUTPUT_FILE = "/Users/rokickik/retired.html"
	static final OUTPUT_ROOT_NAME = "Retired Duplicates"
}

def file = new PrintWriter(SampleReportConstants.OUTPUT_FILE)
f = new JacsUtils(SampleReportConstants.OWNER_KEY, true)

Multimap<String, Entity> sampleMap = HashMultimap.<String,Entity>create();
Set<Long> retiredSampleSet = new HashSet<Long>()

println "Adding owned samples"
addSamples(sampleMap, f.e.getUserEntitiesByTypeName(SampleReportConstants.OWNER_KEY, "Sample"))
println "Adding group samples"
addSamples(sampleMap, f.e.getUserEntitiesByTypeName(SampleReportConstants.GROUP_KEY, "Sample"))

println "Adding owned retired samples"
addRetiredSamples(f, retiredSampleSet, f.getRootEntity(SampleReportConstants.OWNER_KEY, "Retired Data"))
println "Adding group retired samples"
addRetiredSamples(f, retiredSampleSet, f.getRootEntity(SampleReportConstants.GROUP_KEY, "Retired Data"))

println "Generating report..."

List<String> keys = new ArrayList<String>(sampleMap.keySet())
Collections.sort(keys);

int numSlideCodes = 0
int numRetiredSamples = 0
int numActiveSamples = 0

file.println("<html><body><head><style>" +
        "td { font: 8pt sans-serif; vertical-align:top; border: 0px solid #aaa;} table { border-collapse: collapse; } " +
        "</style></head>")
file.println("<h3>"+SampleReportConstants.OWNER+" Retired Samples</h3>")
file.println("<table>")
file.println("<tr><td>Owner</td><td>Sample Name</td><td>Data Set</td><td>Matching Active Sample</td><td>Total Fragments</td><td>Referenced Fragments</td><td>Annotations</td></tr>")
numExtraCols = 5 // Number of columns besides owner and sample name

List<Entity> samplesForDeletion = new ArrayList<Entity>();

Entity rootFolder = null
if (SampleReportConstants.WRITE_DATABASE) {
	rootFolder = f.getRootEntity(SampleReportConstants.OUTPUT_ROOT_NAME)
    if (rootFolder!=null) {
        println "Deleting root folder "+SampleReportConstants.OUTPUT_ROOT_NAME+". This may take a while!"
        f.deleteEntityTree(rootFolder.id)
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
		if (SampleReportConstants.WRITE_DATABASE) {
			keyFolder = f.verifyOrCreateChildFolder(rootFolder, key)
		}
		
        file.println("<tr><td colspan="+(2+numExtraCols)+" style='background-color:#aaa'>"+key+"</td></tr>");
        println("Processing slide code "+key+" ("+index+")") // to see progress

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
        int numTargets = transferMap.size()
        int numTargetsAnnotation = 0

        for(Entity sample : orderedSamples) {

            f.loadChildren(sample)

            List<String> annotations = getAnnotations(f, sample.id)
            String data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)

            SampleReportNeuronCounter counter = new SampleReportNeuronCounter(f)
            counter.count(sample)

            int numAnnotationOnTransferSamples = 0
            
            Set<Entity> transferSamples = transferMap.get(sample)
            StringBuilder transferNamesSb = new StringBuilder()
            StringBuilder transferSamplesSb = new StringBuilder()
            if (transferSamples!=null) {
                transferSamples.each {
                    List<String> activeAnnotations = getAnnotations(f, it.id)
                    numAnnotationOnTransferSamples += activeAnnotations.size()
                    if (transferSamplesSb.length()>0) transferSamplesSb.append(",")
                    transferSamplesSb.append(it.name)
                }
            }
            
            boolean retired = retiredSampleSet.contains(sample.id);

            if (retired) {
                // Retired sample
                numRetiredSamples++
                if (numAnnotationOnTransferSamples==0 && (!annotations.isEmpty() || counter.numWithRefs>0)) {
                    situations.add("can_migrate")
                }
                else if (numAnnotationOnTransferSamples>0) {
                    numTargetsAnnotation++
                }
            }
            else {
                // Active sample
                numActiveSamples++
            }

			if (SampleReportConstants.WRITE_DATABASE) {
				f.addToParent(keyFolder, sample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
			}
            
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
            file.println("<td style='text-align:right'>"+counter.numFragments+"</td>")
            
            def refsColor = ((retired && counter.numWithRefs>0)?SampleReportConstants.COLOR_RETIRED:"white")
            file.println("<td style='background-color:#"+refsColor+"; text-align:right'>"+counter.numWithRefs+"</td>")
            file.println("<td>"+annots.substring(1,annots.length()-1)+"</td></tr>")

            StringBuilder lsb = new StringBuilder();
            Entity supportingData = EntityUtils.getSupportingData(sample)
            f.loadChildren(supportingData)
            if (supportingData != null) {
                List<Entity> tiles = EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY);
                for(Entity imageTile : tiles) {

                    lsb.append("&nbsp&nbsp"+imageTile.name+"<br>")

                    f.loadChildren(imageTile)
                    for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                        lsb.append("&nbsp&nbsp&nbsp&nbsp"+lsm.name+"<br>")
                    }
                }
                if (tiles.size()>2) {
                    situations.add("duplsms");
                }
            }

            file.println("<tr><td></td><td>"+lsb+"</td><td colspan="+numExtraCols+"></td></tr>")

            // free memory
            sample.setEntityData(null)
        }

        def situation = ""
        def color = "D8CED9"
        
        if (situations.contains("available_annotations")) {
            situation += "Migration opportunity. ";
        }
        
        if (numTargetsAnnotation>=numTargets) {
            situation += "Target is already annotated. ";
        }
        
        if (situations.contains("duplsms")) {
            color = "F7ABAB"
            situation += "Active Duplicate LSMs detected. ";
        }
        
        if (situation.equals("")) {
            situation = "&nbsp;";
        }
        
        file.println("<tr><td colspan="+(2+numExtraCols)+" style='background-color:#"+color+"; text-align:right'>"+situation+"</td></tr>")
        file.println("<tr><td height=40 colspan="+(2+numExtraCols)+">&nbsp;</td></td>")
    }
}

if (SampleReportConstants.WRITE_DATABASE) {
	println("Deleting unwanted samples...")
	for(Entity sample : samplesForDeletion) {
	    println("Unlinking and deleting "+sample.name)
	    f.e.deleteEntityTreeById(sample.ownerKey, sample.id, true)
	}
}

file.println("</table>")
file.println("<br>Slide codes: "+numSlideCodes)
file.println("<br>Active samples: "+numActiveSamples)
file.println("<br>Retired samples: "+numRetiredSamples)
file.println("</body></html>")

file.close()

def removeSuffix(String name) {
    return name.replaceFirst("-Retired", "").replaceFirst("-Left_Optic_Lobe", "").replaceFirst("-Right_Optic_Lobe", "").replaceFirst("-Optic_Central_Border", "")
    
}

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
    Set<String> set1 = getLsmSet(f, sample1);
    Set<String> set1r = new HashSet<String>();
    for(String s : set1) {
        set1r.add(s.replaceAll(".bz2", ""));
    }
    
    Set<String> set2 = getLsmSet(f, sample2);
    Set<String> set2r = new HashSet<String>();
    for(String s : set2) {
        set2r.add(s.replaceAll(".bz2", ""));
    }
    
    return (set1r.containsAll(set2r) && set2r.containsAll(set1r));
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
    String sampleName = "";
    String line = "";
    String slide_code = "";
    String objective = "";

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
            if (startOfSlideCode>0) {
                println("Detected typo in slide code: "+sampleName)
            }
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

    JacsUtils f
    int numFragments;
    int numWithRefs;

    public SampleReportNeuronCounter(JacsUtils f) {
        this.f = f
        this.numFragments = 0
        this.numWithRefs = 0
    }

    def count(Entity sample) {
        EntityVistationBuilder.create(f.getEntityLoader()).startAt(sample)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
                .childrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).last()
                .childOfName("Neuron Fragments")
                .childrenOfType(EntityConstants.TYPE_NEURON_FRAGMENT)
                .run(new EntityVisitor() {
            public void visit(Entity fragment) throws Exception {
                numFragments++;
                numWithRefs += f.e.getParentEntities(null, fragment.id).size()>1?1:0;
            }
        });
    }
}
