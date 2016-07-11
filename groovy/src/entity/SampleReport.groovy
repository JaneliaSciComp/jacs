package entity

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.ontology.OntologyAnnotation

class SampleReportScript {
 
    private static final String OWNER = "nerna"
    private static final String GROUP = "flylight"
    private static final String OWNER_KEY = "user:"+OWNER
    private static final String GROUP_KEY = "group:"+GROUP
	private static final boolean WRITE_DATABASE = false
    private static final boolean MIGRATE_NEURONS = false
    private static final boolean DELETE_SAMPLES = false
    private static final String COLOR_RETIRED = "FAA755"
    private static final String COLOR_ACTIVE = "67CF55"
    private static final String OUTPUT_FILE = "/Users/rokickik/retired.html"
	private static final String MANUAL_OUTPUT_ROOT_NAME = "Retired Duplicates (Manual)"
    private static final String AUTO_OUTPUT_ROOT_NAME = "Retired Duplicates (Auto)"
    private static final String AUTO_MIGRATION_TERM_NAME = "Fragments_migrated"
    private static final int NUM_SAMPLE_COLS = 3
    private JacsUtils f
    private Entity autoMigratedTerm;
    
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
        
        for(Entity entity : f.e.getEntitiesByNameAndTypeName(OWNER_KEY, AUTO_MIGRATION_TERM_NAME, EntityConstants.TYPE_ONTOLOGY_ELEMENT)) {
            this.autoMigratedTerm = entity 
        }
        
        if (autoMigratedTerm==null) {
            throw new IllegalStateException("Auto migration ontology term does not exist: "+AUTO_MIGRATION_TERM_NAME)
        }
        
        List<String> keys = new ArrayList<String>(sampleMap.keySet())
        Collections.sort(keys);
        
        int numSlideCodes = 0
        int numRetiredSamples = 0
        int numActiveSamples = 0
        int numMigratedSamples = 0
        int numManualSamples = 0
        
        file.println("<html><body><head><style>" +
                "td { font: 8pt sans-serif; vertical-align:top; border: 0px solid #aaa;} table { border-collapse: collapse; } " +
                "</style></head>")
        file.println("<h3>"+OWNER+" Retired Samples</h3>")
        file.println("<table>")
        file.println("<tr><td>Retired Sample</td><td>Neurons</td><td>Annotations</td><td>Active Sample</td><td>Neurons</td><td>Annotations</td></tr>")
        
        List<Entity> samplesForDeletion = new ArrayList<Entity>();
        
        Entity manualRootFolder = null
        Entity autoRootFolder = null
        if (WRITE_DATABASE) {
        	manualRootFolder = f.createRootEntity(MANUAL_OUTPUT_ROOT_NAME)
            autoRootFolder = f.createRootEntity(AUTO_OUTPUT_ROOT_NAME)
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
        
                boolean matchAll = true;
                Multimap<Entity, Entity> transferMap = HashMultimap.<Entity,Entity>create();
                for(Entity retiredSample : retiredSamples) {
                    boolean match = false;
                    for(Entity activeSample : activeSamples) {
                        if (lsmSetsMatch(f, retiredSample, activeSample)) {
                            transferMap.put(retiredSample, activeSample);
                            match = true;
                        }
                    }
                    if (!match) {
                        matchAll = false;
                    }
                }
                
                // LSM sets didn't match for every retired sample, let's try sample names
                if (!matchAll) {
                    for(Entity retiredSample : retiredSamples) {
                        if (transferMap.get(retiredSample).isEmpty()) {
                            for(Entity activeSample : activeSamples) {
                                if (retiredSample.name.replaceAll("-Retired", "").equals(activeSample.name)) {
                                    transferMap.put(retiredSample, activeSample);
                                }
                            }
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
                    
                    LatestNeuronWalker retiredWalker = getLatestNeuronWalker(f, retiredSample)
                    LatestNeuronWalker activeWalker = getLatestNeuronWalker(f, activeSample)
                    
                    Entity keyFolder = null
                    
                    Boolean autoMigrated = null;
                    if (retiredWalker!=null && activeWalker!=null && retiredWalker.numFragments==activeWalker.numFragments) {
                        if (WRITE_DATABASE) {
                            keyFolder = f.verifyOrCreateChildFolder(autoRootFolder, key)
                        }
                        autoMigrated = migrateNeurons(f, retiredWalker, activeWalker, keyFolder)
                        if (autoMigrated) {
                            if (WRITE_DATABASE && MIGRATE_NEURONS) {
                                OntologyAnnotation annotation = new OntologyAnnotation(null, retiredSample.id, autoMigratedTerm.id, autoMigratedTerm.name, null, null);
                                f.a.createOntologyAnnotation(OWNER_KEY, annotation)
                            }
                        }
                    }
                    
                    if (autoMigrated) {
                        numMigratedSamples++
                    }
                    else {
                        numManualSamples++
                    }
                    
                    if (WRITE_DATABASE) {
                        if (keyFolder==null) {
                            keyFolder = f.verifyOrCreateChildFolder(manualRootFolder, key)
                        }
                        f.addToParent(keyFolder, retiredSample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                        if (activeSample!=null) {
                            f.addToParent(keyFolder, activeSample, keyFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                        }
                    }
                    
                    String bgColor = autoMigrated==null?"#F5D282":(autoMigrated?"#ADEDAD":"#FCB6B6")
                    List<String> retiredCols = getSampleCols(f, retiredSample, retiredWalker, true, bgColor);
                    List<String> activeCols = getSampleCols(f, activeSample, activeWalker, false, bgColor);
                    
                    int i = 0;
                    file.println(getBorderRow())
                    for(String retiredCol : retiredCols) {
                        String activeCol = activeCols==null?null:activeCols.get(i)
                        file.println("<tr>");
                        file.println(retiredCol);
                        if (activeCol==null) {
                            file.println(getBlankCols(bgColor));
                        }
                        else {
                            file.println(activeCol);
                        }
                        file.println("</tr>");
                        i++
                    }
                    
                    if (autoMigrated!=null) {
                        if (autoMigrated) {
                            file.println("<tr><td style='background-color:"+bgColor+"; text-align:right' colspan="+(NUM_SAMPLE_COLS*2)+">Auto-migration succeeded</td></td>");
                        }
                        else {
                            file.println("<tr><td style='background-color:"+bgColor+"; text-align:right; text-color:red; font-weight: bold;' colspan="+(NUM_SAMPLE_COLS*2)+">Auto-migration FAILED</td></td>");
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
                    LatestNeuronWalker activeWalker = getLatestNeuronWalker(f, activeSample)
                    for(String activeCol : getSampleCols(f, activeSample, activeWalker, false, "#FFFFFF")) {
                        file.println("<tr>");
                        file.println(getBlankCols())
                        file.println(activeCol)
                        file.println("</tr>");
                    }
                    
                    // free memory
                    activeSample.setEntityData(null)
                }
                
                file.println(getBreakRow())
            }
        }
        
        if (WRITE_DATABASE && DELETE_SAMPLES) {
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
        file.println("<br>Auto-migrated samples: "+numMigratedSamples)
        file.println("<br>Manual-migration samples: "+numManualSamples)
        file.println("</body></html>")
        
        file.close()
        
        println "Done"
    }
    
    def getBreakRow() {
        return "<tr><td height=40 colspan="+(NUM_SAMPLE_COLS*2)+">&nbsp;</td></td>";
    }
    
    def getBorderRow() {
        return "<tr><td height=10 colspan="+(NUM_SAMPLE_COLS*2)+" style='border-top: 1px solid black'>&nbsp;</td></tr>"
    }
    
    def getBlankCols(String bgColor) {
        return "<td colspan="+NUM_SAMPLE_COLS+" style='background-color:"+bgColor+";'></td>"
    }
        
    def getLatestNeuronWalker(JacsUtils f, Entity sample) {
        if (sample==null) return null;
        LatestNeuronWalker walker = new LatestNeuronWalker(f)
        walker.walk(sample)
        if (walker.neuronFragments==null) return null
        return walker
    }
    
    def getSampleCols(JacsUtils f, Entity sample, LatestNeuronWalker walker, boolean retired, String bgColor) {
        if (sample==null) return null;
        
        String annots = "";
        if (walker!=null) {
            List<String> annotations = walker.getAnnotationNames()
            annots = annotations.toString()
            annots = annots.substring(1,annots.length()-1) 
        }
        
        String data_set = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DATA_SET_IDENTIFIER)
        
        StringBuilder sb = new StringBuilder()
        
        def color = (retired?COLOR_RETIRED:COLOR_ACTIVE)
        sb.append("<td style='background-color:"+bgColor+";'><nobr><b>"+sample.name+"</b></nobr></td>")
        sb.append("<td rowspan=2 style='background-color:"+bgColor+"; text-align:right; padding: 0 1em 0 1em;'>")
        if (walker!=null) {
            sb.append(walker.numWithRefs+" /<br>"+walker.numFragments);
        }
        sb.append("</td>")
        sb.append("<td rowspan=2 style='background-color:"+bgColor+";'>"+annots+"</td>")
    
        StringBuilder lsb = new StringBuilder("<td style='background-color:"+bgColor+";'>")
        Entity supportingData = EntityUtils.getSupportingData(sample)
        f.loadChildren(supportingData)
        if (supportingData != null) {
            List<Entity> tiles = EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY);
            for(Entity imageTile : tiles) {
                f.loadChildren(imageTile)
                List<Entity> lsms = EntityUtils.getChildrenForAttribute(imageTile, EntityConstants.ATTRIBUTE_ENTITY)
                if (lsms.size()>2) {
                    lsb.append("<span style='text-color:red; font-weight: bold;'>")
                }
                lsb.append("&nbsp;&nbsp;"+imageTile.name+"<br>")
                for(Entity lsm : lsms) {
                    lsb.append("&nbsp;&nbsp;&nbsp;&nbsp;"+lsm.name+"<br>")
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
    
    def migrateNeurons(JacsUtils f, LatestNeuronWalker retiredWalker, LatestNeuronWalker activeWalker, Entity keyFolder) {
        
        Entity migrationFolder = null
        
        boolean success = true
        println "Auto-migration for "+retiredWalker.sample.name
        
        Entity retiredFragments = retiredWalker.neuronFragments
        Entity activeFragments = activeWalker.neuronFragments
        
        List<Entity> targets = activeFragments.getOrderedChildren()
        
        int numMigratedAnnots = 0
        int numMigratedRefs = 0
            
        int i = 0
        for(EntityData ed : retiredFragments.getOrderedEntityData()) {
            
            Entity fragment = ed.childEntity
            Entity target = targets.get(i)
            
            boolean hasAnnots = false;
            
            Set<String> existingAnnotNames = new HashSet<>() 
            for(final Entity annotation : activeWalker.annotationMap.get(target.id)) {
                hasAnnots = true
                existingAnnotNames.add(annotation.name)
            }
            
            for(final Entity annotation : retiredWalker.annotationMap.get(fragment.id)) {
                hasAnnots = true
                // migrate annotation to point to new target
                String message;
                try {
                    if (!existingAnnotNames.contains(annotation.name)) {
                        if (WRITE_DATABASE && MIGRATE_NEURONS) f.e.setOrUpdateValue(annotation, EntityConstants.ATTRIBUTE_ANNOTATION_TARGET_ID, target.getId().toString());
                        //println "  Migrated annotation "+annotation.getName();
                        numMigratedAnnots++
                    }
                }
                catch (Exception e) {
                    println "  Error migrating annotation: "+e.message;
                    success = false
                }
            }
    
            for(final EntityData refEd : retiredWalker.parentEdMap.get(fragment.id)) {
                String refType = refEd.getParentEntity().getEntityTypeName();
                if (!refType.equals(EntityConstants.TYPE_FOLDER)) {
                    continue;
                }
                // migrate reference to point to a new target
                String message;
                try {
                    refEd.setChildEntity(target);
                    if (WRITE_DATABASE && MIGRATE_NEURONS) f.e.saveOrUpdateEntityData(refEd);
                    //println "  Migrated reference "+refEd.getParentEntity().getName();
                    numMigratedRefs++
                }
                catch (Exception e) {
                    println "  Error migrating reference: "+e.message;
                    success = false
                }
            }
            
            if (hasAnnots && WRITE_DATABASE) {
                if (migrationFolder==null) {
                    migrationFolder = f.verifyOrCreateChildFolder(keyFolder, retiredWalker.sample.name+" (Migration)")
                }
                f.addToParent(migrationFolder, fragment, migrationFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
                f.addToParent(migrationFolder, target, migrationFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
            }
            
            i++
        }
        
        println "  Migrated "+numMigratedAnnots+" annotations and "+numMigratedRefs+" references. Success="+success
        return success
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
    
    class LatestNeuronWalker {
    
        JacsUtils f
        Entity sample;
        Entity neuronFragments;
        Map<Long,Set<EntityData>> parentEdMap;
        Map<Long,List<Entity>> annotationMap;
        int numFragments;
        int numWithRefs;
        int numAnnotations;
    
        public LatestNeuronWalker(JacsUtils f) {
            this.f = f
        }
    
        def walk(Entity sample) {
            this.sample = sample
            this.neuronFragments = null
            this.numFragments = 0
            this.numWithRefs = 0
            this.numAnnotations = 0
            this.parentEdMap = new HashMap<>()
            this.annotationMap = new HashMap<>()
            EntityVistationBuilder.create(f.getEntityLoader()).startAt(sample)
                    .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN).last()
                    .childrenOfType(EntityConstants.TYPE_SAMPLE_PROCESSING_RESULT).last()
                    .childrenOfType(EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT).last()
                    .childOfName("Neuron Fragments")
                    .run(new EntityVisitor() {
                public void visit(Entity entity) throws Exception {
                    neuronFragments = entity
                }
            });
            if (neuronFragments!=null) {
                f.loadChildren(neuronFragments)
                for(Entity fragment : neuronFragments.getChildren()) {
                    numFragments++;
                    Set<EntityData> parentEds = f.e.getParentEntityDatas(null, fragment.id)
                    parentEdMap.put(fragment.id, parentEds)
                    numWithRefs += parentEds.size()>1?1:0;
                    List<Entity> annotations = f.a.getAnnotationsForEntity(null, fragment.id)
                    annotationMap.put(fragment.id, annotations)
                    numAnnotations += annotations.size()
                }
            }
        }
        
        def getAnnotationNames() {
            List<String> annotNames = new ArrayList<>()
            for(Entity fragment : neuronFragments.getChildren()) {
                for(Entity annotation : annotationMap.get(fragment.id)) {
                    annotNames.add(annotation.name)
                }
            }
            return annotNames
        }
    }
}
    
SampleReportScript script = new SampleReportScript()
script.run()
System.exit(0)