
package org.janelia.it.jacs.compute.service.export;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.export.processor.ExportProcessor;
import org.janelia.it.jacs.compute.service.export.processor.ExportProcessorFactory;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.export.ExportFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 25, 2008
 * Time: 1:21:38 PM
 */
public class FileExportService implements IService {
    private Logger logger;
    protected ExportTask exportTask;
    protected ExportFileNode resultNode;
    protected IProcessData processData;
    protected String sessionName;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("FileExportService execute() started");
            this.processData = processData;
            this.exportTask = (ExportTask) ProcessDataHelper.getTask(processData);
            createResultNodeForExportTask(exportTask);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            ExportProcessor exportProcessor = ExportProcessorFactory.createProcessor(
                    exportTask, resultNode);
            exportProcessor.process();
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void createResultNodeForExportTask(ExportTask task) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        logger.info("task owner=" + task.getOwner());
        if (task.getOwner() == null) {
            logger.info("user is null");
        }
        else {
            logger.info("user is not null");
        }
        resultNode = new ExportFileNode(
                task.getOwner(),
                task,
                "ExportFileNode for job=" + task.getJobName(),
                "ExportFileNode for task=" + task.getObjectId(),
                Node.VISIBILITY_PUBLIC,
                sessionName);
        resultNode = (ExportFileNode) computeBean.createNode(resultNode);
        FileUtil.ensureDirExists(resultNode.getDirectoryPath());
        logger.info("Created ExportResultNode with dir path=" + resultNode.getDirectoryPath());
    }

}
