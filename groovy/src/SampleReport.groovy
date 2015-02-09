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

class SampleReportScript {
 
    private static final String OWNER = "nerna"
    private static final String GROUP = "flylight"
    private static final String OWNER_KEY = "user:"+OWNER
    private static final String GROUP_KEY = "group:"+GROUP
	private static final boolean WRITE_DATABASE = false
    private static final String COLOR_RETIRED = "FAA755"
    private static final String COLOR_ACTIVE = "67CF55"
    private static final String OUTPUT_FILE = "/Users/rokickik/retired.html"
	private static final String OUTPUT_ROOT_NAME = "Retired Duplicates"
    private static final int NUM_SAMPLE_COLS = 3
    private JacsUtils f
    
    public SampleReportScript() {
        this.f = new JacsUtils(OWNER_KEY, WRITE_DATABASE)
    }
    
    public void run() {
        def file = new PrintWriter(OUTPUT_FILE)
        
        Multimap<String, Entity> sampleMap = HashMultimap.<String,Entity>create();
        Set<Long> retiredSampleSet = new HashSet<Long>()
        
        println "Adding owned samples"
        addSamples(sampleMap, f.e.getUserEntitiesByTypeName(OWNER_KEY, "Sample"))
        println "Adding group samples"
        addSamples(sampleMap, f.e.getUserEntitiesByTypeName(GROUP_KEY, "Sample"))
        
        println "Adding owned retired samples"
        addRetiredSamples(f, retiredSampleSet, f.getRootEntity(OWNER_KEY, "Retired Data"))
        println "Adding group retired samples"
        addRetiredSamples(f, retiredSampleSet, f.getRootEntity(GROUP_KEY, "Retired Data"))
        
        println "Generating report..."
        
        List<String> keys = new ArrayList<String>(sampleMap.keySet())
        Collections.sort(keys);
        
        int numSlideCodes = 0
        int numRetiredSamples = 0
        int numActiveSamples = 0
        
        file.println("<html><body><head><style>" +
                "td { font: 8pt sans-serif; vertical-align:top; border: 0px solid #aaa;} table { border-collapse: collapse; } " +
                "</style></head>")
        file.println("<h3>"+OWNER+" Retired Samples</h3>")
        file.println("<table>")
        file.println("<tr><td>Retired Sample</td><td>Neurons</td><td>Annotations</td><td>Active Sample</td><td>Neurons</td><td>Annotations</td></tr>")
        
        List<Entity> samplesForDeletion = new ArrayList<Entity>();
        
        Entity rootFolder = null
        if (WRITE_DATABASE) {
        	rootFolder = f.getRootEntity(OUTPUT_ROOT_NAME)
            if (rootFolder!=null) {
                println "Deleting root folder "+OUTPUT_ROOT_NAME+". This may take a while!"
                f.deleteEntityTree(rootFolder.id)
            }
        	rootFolder = f.createRootEntity(OUTPUT_ROOT_NAME)
        }
        
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
        
                numSlideCodes++
        		
                Entity keyFolder = null
        		if (WRITE_DATABASE) {
        			keyFolder = f.verifyOrCreateChildFolder(rootFolder, key)
        		}
        		
                file.println("<tr><td colspan="+(NUM_SAMPLE_COLS*2)+" style='background-color:#aaa; font-size:10pt; padding: 5px;'>"+key+"</td></tr>");
                println("Processing slide code "+key+" ("+numSlideCodes+")") // to see progress
        
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
                
                numRetiredSamples += retiredSamples.size()
                numActiveSamples += activeSamples.size()
        
                Multimap<Entity, Entity> transferMap = HashMultimap.<Entity,Entity>create();
                for(Entity retiredSample : retiredSamples) {
                    for(Entity activeSample : activeSamples) {
                        if (lsmSetsMatch(f, retiredSample, activeSample)) {
                            transferMap.put(retiredSample, activeSample);
                        }
                    }
                }
        
                Collections.sort(retiredSamples, new Comparator<Entity>() {
                    public int compare(Entity o1, Entity o2) {
                        int c = o1.ownerKey.compareTo(o2.ownerKey)
                        if (c==0) {
                            return o1.name.compareTo(o2.name)
                        }
                        return c
                    }
                })
        
                for(Entity retiredSample : retiredSamples) {
                    
                    // Get corresponding active sample
                    Set<Entity> transferSamples = transferMap.get(retiredSample)
                    if (transferSamples.size()>1) {
                        println "WARNING: more than one matching transfer sample for "+retiredSample.name
                    }
                    
                    Entity activeSample = null
                    
                    if (!transferSamples.isEmpty()) {
                        activeSample = transferSamples.iterator().next()
                        // Remove it from the list of active samples so that it doesn't get printed by itself later on
                        activeSamples.remove(activeSample)
                    }
                    
                    List<String> retiredCols = getSampleCols(f, retiredSample, true);
                    List<String> activeCols = getSampleCols(f, activeSample, false);
                    
                    int i = 0;
                    
                    file.println(getBorderRow())
                    for(String retiredCol : retiredCols) {
                        String activeCol = activeCols==null?null:activeCols.get(i)
                        file.println("<tr>");
                        file.println(retiredCol);
                        if (activeCol==null) {
                            file.println(getBlankCols());
                        }
                        else {
                            file.println(activeCol);
                        }
                        file.println("</tr>");
                        i++
                    }
                    
                    if (WRITE_DATABASE) {
                        f.addToParent(keyFolder, retiredSample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                        if (activeSample!=null) {
                            f.addToParent(keyFolder, activeSample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                        }
                    }
                        
                    // free memory
                    retiredSample.setEntityData(null)
                    if (activeSample!=null) {
                        activeSample.setEntityData(null)
                    }
                    
                }
                
                for(Entity activeSample : activeSamples) {
                    
                    file.println(getBorderRow())
                    for(String activeCol : getSampleCols(f, activeSample, false)) {
                        file.println("<tr>");
                        file.println(getBlankCols())
                        file.println(activeCol)
                        file.println("</tr>");
                    }
                    
                    if (WRITE_DATABASE) {
                        f.addToParent(keyFolder, activeSample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                    }
                    
                    // free memory
                    activeSample.setEntityData(null)
                }
                
                file.println(getBreakRow())
            }
        }
        
        if (WRITE_DATABASE) {
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
    }
    
    def getBreakRow() {
        return "<tr><td height=40 colspan="+(NUM_SAMPLE_COLS*2)+">&nbsp;</td></td>";
    }
    
    def getBorderRow() {
        return "<tr><td height=10 colspan="+(NUM_SAMPLE_COLS*2)+" style='border-top: 1px solid black'>&nbsp;</td></tr>"
    }
    
    def getBlankCols() {
        return "<td colspan="+NUM_SAMPLE_COLS+"></td>"
    }
        
    def getSampleCols(JacsUtils f, Entity sample, boolean retired) {
        
        if (sample==null) return null;
        f.loadChildren(sample)
    
        List<String> annotations = getAnnotations(f, sample.id)
        String data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
    
        SampleReportNeuronCounter counter = new SampleReportNeuronCounter(f)
        counter.count(sample)
        
        StringBuilder sb = new StringBuilder()
        def annots = annotations.toString()
        def color = (retired?COLOR_RETIRED:COLOR_ACTIVE)
        sb.append("<td style='background-color:#"+color+"'><nobr><b>"+sample.name+"</b></nobr></td>")
        sb.append("<td rowspan=2 style='text-align:right; padding: 0 1em 0 1em'>"+counter.numWithRefs+" /<br>"+counter.numFragments+"</td>")
        sb.append("<td rowspan=2>"+annots.substring(1,annots.length()-1)+"</td>")
    
        StringBuilder lsb = new StringBuilder("<td>")
        Entity supportingData = EntityUtils.getSupportingData(sample)
        f.loadChildren(supportingData)
        if (supportingData != null) {
            List<Entity> tiles = EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY);
            for(Entity imageTile : tiles) {
                f.loadChildren(imageTile)
                List<Entity> lsms = EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)
                if (lsms.size()>2) {
                    lsb.append("<span style='text-color:red'>")
                }
                lsb.append("&nbsp&nbsp"+imageTile.name+"<br>")
                for(Entity lsm : lsms) {
                    lsb.append("&nbsp&nbsp&nbsp&nbsp"+lsm.name+"<br>")
                }
                if (lsms.size()>2) {
                    lsb.append("</span>")
                }
            }
        }
        lsb.append("</td>")
        
        List<String> rows = new ArrayList()
        rows.add(sb.toString())
        rows.add(lsb.toString())
        
        return rows
    }
    
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
            def all = it.getFieldValues(OWNER+"_annotations")
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
}
    
SampleReportScript script = new SampleReportScript()
script.run()