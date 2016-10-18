
package org.janelia.it.jacs.compute.service.blast;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.MultiSelectVO;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.blast.ParsedBlastResultCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * This class contains methods used by more than one Blast service
 *
 * @author Tareq Nabeel
 */
public class BlastServiceUtil {

    private static int MAX_HITS_PARAM = SystemConfigurationProperties.getInt("BlastServer.MaxHitsToKeepParameter");

    public static PartitionList getPartitionList(IProcessData processData, Task task) throws IOException, MissingDataException {
        PartitionList partitionList = (PartitionList) processData.getItem(BlastProcessDataConstants.BLAST_PARTITION_LIST);
        if (partitionList == null) {
            partitionList = new PartitionList();
            populatePartitionFileList(processData, partitionList, task);
        }
        return partitionList;
    }

    // This method returns the total length, in peptides or nucleotides as appropriate, of the databases selected.
    private static void populatePartitionFileList(IProcessData processData, PartitionList partitionList, Task task) throws IOException, MissingDataException {
        if (task == null)
            throw new RuntimeException("Task must not be null");
        Logger logger = ProcessDataHelper.getLoggerForTask(processData, BlastServiceUtil.class);
        Long totalLength = 0L;
        MultiSelectVO databaseFileNodeList = null;
        try {
            databaseFileNodeList = (MultiSelectVO) task.getParameterVO(BlastTask.PARAM_subjectDatabases);
        }
        catch (ParameterException e) {
            logger.error(e, e);
        }
        String[] databaseFileNodeIdList = databaseFileNodeList.getValuesAsStringArray();
        if (logger.isInfoEnabled()) {
            logger.info("Received list with " + databaseFileNodeIdList.length + " members.");
        }
        for (String databaseFileNodeIdString : databaseFileNodeIdList) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding files from db=" + databaseFileNodeIdString);
            }
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

            logger.debug("Adding partitions=0 to " + (bfn.getPartitionCount() - 1));
            for (int i = 0; i < bfn.getPartitionCount(); i++) {
                File partitionFile = new File(bfn.getDirectoryPath() + "/" +
                        BlastDatabaseFileNode.PARTITION_PREFIX + "_" + i + ".fasta");
                partitionList.add(partitionFile);
            }
            totalLength += bfn.getLength();
        }
        logger.info("Using " + partitionList.size() + " BLAST DB partitions. Total length = " + totalLength);
        partitionList.setDatabaseLength(totalLength);
        processData.putItem(BlastProcessDataConstants.BLAST_PARTITION_LIST, partitionList);
    }

    public static int getNumberOfHitsToKeepFromBlastTask(BlastTask blastTask) throws ParameterException {
        try {
            LongParameterVO lvo = (LongParameterVO) blastTask.getParameterVO(BlastTask.PARAM_databaseAlignments);
            int numberOfHitsToKeep = lvo.getActualValue().intValue();
            if (MAX_HITS_PARAM > 0 && numberOfHitsToKeep > MAX_HITS_PARAM) {
                numberOfHitsToKeep = MAX_HITS_PARAM;
            }
            return numberOfHitsToKeep;
        }
        catch (Exception ex) {
            throw new ParameterException("Could not determine PARAM_databaseAlignments from blastTask=" + blastTask.getObjectId());
        }
    }

    /**
     * ParsedBlastResultCollection is serialized to file system by MergeSortService because we're planning on moving MergeSort to grid
     * and because run PersistBlastResults Node or PersistBlastResultsXML services after MergeSort is done
     * @param blastDestOutputDir
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ParsedBlastResultCollection deserializeParsedBlastResultCollection(File blastDestOutputDir) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(blastDestOutputDir.getAbsolutePath()+File.separator+"parsedBlastResultsCollection.oos"));
        ParsedBlastResultCollection parsedBlastResultCollection;
        try {
            parsedBlastResultCollection = (ParsedBlastResultCollection) ois.readObject();
        }
        finally {
            ois.close();
        }
        return parsedBlastResultCollection;
    }

}
