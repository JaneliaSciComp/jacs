
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This service is intended to prepare the process data objects for blasting.
 * User: tsafford
 * Date: Apr 23, 2010
 * Time: 2:10:29 PM
 */
public class SamplingBlastConfigService implements IService {
    private ComputeDAO computeDAO;
    private static final int MAX_BLAST_OUTPUT_FILE_SIZE = (SystemConfigurationProperties.getInt("BlastServer.MaxOutputBlastFileSizeMB") * 1000000);
    private static final int MAX_QUERIES_PER_EXEC = SystemConfigurationProperties.getInt("BlastServer.MaxQueriesPerBlastExec");
    private static final int MAX_NUMBER_OF_JOBS = SystemConfigurationProperties.getInt("Grid.MaxNumberOfJobs");
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 1000;
    private static final int MATCH_PLUS_BONDS_PLUS_MATE = 3;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            BlastTask blastTask = (BlastTask) ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO(logger);
            File inputFile = getInputFileFromTask(blastTask.getParameter(BlastTask.PARAM_query));

            processData.putItem(FileServiceConstants.INPUT_FILE, inputFile);
            int dbAlignmentsRequested = Integer.valueOf(blastTask.getParameter(BlastTask.PARAM_databaseAlignments));
            processData.putItem(FileServiceConstants.MAX_RESULTS_PER_JOB, dbAlignmentsRequested);
            PartitionList partitionList = BlastServiceUtil.getPartitionList(processData, blastTask);
            processData.putItem(FileServiceConstants.PARTITION_LIST, partitionList);
            processData.putItem(FileServiceConstants.MAX_OUTPUT_SIZE, MAX_BLAST_OUTPUT_FILE_SIZE);
            processData.putItem(FileServiceConstants.MAX_INPUT_ENTRIES_PER_JOB, MAX_QUERIES_PER_EXEC);
            processData.putItem(FileServiceConstants.MAX_NUMBER_OF_JOBS, MAX_NUMBER_OF_JOBS);
            processData.putItem(FileServiceConstants.OUTPUT_ADDITIONAL_SIZE, XML_JUNK_SIZE_IN_OUTPUT);
            processData.putItem(FileServiceConstants.PER_INPUT_ENTRY_SIZE_MULTIPLIER, MATCH_PLUS_BONDS_PLUS_MATE);
            FastaFileNode fastaNode = (FastaFileNode) computeDAO.getNodeById(Long.valueOf(blastTask.getParameter(BlastTask.PARAM_query)));
            File splitDir = new File(fastaNode.getDirectoryPath() + File.separator + "split");
            File[] splitFiles = splitDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("query") && name.endsWith(".fasta");
                }
            });
            List<File> inputFiles = new ArrayList<File>(Arrays.asList(splitFiles));
            processData.putItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST, inputFiles);
            processData.putItem(BlastProcessDataConstants.BLAST_FASTA_INPUT_FILE, inputFiles);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected File getInputFileFromTask(String queryNodeId) throws ServiceException, IOException, InterruptedException {
        Long inputNodeId = Long.parseLong(queryNodeId);
        File inputFile;
        try {
            FastaFileNode inputNode = (FastaFileNode) computeDAO.genericGet(FastaFileNode.class, inputNodeId);
            if (inputNode == null) {
                throw new ServiceException("Could not find the FASTA file required.");
            }
            else {
                inputFile = new File(inputNode.getFastaFilePath());
            }
        }
        catch (ClassCastException e) {
            throw new ServiceException(e.getMessage(), e);
        }
        return inputFile;
    }


}
