
package org.janelia.it.jacs.compute.service.genomeProject;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.genomeProject.GenomeProjectImportTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.genbank.GenbankFile;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

/**
 * @author Todd Safford
 */
public class CreateGenomeProjectFileNodeService implements IService {

    private Task task;
    private String sessionName;
    private ComputeDAO computeDAO;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            computeDAO = new ComputeDAO(logger);
            GenomeProjectFileNode gpNode = createFileNode();
            String pathToOriginalData = task.getParameter(GenomeProjectImportTask.PARAM_PATH_TO_SOURCE_DATA);
            FileUtil.copyDirectory(pathToOriginalData, gpNode.getDirectoryPath());
            processData.putItem(ProcessDataConstants.INPUT_FILE_NODE_ID, gpNode.getObjectId());
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

    private GenomeProjectFileNode createFileNode() throws Exception {
        // if we get this far then we assume that no result persist exists and create one
        GenomeProjectImportTask importTask = (GenomeProjectImportTask) computeDAO.getTaskById(task.getObjectId());
        // This code block assumes a previous run died unexpectedly.  In that case, we delete all the old stuff
        // Physically delete the files but not the directory
        Set<Node> outputNodes = importTask.getOutputNodes();
        for (Node node : outputNodes) {
            if (node instanceof GenomeProjectFileNode) {
                return (GenomeProjectFileNode) node;
            }
        }
        String visibility = Node.VISIBILITY_PRIVATE;
        if (User.SYSTEM_USER_LOGIN.equalsIgnoreCase(importTask.getOwner())) {
            visibility = Node.VISIBILITY_PUBLIC;
        }
        File[] sourceFiles = new File(task.getParameter(GenomeProjectImportTask.PARAM_PATH_TO_SOURCE_DATA)).listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(GenomeProjectFileNode.GENBANK_FILE_EXTENSION);
                    }
                }
        );
        if (null == sourceFiles || 0 == sourceFiles.length) {
            throw new ServiceException("Unable to find source files for CreateGenomeProjectFileNodeService");
        }
        GenbankFile tmpGBK = new GenbankFile(sourceFiles[0].getAbsolutePath());
        GenomeProjectFileNode gpFileNode = new GenomeProjectFileNode(importTask.getOwner(), importTask, importTask.getParameter(GenomeProjectImportTask.PARAM_GENOME_PROJECT_NAME),
                "GenomeProjectFileNode for createtask " + importTask.getObjectId(), visibility,
                tmpGBK.getKingdom(), importTask.getParameter(GenomeProjectImportTask.PARAM_GENOME_PROJECT_STATUS),
                sessionName);

        computeDAO.saveOrUpdate(gpFileNode);

        FileUtil.ensureDirExists(gpFileNode.getDirectoryPath());
        FileUtil.cleanDirectory(gpFileNode.getDirectoryPath());
        return gpFileNode;
    }

}