
package org.janelia.it.jacs.compute.service.metageno;

import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.MultiFastaSplitterService;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.shared.fasta.FastaFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2009
 * Time: 4:29:35 PM
 */
public class MetaGenoFastaSplitterService extends MultiFastaSplitterService {
    public static final String FASTA_ENTRIES_PER_PIPELINE = "FASTA_ENTRIES_PER_PIPELINE";

    private Task task;
    private ComputeDAO computeDAO;
    private static final int MAX_OUTPUT_SIZE = 1000000000; // 1gb
    private static final int MAX_NUMBER_OF_JOBS = 1000000000; // 1 billion, i.e., unlimited
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 0; // we don't need an output constraint

    public void execute(IProcessData processData) throws ServiceException {
        try {
            this.task = ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            int MAX_INPUT_ENTRIES_PER_JOB = new Integer((String) processData.getItem(FASTA_ENTRIES_PER_PIPELINE));
            File inputFile = getInputFileFromTask();
            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, 1); // keep calc dependent on other vars
            processData.putItem(FileServiceConstants.PARTITION_LIST, null); // null should be OK
            processData.putItem(FileServiceConstants.MAX_OUTPUT_SIZE, MAX_OUTPUT_SIZE);
            processData.putItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB, MAX_INPUT_ENTRIES_PER_JOB);
            processData.putItem(FileServiceConstants.MAX_NUMBER_OF_JOBS, MAX_NUMBER_OF_JOBS);
            processData.putItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE, XML_JUNK_SIZE_IN_OUTPUT);
            processData.putItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER, 1); // 1==no adjustment
            super.execute(processData);
            logger.info("Using MAX_INPUT_ENTRIES_PER_JOB=" + MAX_INPUT_ENTRIES_PER_JOB);
            List<File> inputFileList = (List<File>) processData.getItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
            logger.info("MetaGenoFastaSplitterService contains the following " + inputFileList.size() + " files:");
            for (File f : inputFileList) {
                logger.info("Path: " + f.getAbsolutePath());
            }
            processData.putItem("MG_INPUT_ARRAY", inputFileList);

            // Need to determine this info for normalizing blast
            FastaFile multiFastaHelper = new FastaFile(inputFile);
            int totalNucleotideCount = (int) multiFastaHelper.getSize().getBases();

            processData.putItem("TOTAL_BASE_COUNT", Integer.toString(totalNucleotideCount));
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private File getInputFileFromTask() throws ServiceException, IOException, InterruptedException {
        Long queryNodeId;
        if (task instanceof MetaGenoOrfCallerTask) {
            queryNodeId = Long.parseLong(task.getParameter(MetaGenoOrfCallerTask.PARAM_input_node_id));
        }
        else if (task instanceof MetaGenoAnnotationTask) {
            queryNodeId = Long.parseLong(task.getParameter(MetaGenoAnnotationTask.PARAM_input_node_id));
        }
        else {
            throw new ServiceException("Do not recognize task type=" + task.getDisplayName());
        }
        File inputFile;
        try {
            // This would be the norm
            inputFile = getFastaFile(queryNodeId);
        }
        catch (ClassCastException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return inputFile;
    }

    /**
     * Returns the original fasta file created by FileUploadController (either through upload or query entry on GUI)
     *
     * @param inputNodeId
     * @return
     * @throws ServiceException
     */
    private File getFastaFile(Long inputNodeId) throws ServiceException {
        FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
        if (inputNode == null) {
            logger.info("FastaFileNode with inputNodeId:" + inputNodeId + " does not exist");
            return null;
        }
        else {
            return new File(inputNode.getFastaFilePath());
        }
    }

}
