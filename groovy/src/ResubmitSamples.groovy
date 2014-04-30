import org.apache.commons.io.IOUtils
import org.janelia.it.FlyWorkstation.api.facade.concrete_facade.ejb.EJBFacadeManager
import org.janelia.it.FlyWorkstation.api.facade.facade_mgr.FacadeManager
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.ExitHandler
import org.janelia.it.FlyWorkstation.gui.framework.exception_handlers.UserNotificationExceptionHandler
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr
import org.janelia.it.FlyWorkstation.gui.util.panels.ApplicationSettingsPanel
import org.janelia.it.FlyWorkstation.gui.util.panels.DataSourceSettingsPanel
import org.janelia.it.FlyWorkstation.gui.util.panels.ViewerSettingsPanel
import org.janelia.it.FlyWorkstation.shared.util.ConsoleProperties
import org.janelia.it.FlyWorkstation.shared.util.filecache.WebDavClient
import org.janelia.it.jacs.model.entity.Entity
import org.janelia.it.jacs.model.tasks.Event
import org.janelia.it.jacs.model.tasks.Task
import org.janelia.it.jacs.model.tasks.TaskParameter
import org.janelia.it.jacs.model.tasks.utility.GenericTask
import org.janelia.it.jacs.shared.utils.EntityUtils

import static org.janelia.it.jacs.model.entity.EntityConstants.*


subject = "group:leetlab"
data_set_folder = "Central Brain 40x"
f = new JacsUtils(subject, false)
e = f.e
c = f.c

pipelineProcess = "PipelineConfig_LeetWholeBrain40x512pxINT"

int numSamples = 0
int numSamplesReprocessed = 0

Entity folder = e.getUserEntitiesByNameAndTypeName("user:leey10", "Align", TYPE_FOLDER)[0]
f.loadChildren(folder)

for (Entity sample : EntityUtils.getChildrenOfType(folder, TYPE_SAMPLE)) {

    println "Reprocess "+sample.name
    HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
    taskParameters.add(new TaskParameter("sample entity id", sample.id.toString(), null));
    taskParameters.add(new TaskParameter("reuse processing", "false", null));
    taskParameters.add(new TaskParameter("reuse alignment", "false", null));
    Task task = new GenericTask(new HashSet<Node>(), subject, new ArrayList<Event>(),
            taskParameters, "sampleAllPipelines", "Sample All Pipelines");
    task = c.saveOrUpdateTask(task);
    c.submitJob(pipelineProcess, task.getObjectId());
    numSamplesReprocessed++;
    sample.setEntityData(null)
    numSamples++;
}

println "Num samples processed: "+numSamples
println "Num samples reprocessed: "+numSamplesReprocessed
