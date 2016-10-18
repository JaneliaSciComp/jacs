
package org.janelia.it.jacs.compute.service.blast.fastasplit;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.file.LockLessMultiFastaSplitterService;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * This service is responsible for splitting a multi fasta file into multiple query files.
 *
 * @author Tareq Nabeel
 */
public class BlastMultiFastaSplitterService extends LockLessMultiFastaSplitterService {

    protected FileNode resultFileNode;
    private long avgSubjLength;
    private static final int MAX_BLAST_OUTPUT_FILE_SIZE = (SystemConfigurationProperties.getInt("BlastServer.MaxOutputBlastFileSizeMB") * 1000000);
    private static final int MAX_QUERIES_PER_EXEC = SystemConfigurationProperties.getInt("BlastServer.MaxQueriesPerBlastExec");
    private static final int MAX_NUMBER_OF_JOBS = SystemConfigurationProperties.getInt("Grid.MaxNumberOfJobs");
    private static final int XML_JUNK_SIZE_IN_OUTPUT = 1000;
    private static final int MATCH_PLUS_BONDS_PLUS_MATE = 3;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            BlastTask blastTask = (BlastTask) ProcessDataHelper.getTask(processData);
            computeDAO = new ComputeDAO();
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            File inputFile = getInputFileFromTask(blastTask.getParameter(BlastTask.PARAM_query));

            this.avgSubjLength = getDatasetAvgSeqLength(processData, blastTask);
            // to be safe will use double of the average size
            this.avgSubjLength *= 2;

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
            super.execute(processData);
            List<File> inputFiles = (List<File>) processData.getItem(FileServiceConstants.POST_SPLIT_INPUT_FILE_LIST);
            processData.putItem(BlastProcessDataConstants.BLAST_FASTA_INPUT_FILE, inputFiles);
        }
        catch (ServiceException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    // calcutation of the split has to be based on smaller of the sequences
    protected long getAvgQueryLength(int queryCountInCurrentSplit, long nucleotideCountInCurrentSplit) {
        long avgQueryLen = super.getAvgQueryLength(queryCountInCurrentSplit, nucleotideCountInCurrentSplit);
        if (avgQueryLen < avgSubjLength)
            return avgQueryLen;
        else
            return avgSubjLength;
    }

    private long getDatasetAvgSeqLength(IProcessData processData, Task task) throws IOException, MissingDataException {
        Logger logger = ProcessDataHelper.getLoggerForTask(processData, BlastServiceUtil.class);
        long totalLength = 0L;
        long totalSeqs = 0L;
        String[] databaseFileNodeIdList = task.getParameter(BlastTask.PARAM_subjectDatabases).split(",");
        if (null==databaseFileNodeIdList || 0==databaseFileNodeIdList.length){
            throw new MissingDataException("Cannot blast with no subject databases specified.");
        }
        for (String databaseFileNodeIdString : databaseFileNodeIdList) {
            BlastDatabaseFileNode bfn;
            try {
                bfn = new ComputeDAO().getBlastDatabaseFileNodeById(Long.parseLong(databaseFileNodeIdString));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (bfn == null) {
                throw new RuntimeException("Could not find BlastDatabaseFileNode Id: " + databaseFileNodeIdString);
            }
            if (bfn.getLength() == 0L || bfn.getSequenceCount() == 0L)
                throw new RuntimeException("BlastDatabaseFileNode Id: " + databaseFileNodeIdString + " does not have valid length of # of sequences");

            totalLength += bfn.getLength();
            totalSeqs += bfn.getSequenceCount();
        }

        // using top aproximation
        return ((totalLength / totalSeqs + 1) * totalSeqs) / totalSeqs;
    }

}
