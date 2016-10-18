
package org.janelia.it.jacs.compute.service.common.file;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.zip.ZipException;
import org.janelia.it.jacs.model.tasks.utility.CopyFileTask;
import org.janelia.it.jacs.model.tasks.utility.UploadFileTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.fasta.FastaFile;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The service can copy, move, extract (create File lists), delete, and create files depending
 * on what mode it's running in.  The parameters it expects and its output will vary depending on
 * what mode it's running in.  It's mode and as well as it's input parameters are specified in the
 * process definition
 *
 * @author Tareq Nabeel
 */
public class FileService implements IService {
    public enum FileOperation {
        DELETE, COPY, CREATE, MOVE, EXTRACT, UPLOAD
    }

    private Logger logger;

    private List<File> sourceDirs = new ArrayList<File>();
    private Pattern sourceFilePattern;
    private FileOperation fileOperation;

    public FileService() {
    }

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            init(processData);
            switch (fileOperation) {
                case DELETE:
                    deleteFiles();
                    break;
                case UPLOAD:
                    uploadFile(processData);
                    break;
                case COPY:
                    copyFile(processData);
                    break;
                default:
                    throw new IllegalArgumentException("Operation " + fileOperation + " is not supported at this time");
            }
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
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
        String tmpSourceDir = ProcessDataHelper.getTask(processData).getParameter("sourceDir");
        // If the task has the source dir, use it
        if (null == tmpSourceDir || "".equals(tmpSourceDir)) {
            setSourceDir(processData);
        }
        // else look in the *.process file for it
        else {
            sourceDirs.add(FileUtil.checkFileExists(tmpSourceDir));
        }
        // The file pattern to match on within each sourceDir
        String tmpSourceFilePattern = (String) processData.getItem("sourceFilePattern");
        if (null != tmpSourceFilePattern) {
            sourceFilePattern = Pattern.compile(tmpSourceFilePattern);
        }

        setMode(processData);
    }

    private void setSourceDir(IProcessData processData) throws MissingDataException, IOException {
        // One or more source directories must be supplied as input
        Object sourceDirObj = processData.getMandatoryItem("sourceDir");
        if (sourceDirObj instanceof String) {
            sourceDirs.add(FileUtil.checkFileExists((String) sourceDirObj));
        }
        else if (sourceDirObj instanceof File) {
            sourceDirs.add((File) sourceDirObj);
        }
        else if (sourceDirObj instanceof List) {
            sourceDirs = (List<File>) sourceDirObj;
        }
    }

    private void setMode(IProcessData processData) throws MissingDataException {
        String operation = (String) processData.getMandatoryItem("mode");
        if (FileOperation.DELETE.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.DELETE;
        }
        else if (FileOperation.COPY.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.COPY;
        }
        else if (FileOperation.MOVE.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.MOVE;
        }
        else if (FileOperation.EXTRACT.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.EXTRACT;
        }
        else if (FileOperation.CREATE.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.CREATE;
        }
        else if (FileOperation.UPLOAD.toString().equalsIgnoreCase(operation)) {
            this.fileOperation = FileOperation.UPLOAD;
        }
        else {
            throw new IllegalArgumentException("Invalid mode: " + operation + ".  Valid modes include: " + Arrays.toString(FileOperation.values()));
        }
    }

    private void deleteFiles() throws ZipException, IOException {
        for (File sourceDir : sourceDirs) {
            File[] sourceFiles = sourceDir.listFiles(new SourceFileFilter());
            if (sourceFiles != null) {
                int numOfFilesDeleted = 0;
                int numOfDirsDeleted = 0;
                for (File sourceFile : sourceFiles) {
                    if (sourceFile.isFile()) {
                        numOfFilesDeleted = sourceFile.delete() ? ++numOfFilesDeleted : numOfFilesDeleted;
                    }
                    else {
                        numOfDirsDeleted = FileUtil.deleteDirectory(sourceFile) ? ++numOfDirsDeleted : numOfDirsDeleted;
                    }
                }
                if (logger.isInfoEnabled()) {
                    logger.info("Deleted " + numOfFilesDeleted + " files and " + numOfDirsDeleted + " dirs in " + sourceDir.getAbsolutePath());
                }
            }
        }
    }

    private void uploadFile(IProcessData processData) throws Exception {
        ComputeDAO computeDAO = new ComputeDAO();
        UploadFileTask uploadTask = (UploadFileTask) ProcessDataHelper.getTask(processData);
        String sessionName = ProcessDataHelper.getSessionRelativePath(processData);
        String path = uploadTask.getPathToOriginalFile();
        if (null == uploadTask.getOwner()) {
            throw new MissingDataException("Unable to find the owner in the system.");
        }
        if (null == path || "".equals(path)) {
            throw new MissingDataException("No path to the file was provided.");
        }
        File tmpFile = new File(path);
        if (!tmpFile.exists()) {
            throw new MissingDataException("The file " + path + " does not exist or is unreachable.");
        }
        FileNode node;
        String newFileName;

        if (FastaUtil.isFastaFile(path)) {
            FastaFile fastaFile = new FastaFile(tmpFile);
            long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(fastaFile);
            String tmpSeqType = FastaUtil.determineSequenceType(fastaFile);
            node = new FastaFileNode(uploadTask.getOwner(), uploadTask, tmpFile.getName(), "", Node.VISIBILITY_PRIVATE,
                    tmpSeqType, (int) sequenceCountAndTotalLength[0], sessionName);
            node.setLength(sequenceCountAndTotalLength[1]);
            newFileName = tmpSeqType + ".fasta";
        }
        else {
            throw new Exception("Currently, the system does not support files of this(" +
                    path.substring(path.lastIndexOf('.') + 1) + ") type.");
        }
        computeDAO.saveOrUpdate(node);
        FileUtil.ensureDirExists(node.getDirectoryPath());
        FileUtil.cleanDirectory(node.getDirectoryPath());
        FileUtil.copyFile(tmpFile.getAbsolutePath(), node.getDirectoryPath() + File.separator + newFileName);
    }

    private class SourceFileFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return sourceFilePattern.matcher(name).matches();
        }
    }

    private void copyFile(IProcessData processData) throws MissingDataException, IOException {
        CopyFileTask copyTask = (CopyFileTask) ProcessDataHelper.getTask(processData);
        File source = new File(copyTask.getPathToOriginal());
        File destination = new File(copyTask.getPathToDestination());
        if (source.isDirectory() && destination.isDirectory()) {
            FileUtil.copyDirectory(source.getAbsolutePath(), destination.getAbsolutePath());
            logger.debug("FileService - Directory copied from " + source + " to " + destination);
            return;
        }
        if (source.isFile() && destination.canWrite()) {
            FileUtil.copyFile(source, destination);
            logger.debug("FileService - File copied from " + source + " to " + destination);
        }

    }

}
