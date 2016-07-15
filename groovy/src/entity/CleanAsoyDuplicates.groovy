package entity

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils

class CleanAsoyDuplicatesScript {
	
	private static final boolean DEBUG = true;
	private final JacsUtils f; 
	private String ownerKey = "user:asoy";
	
	public CleanAsoyDuplicatesScript() {
		f = new JacsUtils(null, false)
	}
	
	public void run() {
		
		List<Long> toDelete = new ArrayList<Long>();
		
		for(Entity folder : f.e.getUserEntitiesByNameAndTypeName(ownerKey, "Retired Data", "Folder")) {
			f.loadChildren(folder)
			for(Entity sample : EntityUtils.getChildrenOfType(folder, "Sample")) {
				
				println "Checking "+sample.name
				
				String realName = sample.name.replaceAll("-Retired","")
				Set<Entity> matchingSamples = f.e.getEntitiesByName(ownerKey, realName)
				if (matchingSamples.size()>1) {
					println "  More than one matching sample: "+matchingSamples.size()
				}
				
				Entity realSample = matchingSamples.iterator().next()
				f.loadChildren(realSample)
				
				boolean broken = false;
				
				Entity supportingData = EntityUtils.getSupportingData(realSample)
				f.loadChildren(supportingData)
				if (supportingData != null) {
					for(Entity imageTile : EntityUtils.getChildrenForAttribute(supportingData, EntityConstants.ATTRIBUTE_ENTITY)) {
						if (imageTile.name.equals("Tile 1")) {
							broken = true;	
							break;
						}
					}
				}
				
				if (broken) {
					println "  Will delete duplicate: "+realSample.name
					toDelete.add(realSample.id);
				}
			}
		}
		
		for(Long id : toDelete) {
			if (DEBUG) {
				println "Would like to delete "+id
			}
			else {
				println "Deleting "+id
				f.e.deleteEntityTreeById(ownerKey, id)
			}	
		}
		
		println "Done"
	}
	
}

CleanAsoyDuplicatesScript script = new CleanAsoyDuplicatesScript();
script.run();
System.exit(0);