
package org.janelia.it.jacs.compute.service.blast.merge;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.blast.BlastServiceUtil;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.PartitionList;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.BlastSharedUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 2, 2008
 * Time: 1:54:08 PM
 */
public class GridMergeSortResultsService extends SubmitDrmaaJobService {

    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMaximumMemoryMB");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    public static final String GRID_MERGE_PROCESSOR = SystemConfigurationProperties.getString("BlastServer.GridMergeSortProcessor");
    public static final String GRID_MERGE_QUEUE = SystemConfigurationProperties.getString("BlastServer.GridMergeQueue");
    private static int MAX_HITS_PER_EXEC = SystemConfigurationProperties.getInt("BlastServer.MaxTotalHitsPerBlastExec");

    private Integer numberOfHitsToKeep;
    private PartitionList partitionList;
    private List<File> blastDestOutputDirList;
    String totalHitCountFilePath;
    long hitsPerQuery;

    public GridMergeSortResultsService() {
    }

    @Override
    public void postProcess() throws MissingDataException {
        RandomAccessFile totalBlastHitsFile = null;
        try {
            FileUtil.waitForFile(totalHitCountFilePath);
            totalBlastHitsFile = new RandomAccessFile(totalHitCountFilePath, "r");
            long totalBlastHits = BlastSharedUtil.retrieveTotalBlastHitsCount(totalBlastHitsFile);
            processData.putItem(BlastProcessDataConstants.TOTAL_BLAST_HITS, totalBlastHits);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new MissingDataException(e.getMessage());
        }
        finally {
            if (null != totalBlastHitsFile) {
                try {
                    totalBlastHitsFile.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void init(IProcessData processData) throws Exception {
        BlastTask blastTask = (BlastTask) ProcessDataHelper.getTask(processData);
        blastDestOutputDirList = (List<File>) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_DEST_OUTPUT_DIR);
        partitionList = BlastServiceUtil.getPartitionList(processData, blastTask);
        BlastResultFileNode resultFileNode = (BlastResultFileNode) ProcessDataHelper.getResultFileNode(processData);
        try {
            this.numberOfHitsToKeep = BlastServiceUtil.getNumberOfHitsToKeepFromBlastTask(blastTask);
        }
        catch (Exception ex) {
            throw new MissingDataException(ex.toString());
        }
        totalHitCountFilePath = resultFileNode.getFilePathByTag(BlastResultFileNode.TAG_TOTAL_BLAST_HITS);
        super.init(processData);
    }

    private int getNumberOfQueryEntries(File blastDestOutputDir) throws IOException {
        String seqCountFilePath = blastDestOutputDir.getAbsolutePath() + File.separator + "seqCount";
        logger.debug("GridMergeSortResultsService getNumberOfQueryEntries seqCountFilePath=" + seqCountFilePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(seqCountFilePath)));
        int seqCount;
        try {
            seqCount = Integer.parseInt(br.readLine());
        }
        finally {
            br.close();
        }
        return seqCount;
    }

    protected String getGridServicePrefixName() {
        return "blastGridMerge_";
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Setting grid merge queue to " + GRID_MERGE_QUEUE);
        jt.setNativeSpecification(GRID_MERGE_QUEUE);
    }

    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        logger.debug("\nGridMergeSortResultsService createJobScriptAndConfigurationFiles started");
        StringBuffer script = new StringBuffer();
        script.append("read BLAST_DEST_OUTPUT_DIR\n");
        script.append("read PARTITION_LIST_SIZE\n");
        script.append("read HITS_PER_QUERY\n");
        script.append("read TOTAL_HIT_COUNT_FILE_PATH\n");
        script.append(JAVA_PATH).append(" -Xmx").append(JAVA_MAX_MEMORY).append("m -classpath ").append(GRID_JAR_PATH).append(" ")
                .append(GRID_MERGE_PROCESSOR)
                .append(" $BLAST_DEST_OUTPUT_DIR")
                .append(" $PARTITION_LIST_SIZE")
                .append(" $HITS_PER_QUERY")
                .append(" $TOTAL_HIT_COUNT_FILE_PATH");
        script.append("\n");
        writer.append(script.toString());
        writer.write("");
        int i = 1;
        for (File dir : blastDestOutputDirList) {
            if (!dir.exists())
                throw new MissingDataException("Could not find blast output directory=" + dir.getAbsolutePath());
            int numberOfQueryEntries;
            try {
                numberOfQueryEntries = getNumberOfQueryEntries(dir);
            }
            catch (IOException ie) {
                numberOfQueryEntries = 0;
            }
            hitsPerQuery = calculateMergeSortHitsPerQuery(numberOfQueryEntries, numberOfHitsToKeep);
            File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName() + "Configuration." + i);
            logger.debug("Writing GridMergeSortResultsService grid config file=" + configFile.getAbsolutePath());
            FileWriter configWriter = new FileWriter(configFile);
            configWriter.write(dir.getAbsolutePath() + "\n");
            configWriter.write(partitionList.size() + "\n");
            configWriter.write(hitsPerQuery + "\n");
            configWriter.write(totalHitCountFilePath + "\n");
            configWriter.close();
            i++;
        }
        setJobIncrementStop(blastDestOutputDirList.size());
        logger.debug("\nGridMergeSortResultsService createJobScriptAndConfigurationFiles complete");
    }

    public static int calculateMergeSortHitsPerQuery(int numberOfQueries, int numberOfHitsToKeep) {
        int mergeSortHitsPerQuery = numberOfHitsToKeep;
        if (MAX_HITS_PER_EXEC > 0) {
            if ((numberOfQueries * numberOfHitsToKeep) >= MAX_HITS_PER_EXEC) {
                mergeSortHitsPerQuery = MAX_HITS_PER_EXEC / numberOfQueries;
            }
            if (mergeSortHitsPerQuery <= 0)
                mergeSortHitsPerQuery = 1;
        }
        return mergeSortHitsPerQuery;
    }

}
