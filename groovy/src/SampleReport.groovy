import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.common.SolrDocument
import org.apache.solr.common.SolrDocumentList
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.StringUtils
import org.janelia.it.FlyWorkstation.api.entity_model.management.*
import org.janelia.it.jacs.shared.utils.entity.*

class Constants {
    static final OWNER = "nerna"
    static final GROUP = "flylight"
    static final OWNER_KEY = "user:"+OWNER
    static final GROUP_KEY = "group:"+GROUP
    static final HTML = true
    static final COLOR_RETIRED = "aaf"
    static final COLOR_ACTIVE = "faa"
    static final OUTPUT_FILE = "/Users/rokickik/retired." + (HTML?"html":"txt")
}

def file = null
if (Constants.HTML) {
    file = new PrintWriter(Constants.OUTPUT_FILE)
}
else {
    file = System.out
}

f = new JacsUtils(Constants.OWNER_KEY, false)

Multimap<String, Entity> sampleMap = HashMultimap.<String,Entity>create();

addSamples(sampleMap, f.e.getUserEntitiesByTypeName(Constants.OWNER_KEY, "Sample"));
addSamples(sampleMap, f.e.getUserEntitiesByTypeName(Constants.GROUP_KEY, "Sample"));

List<String> keys = new ArrayList<String>(sampleMap.keySet());
Collections.sort(keys);

int numSlideCodes = 0
int numRetiredSamples = 0

if (Constants.HTML) {
    file.println("<html><body><head><style>" +
            "td { font: 8pt sans-serif; vertical-align:top; border: 1px solid #aaa;} table { border-collapse: collapse; } " +
            "</style></head>")
    file.println("<h3>"+Constants.OWNER+" Retired Samples</h3>")
    file.println("<table>")
    file.println("<tr><td>Owner</td><td>Sample Name</td><td>Data Set</td><td>Matching Active Sample</td><td>Fragments</td><td>Annotations</td></tr>")
}

for(String key : keys) {

    Collection<Entity> samples = sampleMap.get(key);

    boolean unvisited = false;

    for(Entity sample : samples) {
        visited = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)
        if (StringUtils.isEmpty(visited)) {
            unvisited = true;
            break;
        }
    }

    if (unvisited) {

        numSlideCodes++

        if (Constants.HTML) {
            file.println("<tr><td colspan=6 style='background-color:#aaa'>"+key+"</td></tr>");
            println(key) // to see progress
        }
        else {
            file.println()
            file.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------")
            file.println(key)
            file.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------")
        }


        List<Entity> activeSamples = new ArrayList<Entity>();
        List<Entity> retiredSamples = new ArrayList<Entity>();
        for(Entity sample : samples) {
            visited = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)
            if (visited==null) {
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
            int compare(Entity o1, Entity o2) {
                def visited1 = o1.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)
                def visited2 = o2.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)
                if (visited1==null && visited2!=null) {
                    return -1;
                }
                if (visited2==null && visited1!=null) {
                    return 1;
                }
                int c = o1.ownerKey.compareTo(o2.ownerKey)
                if (c==0) {
                    return o1.name.compareTo(o2.name)
                }
                return c
            }
        })

        for(Entity sample : orderedSamples) {

            SampleInfo info = new SampleInfo(sample)
            annotations = getAnnotations(sample.id)
            visited = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_VISITED)
            data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
            retired = (visited==null?"Retired":"")

            if (visited==null) numRetiredSamples++

            Set<Entity> transferSamples = transferMap.get(sample)
            StringBuilder sb = new StringBuilder()

            if (transferSamples!=null) {
                transferSamples.each {
                    if (sb.length()>0) sb.append(",")
                    sb.append(it.name)
                }
            }

            f.loadChildren(sample)

            NeuronCounter counter = new NeuronCounter(f.getEntityLoader())
            counter.count(sample)

            Set annotSet = new HashSet<String>(annotations)
            if (annotSet.contains("Stitching_error")) {
    
            }


            if (Constants.HTML) {
                def annots = annotations.toString()
                def color = (visited==null?Constants.COLOR_RETIRED:Constants.COLOR_ACTIVE)
                file.println("<tr><td>"+sample.ownerKey.replaceAll("group:","").replaceAll("user:","")+"</td>")
                file.println("<td style='background-color:#"+color+"'><nobr><b>"+sample.name+"</b></nobr></td>")
                file.println("<td>"+data_set+"</td>")
                if (sb.length()>0) {
                    file.println("<td style='background-color:#"+Constants.COLOR_ACTIVE+"'><nobr><b>"+sb+"</b></nobr></td>")
                }
                else {
                    file.println("<td></td>");
                }
                file.println("<td>"+counter.numFragments+"</td>")
                file.println("<td>"+annots.substring(1,annots.length()-1)+"</td></tr>")
            }
            else {
                transfer = sb.length()>0?"--> "+sb:""
                file.println padRight(sample.ownerKey, 16) + padRight(sample.name, 65) + padRight(data_set, 40) + " " + transfer + " " + counter.numFragments + " "+ annotations

            }

            StringBuilder lsb = new StringBuilder();
            Entity supportingData = EntityUtils.getSupportingData(sample)
            f.loadChildren(supportingData)
            if (supportingData != null) {
                for(Entity imageTile : EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)) {

                    if (Constants.HTML) {
                        lsb.append("&nbsp&nbsp"+imageTile.name+"<br>")
                    }
                    else {
                        file.println "                  "+imageTile.name
                    }

                    f.loadChildren(imageTile)
                    for(Entity lsm : EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)) {
                        if (Constants.HTML) {
                            lsb.append("&nbsp&nbsp&nbsp&nbsp"+lsm.name+"<br>")
                        }
                        else {
                            file.println "                      "+lsm.name
                        }
                    }
                }
            }

            if (Constants.HTML) {
                file.println("<tr><td></td><td>"+lsb+"</td><td colspan=4></td></tr>")
            }

            // free memory
            sample.setEntityData(null)

        }
    }
}

if (Constants.HTML) {
    file.println("</table>");
    file.println("<br>Slide codes: "+numSlideCodes)
    file.println("<br>Samples codes: "+numSlideCodes)
    file.println("</body></html>");
}
else {
    file.println("Slide codes: "+numSlideCodes)
    file.println("Samples: "+numRetiredSamples)
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
        SampleInfo info = new SampleInfo(sample)
        sampleMap.put(info.slide_code, sample);
    }
}

def getAnnotations(sampleId) {
    SolrQuery query = new SolrQuery("(id:"+sampleId+" OR ancestor_ids:"+sampleId+") AND all_annotations:*")
    SolrDocumentList results = f.s.search(null, query, false).response.results
    List<String> annotations = new ArrayList<String>()
    results.each {
        def all = it.getFieldValues(Constants.OWNER+"_annotations")
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

class SampleInfo {
    def sampleName = "";
    def line = "";
    def slide_code = "";
    def objective = "";

    SampleInfo(Entity sample) {

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

class NeuronCounter {

    AbstractEntityLoader loader
    int numFragments;

    public NeuronCounter(AbstractEntityLoader loader) {
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
