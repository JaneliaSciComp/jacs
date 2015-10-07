
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
import org.janelia.it.jacs.model.tasks.utility.UploadFastqDirectoryTask;
import org.janelia.it.jacs.model.user_data.FastqDirectoryNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.node.FastqUtil;
import org.janelia.it.jacs.shared.node.FastqUtil.LaneAndDirection;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 2, 2010
 * Time: 3:28:24 PM
 * <p/>
 * This service uploads directories containing a set of FASTQ files, typically
 * from an Illumina run.
 * <p/>
 * The data is either paired or un-paired.
 * <p/>
 * If the data is un-paired, the filenames should be:
 * <p/>
 * <prefix>1_1.<extension>
 * <prefix>2_1.<extension>
 * ...
 * <p/>
 * If the data is paired, the filenames should be:
 * <p/>
 * <prefix>1_1.<extension>   "left"
 * <prefix>1_2.<extension>   "right"
 * <prefix>2_1.<extension>   "left"
 * <prefix>2_2.<extension>   "right"
 * <prefix>3_1.<extension>   "left"
 * <prefix>3_2.<extension>   "right"
 * ...
 * ...
 * <p/>
 * Regardless of the <prefix> and the <extension> in the source directory, the
 * upload service converts the filenames to use:
 * <p/>
 * <prefix> = "s"
 * <extension> = ".fq"
 */
public class UploadFastqDirectoryService implements IService {

    private File sourceDirectory;
    UploadFastqDirectoryTask task;
    String sessionName;
    File scratchDir = new File(SystemConfigurationProperties.getString("SystemCall.ScratchDir"));

    public UploadFastqDirectoryService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info("UploadFastqDirectoryService execute() start");
            init(processData);
            List<LaneAndDirection> ladList = FastqUtil.getLaneAndDirectionListFromFastqDir(sourceDirectory, logger);
            logger.info("Order of lad files:");
            for (LaneAndDirection lad : ladList) {
                logger.info("lane=" + lad.getLane() + " direction=" + lad.getDirection() + " file=" + lad.getFilename() + " stdFile=" + lad.getStandardFilename());
            }
            // Check name uniqueness
            Set<String> ladUniqCheckSet = new HashSet<String>();
            for (LaneAndDirection lad : ladList) {
                if (ladUniqCheckSet.contains(lad.getStandardFilename())) {
                    throw new Exception("Problem with naming convention of contents of source directory=" + sourceDirectory.getAbsolutePath());
                }
                ladUniqCheckSet.add(lad.getStandardFilename());
            }
            long totalSequenceCount = 0;
            boolean isPaired = FastqUtil.isLaneAndDirectionListPaired(ladList);
            logger.info("Paired status of directory=" + sourceDirectory.getAbsolutePath() + " is=" + isPaired);
            for (LaneAndDirection lad : ladList) {
                File file = new File(sourceDirectory, lad.getFilename());
                long sequenceCount = FastqUtil.countSequencesInFastqFile(file, scratchDir, logger);
                logger.info("Found " + sequenceCount + " sequences in file=" + file.getAbsolutePath());
                totalSequenceCount += sequenceCount;
            }
            logger.info("Found total of " + totalSequenceCount + " sequences in directory=" + sourceDirectory.getAbsolutePath());
            FastqDirectoryNode fastqDirNode = createFastqDirectoryNode(totalSequenceCount, isPaired);
            File targetDirectory = new File(fastqDirNode.getDirectoryPath());
            for (LaneAndDirection lad : ladList) {
                logger.info("Processing lad with lane=" + lad.getLane() + " direction=" + lad.getDirection() + " filename=" + lad.getFilename() + " stdFilename=" + lad.getStandardFilename());
                File sourceFile = new File(sourceDirectory, lad.getFilename());
                File targetFile = new File(targetDirectory, lad.getStandardFilename());
                logger.info("Copying file to FastqDirectoryNode source=" + sourceFile.getAbsolutePath() + " target=" + targetFile.getAbsolutePath());
                FileUtil.copyFileUsingSystemCall(sourceFile, targetFile);
            }
            logger.info("UploadFastqDirectoryService execute() end");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private FastqDirectoryNode createFastqDirectoryNode(long totalSequenceCount, Boolean isPaired) throws Exception {
        if (isPaired) {
            if (task.getParameter(UploadFastqDirectoryTask.PARAM_MATE_MEAN_INNER_DISTANCE) == null ||
                    task.getMateMeanInnerDistace() == 0) {
                throw new Exception("If the FastqDirectoryNode is specified as PAIRED then the MateMeanInnerDistance must be set and non-zero");
            }
        }
        String nodeName=task.getParameter(UploadFastqDirectoryTask.PARAM_NODE_NAME);
        if (nodeName==null || nodeName.trim().length()==0) {
            nodeName="Upload node for task="+task.getObjectId();
        }
        FastqDirectoryNode node = new FastqDirectoryNode(task.getOwner(), task, nodeName,
                "Upload of dir=" + sourceDirectory.getAbsolutePath(), Node.VISIBILITY_PUBLIC, isPaired, task.getMateMeanInnerDistace(),
                new Integer(totalSequenceCount + ""), sessionName);
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        node = (FastqDirectoryNode) computeBean.saveOrUpdateNode(node);
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
        task = (UploadFastqDirectoryTask) ProcessDataHelper.getTask(processData);
        sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        String tmpSourceDir = task.getParameter(UploadFastqDirectoryTask.PARAM_SOURCE_DIR);
        // If the task has the source dir, use it
        if (null == tmpSourceDir || "".equals(tmpSourceDir)) {
            setSourceDir(processData);
        }
        // else look in the *.process file for it
        else {
            sourceDirectory = FileUtil.checkFileExists(tmpSourceDir);
        }
    }

    private void setSourceDir(IProcessData processData) throws MissingDataException, IOException {
        Object sourceDirObj = processData.getMandatoryItem(UploadFastqDirectoryTask.PARAM_SOURCE_DIR);
        if (sourceDirObj instanceof String) {
            sourceDirectory = (FileUtil.checkFileExists((String) sourceDirObj));
        }
        else if (sourceDirObj instanceof File && ((File) sourceDirObj).isDirectory()) {
            sourceDirectory = (File) sourceDirObj;
        }
        else {
            throw new MissingDataException("Could not process sourceDirObj - expected it to be a String or Directory");
        }
    }

}
