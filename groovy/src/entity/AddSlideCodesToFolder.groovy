package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.StringUtils

class AddSlideCodesToFolderScript {
	
    private String ownerKey = "user:nerna";
	private static final boolean DEBUG = false;
	private final JacsUtils f;
	private String filename = "slides_uniq";
    private String OUTPUT_ROOT_NAME = "Modified Samples";
	private Entity rootFolder;
    
	public AddSlideCodesToFolderScript() {
		f = new JacsUtils(ownerKey, !DEBUG)
	}
	
	public void run() {
        
        this.rootFolder = f.getRootEntity(OUTPUT_ROOT_NAME)
        if (rootFolder!=null) {
            println "Deleting root folder "+OUTPUT_ROOT_NAME+". This may take a while!"
            if (!DEBUG)f.deleteEntityTree(rootFolder.id)
        }
        if (!DEBUG) rootFolder = f.createRootEntity(OUTPUT_ROOT_NAME)
        
        def file = new File(filename)
        file.eachLine {
            if (!StringUtils.isEmpty(it)) {
                String slideCode = it
                processSlideCode(slideCode)
            }
        }
        
        println "Done"
	}
	
	public void processSlideCode(String slideCode) {
        
        println "Processing "+slideCode
        
        for(Entity sample : f.e.getEntitiesWithAttributeValue(ownerKey, EntityConstants.ATTRIBUTE_SLIDE_CODE, slideCode)) {
            if (!ownerKey.equals(sample.ownerKey)) continue
            if (!EntityConstants.TYPE_SAMPLE.equals(sample.entityTypeName)) continue
            if (sample.name.contains("~")) continue
            println "  Adding "+sample.name+" ("+sample.id+")"
            if (!DEBUG) {
                f.addToParent(rootFolder, sample, rootFolder.maxOrderIndex+1, EntityConstants.ATTRIBUTE_ENTITY)
            }
        }
	}
}

AddSlideCodesToFolderScript script = new AddSlideCodesToFolderScript();
script.run();
System.exit(0)