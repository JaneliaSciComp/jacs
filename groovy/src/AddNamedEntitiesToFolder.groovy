import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.FlyWorkstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettingsPanel
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.FileUtil
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader


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
