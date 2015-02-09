import org.janelia.it.jacs.model.entity.Entity

inputFilename = "nerna_sample_rerun.txt"

// Globals
subject = "user:nerna"
f = new JacsUtils(subject, true)
String folderName = "Non-Rigid Merge Reruns"

def inputFile = new File(inputFilename)

Entity topFolder = f.getRootEntity(subject, folderName)
if (topFolder==null) {
    topFolder = f.createRootEntity(folderName)
    println "Created new root entity: "+folderName
}
else {
    println "Found existing root entity: "+folderName
}

inputFile.eachLine {
    def name = it;
    
    println name
    
    Set<Entity> matching = f.e.getEntitiesByName(subject, name)
    Set<Long> ids = new HashSet<Long>() 
    
    if (matching.isEmpty()) {
        println "  WARNING: Could not find entity with name: "+name;
    }
    else {
        if (matching.size()>1) {
            println "  WARNING: More than one entity found with name: "+name
        }
        
        for(Entity entity : matching) {
            println "  "+entity.id
            ids.add(entity.id)
        }
    }
    
    f.e.addChildren(subject, topFolder.id, new ArrayList<Long>(ids), "Entity")
    
}

println "Done"
