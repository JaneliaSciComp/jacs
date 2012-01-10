
package org.janelia.it.jacs.compute.service.rnaSeq;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.rnaSeq.UploadRnaSeqReferenceGenomeTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.rnaSeq.RnaSeqReferenceGenomeNode;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 22, 2010
 * Time: 11:25:59 AM
 */
public class UploadRnaSeqReferenceGenomeService implements IService {

    private File sourceFile;
    UploadRnaSeqReferenceGenomeTask task;
    String sessionName;
    File scratchDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));

    public UploadRnaSeqReferenceGenomeService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("UploadRnaSeqReferenceGenomeService execute() start");
            init(processData);
            long[] sequenceCountArr = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
            long sequenceCount=sequenceCountArr[0];
            logger.info("Found " + sequenceCount + " sequences in file=" + sourceFile.getAbsolutePath());
            RnaSeqReferenceGenomeNode refFileNode = createReferenceGenomeNode(sequenceCount);
            File targetFile = new File(refFileNode.getFastaFilePath());
            logger.info("Copying file to RnaSeqReferenceGenomeNode source=" + sourceFile.getAbsolutePath() + " target=" + targetFile.getAbsolutePath());
            FileUtil.copyFileUsingSystemCall(sourceFile, targetFile);
            logger.info("UploadRnaSeqReferenceGenomeService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private RnaSeqReferenceGenomeNode createReferenceGenomeNode(long totalSequenceCount) throws Exception {
        String nodeName=task.getParameter(UploadRnaSeqReferenceGenomeTask.PARAM_NODE_NAME);
        if (nodeName==null || nodeName.trim().length()==0) {
            nodeName="Upload node for task="+task.getObjectId();
        }
        RnaSeqReferenceGenomeNode node = new RnaSeqReferenceGenomeNode(task.getOwner(), task, nodeName,
                "Upload of dir=" + sourceFile.getAbsolutePath(), Node.VISIBILITY_PUBLIC, new Integer(totalSequenceCount + ""), sessionName);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        node = (RnaSeqReferenceGenomeNode) computeBean.saveOrUpdateNode(node);
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
        task = (UploadRnaSeqReferenceGenomeTask) ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        String tmpSourceFile = task.getParameter(UploadRnaSeqReferenceGenomeTask.PARAM_SOURCE_FILE);
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
        Object sourceFileObj = processData.getMandatoryItem(UploadRnaSeqReferenceGenomeTask.PARAM_SOURCE_FILE);
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
