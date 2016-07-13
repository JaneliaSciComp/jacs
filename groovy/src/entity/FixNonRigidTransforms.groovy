package entity

import static org.janelia.it.jacs.model.entity.EntityConstants.*

import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.workstation.gui.util.panels.UserAccountSettingsPanel
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.workstation.shared.util.ConsoleProperties
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.entity.EntityConstants
import org.janelia.it.jacs.shared.utils.EntityUtils
import org.janelia.it.jacs.shared.utils.FileUtil
import org.janelia.it.jacs.shared.utils.entity.AbstractEntityLoader

// Globals
subject = "user:nerna"
f = new JacsUtils(subject, false)
String outputFilename = "output.txt"
PrintWriter file = new PrintWriter(outputFilename)
login()

Set<String> subjectKeys = new HashSet<String>();
for(Entity dataSet : f.e.getEntitiesByTypeName(null, EntityConstants.TYPE_DATA_SET)) {
    subjectKeys.add(dataSet.getOwnerKey());
}

println "Found users with data sets: "+subjectKeys
for(String subjectKey : subjectKeys) {
    println "Processing "+subjectKey;
    //if (!subjectKey.equals("user:nerna")) continue
    
    f = new JacsUtils(subjectKey, false)
    
    int c = 0
    int nonrigid = 0
    int rigid = 0
    
    AbstractEntityLoader loader = f.getEntityLoader();
    
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(0);
    cal.set(2013, 4, 12, 0, 0, 0);
    Date cutoff = cal.getTime();
    
    f.e.getUserEntitiesByTypeName(subjectKey, TYPE_IMAGE_TILE).each {
        Entity tile = (Entity)it
        if (tile.creationDate.after(cutoff)) {
            loader.populateChildren(tile)
            if (EntityUtils.getChildrenOfType(tile, TYPE_LSM_STACK).size()>1) {
                String defaultFilepath = tile.getValueByAttributeName(ATTRIBUTE_DEFAULT_2D_IMAGE)
                if (defaultFilepath!=null) {
                    String ttFilepath = defaultFilepath.replaceAll("_signal.png", "-tt.txt")
                    File ttFile = SessionMgr.getSessionMgr().getCachedFile(ttFilepath, false)
                    if (ttFile!=null) {
                        String tt = FileUtil.getFileContentsAsString(ttFile.getAbsolutePath())
                        if (tt.contains("1 non-rigid transformations")) {
                            Entity sample = f.e.getAncestorWithType(subjectKey, tile.id, "Sample")
                            if (sample==null) {
                                println("Ancestor aample is null for tile: "+tile.id);
                            }
                            else {
                                if (sample.name.contains("~")) {
                                    sample = f.e.getAncestorWithType(subjectKey, sample.id, "Sample")
                                    if (sample==null) {
                                        println("Ancestor sample is null for subsample: "+sample.id);
                                    }
                                }
                                if (sample!=null) {
                                    f.e.setOrUpdateValue(subjectKey, sample.id, ATTRIBUTE_STATUS, VALUE_MARKED)
                                    file.println(subjectKey+" "+sample.name)
                                    nonrigid++;
                                }
                            }
                        }
                        else {
                            rigid++;
                        }
                    }
                }
            }
        }
        
        // Free memory
        tile.setEntityData(null)
        c++
    }
    
    println "Processed "+c+" tiles"
    println "  "+rigid+" rigid tiles"
    println "  "+nonrigid+" non-rigid tiles"
    println "  "+(c-rigid-nonrigid)+" unknown tiles"
}

file.close()
println "Wrote "+outputFilename


// Need this in order to use WebDAV
def login() {
    try {
       // This try block is copied from ConsoleApp. We may want to consolidate these in the future.
       ConsoleProperties.load();
       FacadeManager.registerFacade(FacadeManager.getEJBProtocolString(), EJBFacadeManager.class, "JACS EJB Facade Manager");
       final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
       sessionMgr.registerExceptionHandler(new UserNotificationExceptionHandler());
       sessionMgr.registerExceptionHandler(new ExitHandler());
       sessionMgr.registerPreferenceInterface(ApplicationSettingsPanel.class, ApplicationSettingsPanel.class);
       sessionMgr.registerPreferenceInterface(UserAccountSettingsPanel.class, UserAccountSettingsPanel.class);
       sessionMgr.registerPreferenceInterface(ViewerSettingsPanel.class, ViewerSettingsPanel.class);
       SessionMgr.getSessionMgr().loginSubject();
    }
    catch (Exception e) {
       SessionMgr.getSessionMgr().handleException(e);
       SessionMgr.getSessionMgr().systemExit();
    }
}