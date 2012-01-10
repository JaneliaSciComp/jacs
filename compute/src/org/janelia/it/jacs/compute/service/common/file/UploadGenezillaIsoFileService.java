
package org.janelia.it.jacs.compute.service.common.file;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.utility.UploadGenezillaIsoFileTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.tools.GenezillaIsoFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: jinman
 * Date: Jun 29, 2010
 * Time: 9:56:00 AM
 */
public class UploadGenezillaIsoFileService implements IService {

    private File sourceFile;
    UploadGenezillaIsoFileTask task;
    String sessionName;
    File scratchDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));

    public UploadGenezillaIsoFileService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("UploadGenezillaIsoFileService execute() start");
            init(processData);
            GenezillaIsoFileNode isoFileNode = createGenezillaIsoFileNode();
            File targetFile = new File(isoFileNode.getIsoFilePath());
            logger.info("Copying file to GenezillaIsoFileNode source=" + sourceFile.getAbsolutePath() + " target=" + targetFile.getAbsolutePath());
            FileUtil.copyFileUsingSystemCall(sourceFile, targetFile);
            logger.info("UploadGenezillaIsoFileService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private GenezillaIsoFileNode createGenezillaIsoFileNode() throws Exception {
        GenezillaIsoFileNode node = new GenezillaIsoFileNode(task.getOwner(), task, "Upload node for task=" + task.getObjectId(),
                "Upload of dir=" + sourceFile.getAbsolutePath(), Node.VISIBILITY_PUBLIC, 0, sessionName);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        node = (GenezillaIsoFileNode) computeBean.saveOrUpdateNode(node);
        File nodeFile = new File(node.getDirectoryPath());
        if (!nodeFile.mkdirs()) {
            throw new Exception("Could not create node directory=" + nodeFile.getAbsolutePath());
        }
        return node;
    }

    /**
     * Initialize the input parameters
     *
     * @param processData params of the task
     * @throws org.janelia.it.jacs.compute.engine.data.MissingDataException
     *                             cannot find data required to process
     * @throws java.io.IOException problem accessing file data
     */
    protected void init(IProcessData processData) throws MissingDataException, IOException {
        task = (UploadGenezillaIsoFileTask) ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        String tmpSourceFile = task.getParameter(UploadGenezillaIsoFileTask.PARAM_SOURCE_FILE);
        // If the task has the source dir, use it
        if (null == tmpSourceFile || "".equals(tmpSourceFile)) {
            setSourceFile(processData);
        }
        // else look in the *.process file for it
        else {
            sourceFile = FileUtil.checkFileExists(tmpSourceFile);
        }
    }

    private void setSourceFile(IProcessData processData) throws MissingDataException, IOException {
        Object sourceFileObj = processData.getMandatoryItem(UploadGenezillaIsoFileTask.PARAM_SOURCE_FILE);
        if (sourceFileObj instanceof String) {
            sourceFile = (FileUtil.checkFileExists((String) sourceFileObj));
        }
        else if (sourceFileObj instanceof File && ((File) sourceFileObj).isFile()) {
            sourceFile = (File) sourceFileObj;
        }
        else {
            throw new MissingDataException("Could not process sourceFileObj - expected it to be a String or File");
        }
    }

}