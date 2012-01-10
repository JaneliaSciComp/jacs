
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.utils.SystemException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 20, 2008
 * Time: 3:13:32 PM
 */
public class ExportAPI {
    static Logger logger = Logger.getLogger(ExportAPI.class.getName());

    TaskDAO taskDAO;
    ComputeBeanRemote computeBean;

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public void setComputeBean(ComputeBeanRemote computeBean) {
        this.computeBean = computeBean;
    }

    public String submitExportTask(User requestingUser, ExportTask exportTask) throws SystemException {
        try {
            exportTask.setOwner(requestingUser.getUserLogin());
            exportTask.setJobName(exportTask.getSuggestedFilename());
            exportTask = (ExportTask) taskDAO.saveOrUpdateTask(exportTask);
            computeBean.submitJob("FileExport", exportTask.getObjectId());
            logger.debug("Export job submitted successfully.");
            return exportTask.getObjectId().toString();
        }
        catch (Throwable e) {
            logger.error("Error processing submitExportTask: " + e.getMessage());
        }
        throw new SystemException("Unable to submit the export task.");
    }
}
