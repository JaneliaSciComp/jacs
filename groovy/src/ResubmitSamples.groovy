import org.apache.commons.io.IOUtils
import org.janelia.it.workstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.workstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.workstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.workstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.workstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.workstation.gui.util.panels.UserAccountSettingsPanel
import org.janelia.it.workstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.workstation.shared.util.ConsoleProperties
import org.janelia.it.workstation.shared.util.filecache.WebDavClient
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.shared.utils.EntityUtils

import static org.janelia.it.jacs.model.entity.EntityConstants.*

// Globals
subject = "group:leetlab"
data_set_folder = "Central Brain 40x"
f = new JacsUtils(subject, false)
e = f.e
c = f.c

login()

int numSamples = 0
int numSamplesReprocessed = 0

Entity folder = e.getUserEntitiesByNameAndTypeName(subject, data_set_folder, TYPE_FOLDER)[0]
f.loadChildren(folder)

for (Entity sample : EntityUtils.getChildrenOfType(folder, TYPE_SAMPLE)) {

    boolean reprocess = false;
    String standardPath = ""

    f.loadChildren(sample)
    Entity latestRun = EntityUtils.getLatestChildOfType(sample, TYPE_PIPELINE_RUN)

    if (latestRun!=null) {
        f.loadChildren(latestRun)
        Entity alignment = EntityUtils.getLatestChildOfType(latestRun, TYPE_ALIGNMENT_RESULT)
        if (alignment!=null) {
            standardPath = alignment.getValueByAttributeName(ATTRIBUTE_FILE_PATH)
            WebDavClient client = SessionMgr.getSessionMgr().getWebDavClient()
            URL remoteDirUrl = client.getWebDavUrl(standardPath)
            URL remoteFileUrl = new URL(remoteDirUrl.toString()+"/sge_config/alignCmd.sh")

            InputStream input = remoteFileUrl.openStream()
            try {
                if (IOUtils.toString(input).contains("imprv")) {
                    reprocess = true;
                }
            }
            finally {
              IOUtils.closeQuietly(input);
            }
        }
    }

    println sample.name+"\t"+standardPath+"\t"+reprocess

    if (reprocess) {
//        println "Reprocess "+sample.name
//        HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
//        taskParameters.add(new TaskParameter("sample entity id", sample.id.toString(), null));
//        taskParameters.add(new TaskParameter("reuse processing", "false", null));
//        Task task = new GenericTask(new HashSet<Node>(), subject, new ArrayList<Event>(),
//                taskParameters, "sampleAllPipelines", "Sample All Pipelines");
//        task = c.saveOrUpdateTask(task);
//        c.submitJob("GSPS_CompleteSamplePipeline", task.getObjectId());
        numSamplesReprocessed++;
    }

    sample.setEntityData(null)
    numSamples++;
}

println "Num samples processed: "+numSamples
println "Num samples reprocessed: "+numSamplesReprocessed


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