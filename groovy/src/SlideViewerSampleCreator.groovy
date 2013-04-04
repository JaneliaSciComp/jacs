import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants

// Parameters
username = "saffordt"
rootEntityName = "Slide Viewer Data"
targetFolder = "Slide Viewer Data"

// Globals
f = new JacsUtils(username, true)
a = f.a

// Main script
Entity folder = f.newEntity(rootEntityName, EntityConstants.TYPE_FOLDER)
folder = f.save(folder)
Entity sample = f.newEntity("octreeZ", "3D Tile Microscope Sample")
sample.setValueByAttributeName("Confocal Blocks Sample", "/nobackup/jacsdata/octreeZ")
sample = f.save(sample)
println "Saved sample as "+sample.id
f.addToParent(folder, sample)

Entity sample2 = f.newEntity("pyramidZ", "3D Tile Microscope Sample")
sample2.setValueByAttributeName("Confocal Blocks Sample", "/nobackup/jacsdata/pyramidZ")
sample2 = f.save(sample2)
println "Saved sample as "+sample2.id
f.addToParent(folder, sample2)

