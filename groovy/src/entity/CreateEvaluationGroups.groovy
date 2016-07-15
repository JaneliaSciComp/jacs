package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*
import static org.janelia.it.jacs.shared.utils.EntityUtils.*

import java.text.SimpleDateFormat
import java.util.Set;

import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.screen.ScreenEvalUtils

// Parameters
username = "jenetta"
evalFolderName = "FlyLight Pattern Evaluation"
topLevelFolderName = "Arnim Data Combinations"

strongFolderName = "Strong Expression"
mediumFolderName = "Medium Expression"
weakFolderName = "Weak Expression"

splitADFolderName = "AD";
splitDBDFolderName = "DBD";

Set<String> compartments = new HashSet<String>(Arrays.asList("EB,FB,NOg,PBg".split(",")))


// Globals
f = new JacsUtils(username, true)
e = f.e
a = f.a
df = new SimpleDateFormat()
now = new Date()

// Main script
Entity evalFolder = f.loadChildren(a.getCommonRootFolderByName(username, evalFolderName, false))

println "Creating folder structure..."

Entity topLevelFolder = f.loadChildren(a.getCommonRootFolderByName(username, topLevelFolderName, true))
Entity linesFolder = f.loadChildren(a.getChildFolderByName(username, topLevelFolder.id, "Central Complex Split Lines", true))
Entity samplesFolder = f.loadChildren(a.getChildFolderByName(username, topLevelFolder.id, "Central Complex Screen Samples", true))

Entity linesStrongFolder = f.loadChildren(a.getChildFolderByName(username, linesFolder.id, strongFolderName, true))
Entity linesMediumFolder = f.loadChildren(a.getChildFolderByName(username, linesFolder.id, mediumFolderName, true))
Entity linesWeakFolder = f.loadChildren(a.getChildFolderByName(username, linesFolder.id, weakFolderName, true))

Entity linesADStrongFolder = f.loadChildren(a.getChildFolderByName(username, linesStrongFolder.id, splitADFolderName, true))
Entity linesADMediumFolder = f.loadChildren(a.getChildFolderByName(username, linesMediumFolder.id, splitADFolderName, true))
Entity linesADWeakFolder = f.loadChildren(a.getChildFolderByName(username, linesWeakFolder.id, splitADFolderName, true))

Entity linesDBDStrongFolder = f.loadChildren(a.getChildFolderByName(username, linesStrongFolder.id, splitDBDFolderName, true))
Entity linesDBDMediumFolder = f.loadChildren(a.getChildFolderByName(username, linesMediumFolder.id, splitDBDFolderName, true))
Entity linesDBDWeakFolder = f.loadChildren(a.getChildFolderByName(username, linesWeakFolder.id, splitDBDFolderName, true))

Entity samplesStrongFolder = f.loadChildren(a.getChildFolderByName(username, samplesFolder.id, strongFolderName, true))
Entity samplesMediumFolder = f.loadChildren(a.getChildFolderByName(username, samplesFolder.id, mediumFolderName, true))
Entity samplesWeakFolder = f.loadChildren(a.getChildFolderByName(username, samplesFolder.id, weakFolderName, true))

Map<Long,Entity> folderMap = new LinkedHashMap<Long,Entity>();
folderMap.put(linesADStrongFolder.id, linesADStrongFolder);
folderMap.put(linesADMediumFolder.id, linesADMediumFolder);
folderMap.put(linesADWeakFolder.id, linesADWeakFolder);
folderMap.put(linesDBDStrongFolder.id, linesDBDStrongFolder);
folderMap.put(linesDBDMediumFolder.id, linesDBDMediumFolder);
folderMap.put(linesDBDWeakFolder.id, linesDBDWeakFolder);
folderMap.put(samplesStrongFolder.id, samplesStrongFolder);
folderMap.put(samplesMediumFolder.id, samplesMediumFolder);
folderMap.put(samplesWeakFolder.id, samplesWeakFolder);

Map<Long,List<Long>> childMap = new LinkedHashMap<Long,Set<Long>>();
childMap.put(linesADStrongFolder.id, new LinkedHashSet<Long>());
childMap.put(linesADMediumFolder.id, new LinkedHashSet<Long>());
childMap.put(linesADWeakFolder.id, new LinkedHashSet<Long>());
childMap.put(linesDBDStrongFolder.id, new LinkedHashSet<Long>());
childMap.put(linesDBDMediumFolder.id, new LinkedHashSet<Long>());
childMap.put(linesDBDWeakFolder.id, new LinkedHashSet<Long>());
childMap.put(samplesStrongFolder.id, new LinkedHashSet<Long>());
childMap.put(samplesMediumFolder.id, new LinkedHashSet<Long>());
childMap.put(samplesWeakFolder.id, new LinkedHashSet<Long>());

println "Processing evaluations..."

for(Entity compEntity : evalFolder.children) {
	f.loadChildren(compEntity)
	if (!compartments.contains(compEntity.name)) continue;
	println "Processing "+compEntity.name
	
	for(Entity intFolder : compEntity.orderedChildren) {
		
		int i = ScreenEvalUtils.getValueFromFolderName(intFolder);
		if (i==0) continue;
		
		// Allocate this separately, so that memory can be freed once we're done with it
		Entity tmpIntFolder = e.getEntityById(""+intFolder.id)
		f.loadChildren(tmpIntFolder)
		
		for(Entity distFolder : tmpIntFolder.orderedChildren) {
			
			// Allocate this separately, so that memory can be freed once we're done with it
			Entity tmpDistFolder = e.getEntityById(""+distFolder.id)
			f.loadChildren(tmpDistFolder)
			
			int d = ScreenEvalUtils.getValueFromFolderName(distFolder);
			
			for(Entity mask : tmpDistFolder.orderedChildren) {
				
				screenSample = e.getAncestorWithType(mask, TYPE_SCREEN_SAMPLE)
				
				List<Long> flylineIds = new ArrayList<Long>(e.getParentIdsForAttribute(screenSample.id, ATTRIBUTE_REPRESENTATIVE_SAMPLE))
				for(Entity flyline : e.getEntitiesById(flylineIds)) {
					String splitPart = flyline.getValueByAttributeName(EntityConstants.ATTRIBUTE_SPLIT_PART);
					if ("AD".equals(splitPart)) {
						if (i==5 || (i==4 && d>=3)) {
							childMap.get(linesADStrongFolder.id).add(flyline.id)
						}
						else if (i==1 || (i==2 && d<=3)) {
							childMap.get(linesADWeakFolder.id).add(flyline.id)
						}
						else {
							childMap.get(linesADMediumFolder.id).add(flyline.id)
						}
					}
					else if ("DBD".equals(splitPart)) {
						if (i==5 || (i==4 && d>=3)) {
							childMap.get(linesDBDStrongFolder.id).add(flyline.id)
						}
						else if (i==1 || (i==2 && d<=3)) {
							childMap.get(linesDBDWeakFolder.id).add(flyline.id)
						}
						else {
							childMap.get(linesDBDMediumFolder.id).add(flyline.id)
						}
					}
				}
				
				if (i==5 || (i==4 && d>=3)) {
					childMap.get(samplesStrongFolder.id).add(screenSample.id)
				}
				else if (i==1 || (i==2 && d<=3)) {
					childMap.get(samplesWeakFolder.id).add(screenSample.id)
				}
				else {
					childMap.get(samplesMediumFolder.id).add(screenSample.id)
				}
			}
		}
	}
	
	
	for(Long folderId : folderMap.keySet()) {
		Entity folder = folderMap.get(folderId)
		Set<Long> childIds = childMap.get(folderId)
		if (childIds!=null) {
			println "  Now we have "+childIds.size()+" children for "+folder?.name
		}
		else {
			println "  Now we have no children for "+folder?.name
		}
	}	
}


println "Eliminating duplicate classifications..."

childMap.get(linesADWeakFolder.id).removeAll(childMap.get(linesADMediumFolder.id))
childMap.get(linesADWeakFolder.id).removeAll(childMap.get(linesADStrongFolder.id))
childMap.get(linesADMediumFolder.id).removeAll(childMap.get(linesADStrongFolder.id))

childMap.get(linesDBDWeakFolder.id).removeAll(childMap.get(linesDBDMediumFolder.id))
childMap.get(linesDBDWeakFolder.id).removeAll(childMap.get(linesDBDStrongFolder.id))
childMap.get(linesDBDMediumFolder.id).removeAll(childMap.get(linesDBDStrongFolder.id))

childMap.get(samplesWeakFolder.id).removeAll(childMap.get(samplesMediumFolder.id))
childMap.get(samplesWeakFolder.id).removeAll(childMap.get(samplesStrongFolder.id))
childMap.get(samplesMediumFolder.id).removeAll(childMap.get(samplesStrongFolder.id))

println "Populating folder hierarchy..."

for(Long folderId : folderMap.keySet()) {
	Entity folder = folderMap.get(folderId)
	Set<Long> childIds = childMap.get(folderId)
	println "  Adding "+childIds.size()+" to "+folder.name
	e.addChildren(username, folder.id, new ArrayList<Long>(childIds), ATTRIBUTE_ENTITY)
}
