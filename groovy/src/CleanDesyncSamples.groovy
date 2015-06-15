
import java.util.Collections;
import java.util.Comparator;

import javax.ejb.EntityContext;

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.model.entity.EntityData
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanDesyncSamplesScript {
	
	private static final boolean DEBUG = true;
    private String ownerKey = null;
    private final JacsUtils f;
	private String context;
    private int numOriginalBlocked = 0
    private int numProblems = 0
    private int numNonIssues = 0
    private int numFixed = 0
    
	public CleanDesyncSamplesScript() {
		f = new JacsUtils(ownerKey, !DEBUG)
	}
	
	public void run() {
        
        if (ownerKey==null) {
            Set<String> subjectKeys = new TreeSet<String>();
            for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
                subjectKeys.add(dataSet.getOwnerKey());
            }
            for(String subjectKey : subjectKeys) {
                if (subjectKey.equals("user:nerna")) continue
                processSamples(subjectKey);
            }
        }
        else {
            processSamples(ownerKey);
        }
        
        
        println "Done"
        System.exit(0)
	}

    private void processSamples(String ownerKey) {
        
        println "Processing samples for "+ownerKey
        
        Collection<Entity> desync = f.e.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_DESYNC)
        println ownerKey+" had "+desync.size()+" desynced samples"
        
        Set<String> names = new HashSet<>()
        for(Entity sample : desync) {
            names.add(sample.name)
        }
        
        for(String name : names) {
            Collection<Entity> dups = f.e.getEntitiesByName(ownerKey, name)
            if (dups.size()>1) {
                processProblem(name, dups)
//                if (numFixed>0) break
            }
        }
        
        println ownerKey+" had "+numOriginalBlocked+" blocked samples that were unblocked by desyncing"
        println ownerKey+" had "+numProblems+" problem samples and "+numFixed+" were fixed"
        println ownerKey+" had "+numNonIssues+" non-issues that will work themselves out with retirement"
    }
    
    private void processProblem(String name, Collection<Entity> samples) {
        
        List<Entity> ordered = new ArrayList<Entity>()
        
        for(Entity sample : samples) {
            if (sample.entityTypeName.equals(EntityConstants.TYPE_SAMPLE)) {
                ordered.add(sample)
            }
        }
        
        Collections.sort(ordered, new Comparator<Entity>() {
            @Override
            public int compare(Entity o1, Entity o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        
        Entity original = ordered.get(0)
        Entity newest = ordered.get(ordered.size()-1)
        
        String originalStatus = original.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
        String newestStatus = newest.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
        
        if (EntityConstants.VALUE_DESYNC.equals(newestStatus)) {
            println name+" was already fixed in a previous run (in theory)"
            return
        }
        
        StringBuilder sb = new StringBuilder()
        int numTotalAnnots = 0
        for(Entity sample : ordered) {
            int annots = f.a.getNumDescendantsAnnotated(sample.id)
            String status = sample.getValueByAttributeName(EntityConstants.ATTRIBUTE_STATUS)
            numTotalAnnots += annots
            sb.append("    "+sample.id+" "+annots+" ("+status+")\n")
        }

        if (numTotalAnnots>0) {
            println name
            print sb.toString()
            fixProblem(name, ordered)
            numProblems++
        }
        else {
            if (originalStatus.equals(EntityConstants.VALUE_BLOCKED)) {
                println name
                println "    Original "+original.name+" was blocked. We should reblock."
                numOriginalBlocked++;
            }
            else {
                numNonIssues++
            }
        }
    }
    
    private void fixProblem(String name, List<Entity> ordered) {
        
        Entity original = ordered.get(0)
        Entity newest = ordered.get(ordered.size()-1)
        
        f.loadChildren(original)
        f.loadChildren(newest)
        
        Entity sf = EntityUtils.getSupportingData(newest)
        f.loadChildren(sf)
        
        boolean aaProblem = false
        Map<String,String> tileMap = new HashMap<>() 
        for(Entity tile : sf.children) {
            String aa = tile.getValueByAttributeName(EntityConstants.ATTRIBUTE_ANATOMICAL_AREA)
            String currAa = tileMap.get(tile.name)
            if (currAa!=null && !currAa.equals(aa)) {
                aaProblem = true
            }
            tileMap.put(tile.name, aa)
        }
        
        if (!aaProblem) {
            println "- Sample has unknown problem, will not attempt surgery"
            return
        }
        
        swapTiles(original, newest)
        
        
        List<Entity> originalChildSamples = EntityUtils.getChildrenOfType(original, "Sample")
        List<Entity> newestChildSamples = EntityUtils.getChildrenOfType(newest, "Sample")
        
        if (!originalChildSamples.isEmpty()) {
            
            Entity originalSample20x = null
            Entity originalSample63x = null
            
            // Remove extra sub-samples
            int c = 0
            for(Iterator<EntityData> i = original.getOrderedEntityData().iterator(); i.hasNext(); ) {
                EntityData ed = i.next()
                Entity child = ed.getChildEntity()
                if (child!=null && child.entityTypeName.equals("Sample")) {
                    if (c>1) {
                        i.remove();
                        println "- Removing extra sub-sample "+child.name 
                        if (!DEBUG) {
                            f.e.deleteEntityTreeById(ownerKey, child.id, true)
                        }
                    }
                    else {
                        if (child.name.endsWith("20x")) {
                            originalSample20x = child
                        }
                        else if (child.name.endsWith("63x")) {
                            originalSample63x = child
                        }
                    }
                    c++
                }
            }
            
            if (originalSample20x==null || originalSample63x==null) {
                throw new IllegalStateException("Could not find original sub-samples under "+original.id)
            }
            
            for(Entity childSample : newestChildSamples) {
                
                if (childSample.name.endsWith("20x")) {
                    swapTiles(originalSample20x, childSample)
                }
                if (childSample.name.endsWith("63x")) {
                    swapTiles(originalSample63x, childSample)
                }
            }   
        }
        
        println "- Updating "+newest.id+" with desync status"
        
        if (!DEBUG) {
            f.e.setOrUpdateValue(ownerKey, newest.id, EntityConstants.ATTRIBUTE_STATUS, EntityConstants.VALUE_DESYNC)
        }
        
        numFixed++
    }
 
    private void swapTiles(Entity sample1, Entity sample2) {
        
        if (sample1==null || sample2==null) {
            throw new IllegalStateException("Attempting to swap null sample: "+sample1+" and "+sample2)
        }
        
        if (!sample1.name.equals(sample2.name)) {
            throw new IllegalStateException("Attempting to swap incompatible samples: "+sample1+" and "+sample2)
        }
        
        println "- Swapping tiles for "+sample1.id+" and "+sample2.id
        
        f.loadChildren(sample1)
        f.loadChildren(sample2)
        
        Entity sf1 = EntityUtils.getSupportingData(sample1)
        Entity sf2 = EntityUtils.getSupportingData(sample2)
        
        f.loadChildren(sf1)
        f.loadChildren(sf2)
        
        for(Iterator<EntityData> i = sf1.getEntityData().iterator(); i.hasNext(); ) {
            EntityData ed = i.next()
            i.remove();
            ed.setParentEntity(sf2)
            //println "Moving "+ed.id+" to "+sf2.id
            if (!DEBUG) {
                f.e.saveOrUpdateEntityData(ownerKey, ed)
            }
        }
        
        for(Iterator<EntityData> i = sf2.getEntityData().iterator(); i.hasNext(); ) {
            EntityData ed = i.next()
            i.remove();
            ed.setParentEntity(sf1)
            //println "Moving "+ed.id+" to "+sf1.id
            if (!DEBUG) {
                f.e.saveOrUpdateEntityData(ownerKey, ed)
            }
        }
    }   
}

CleanDesyncSamplesScript script = new CleanDesyncSamplesScript();
script.run();