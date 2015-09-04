
package org.janelia.it.jacs.compute.service.reversePsiBlast;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.file.FileServiceConstants;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.psiBlast.ReversePsiBlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.user_data.reversePsiBlast.ReversePsiBlastResultNode;
import org.janelia.it.jacs.shared.blast.BlastGridContinuousMergeSort;
import org.janelia.it.jacs.shared.blast.BlastResultCollectionConverter;
import org.janelia.it.jacs.shared.blast.BlastSharedUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Apr 14, 2008
 * Time: 3:41:46 PM
 */
public class PersistFinalXMLResultsGridService extends SubmitDrmaaJobService {

    private List<File> blastDestOutputDirs;
    private ReversePsiBlastResultNode resultFileNode;
    // private Long totalBlastHits;
    private String[] formats;

    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("BlastServer.GridMergeSortMaximumMemoryMB");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    public static final String GRID_PERSISTXML_PROCESSOR = SystemConfigurationProperties.getString("BlastServer.GridPeristsBlastResultProcessor");
    public static final String GRID_PERSISTXML_QUEUE = SystemConfigurationProperties.getString("BlastServer.GridPersistXMLQueue");
    public static boolean USE_SYSTEM_CONCAT = SystemConfigurationProperties.getBoolean("BlastServer.GridPersistXMLUseSystemConcat");

    private static final String resultFilename = SystemConfigurationProperties.getString("RpsBlast.ResultName");

    public PersistFinalXMLResultsGridService() {
    }

    public void init(IProcessData processData) throws Exception {
        blastDestOutputDirs = (List<File>) processData.getMandatoryItem(FileServiceConstants.OUTPUT_DIR_LIST);
        // totalBlastHits = (Long) processData.getMandatoryItem(BlastProcessDataConstants.TOTAL_BLAST_HITS);
        resultFileNode = (ReversePsiBlastResultNode) ProcessDataHelper.getResultFileNode(processData);
        ReversePsiBlastTask blastTask = (ReversePsiBlastTask) ProcessDataHelper.getTask(processData);
        formats = new String[]{"xml", "btab"};
        for (File dir : blastDestOutputDirs) {
            try {
                BlastGridContinuousMergeSort.copyBlastTaskToDir((IBlastOutputFormatTask)blastTask, dir);
            }
            catch (Exception e) {
                throw new MissingDataException(e.getMessage());
            }
        }
        super.init(processData);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        jt.setNativeSpecification(GRID_PERSISTXML_QUEUE);
    }

    protected String getGridServicePrefixName() {
        return "persistXML_";
    }

    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        logger.debug("\nPersistFinalXMLResultsGridService createJobScriptAndConfigurationFiles started");
        StringBuffer script = new StringBuffer();
        script.append("read BLAST_OUTPUT_DIR\n");
        script.append("read START_ITERATION\n");
        script.append("read EXPECTED_ITERATIONS\n");
        script.append("read WRITE_TOP\n");
        script.append("read WRITE_BOTTOM\n");
        script.append("read FORMATS\n");
        script.append(JAVA_PATH).append(" -Xmx").append(JAVA_MAX_MEMORY).append("m -classpath ").append(GRID_JAR_PATH).append(" ")
                .append(GRID_PERSISTXML_PROCESSOR).append(" ")
                .append(BlastResultCollectionConverter.KEY_BLAST_OUTPUT_DIR).append("=$BLAST_OUTPUT_DIR ")
                .append(BlastResultCollectionConverter.KEY_START_INTERATION).append("=$START_ITERATION ")
                .append(BlastResultCollectionConverter.KEY_EXPECTED_INTERATIONS).append("=$EXPECTED_ITERATIONS ")
                .append(BlastResultCollectionConverter.KEY_IS_FIRST).append("=$WRITE_TOP ")
                .append(BlastResultCollectionConverter.KEY_IS_LAST).append("=$WRITE_BOTTOM ")
                .append(BlastResultCollectionConverter.KEY_OUTPUT_FORMATS).append("=$FORMATS ");
        script.append("\n");
        writer.append(script.toString());
        List<Long> queryCountList = getQueryCounts();
        int i = 1;
        for (File dir : blastDestOutputDirs) {
            File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName() + "Configuration." + i);
            logger.debug("Writing PersistFinalXMLResultsGridService grid config file=" + configFile.getAbsolutePath());
            FileWriter configWriter = new FileWriter(configFile);
            try {
                configWriter.write(dir.getAbsolutePath() + "\n");
                configWriter.write("" + queryCountList.get((i - 1) * 2) + "\n");
                configWriter.write("" + queryCountList.get((i - 1) * 2 + 1) + "\n");
                if (i == 1) {
                    configWriter.write("yes\n");
                }
                else {
                    configWriter.write("no\n");
                }
                if (i == blastDestOutputDirs.size()) {
                    configWriter.write("yes\n");
                }
                else {
                    configWriter.write("no\n");
                }
                for (int fi = 0; fi < formats.length; fi++) {
                    // add more formats here if needed as in "xml,btab,txt"
                    configWriter.write(formats[fi] + (fi == formats.length - 1 ? "\n" : ","));
                }
            }
            finally {
                configWriter.flush();
                configWriter.close();
            }
            i++;
        }
        setJobIncrementStop(blastDestOutputDirs.size());
        logger.debug("\nPersistFinalXMLResultsGridService createJobScriptAndConfigurationFiles complete");
    }

    // The values here are returned in pairs-per-directory <start> <num> <start> <num> ...
    protected List<Long> getQueryCounts() throws IOException {
        ArrayList<Long> queryList = new ArrayList<Long>();
        long currentStart = 0;
        for (File dir : blastDestOutputDirs) {
            File queryFile = new File(dir, "queryBlastHits");
            long qNum = BlastSharedUtil.readLongValueFromFile(queryFile);
            queryList.add(currentStart);
            queryList.add(qNum);
            currentStart = currentStart + qNum;
        }
        return queryList;
    }

    public void postProcess() throws MissingDataException {
        try {
            // store blast hits
            storeBlastHitCount();

            // concatinate results for each requested format
            for (String f : formats) {
                concatinateAndStoreResults(f);
            }
        }
        catch (Exception ex) {
            throw new MissingDataException(ex.getMessage());
        }
    }

    private void storeBlastHitCount() throws IOException, DaoException {
        long totalBlastHits = 0;

        for (File dir : blastDestOutputDirs) {
            // calcualte total blast hits based on individual blast hit results
            File blastHitsFile = new File(dir, BlastResultFileNode.TAG_TOTAL_BLAST_HITS);
            totalBlastHits += BlastSharedUtil.readLongValueFromFile(blastHitsFile);
        }

        // set and save the total hit count
        this.resultFileNode.setHitCount(totalBlastHits);
        new ComputeDAO(logger).genericSave(resultFileNode);
    }

    private void concatinateAndStoreResults(String format) throws IOException, MissingDataException, DaoException {
        List<File> sourceFiles = new ArrayList<File>();

        // pull together list of source data files to be concatinated
        for (File dir : blastDestOutputDirs) {
            File sourceFile = new File(dir, BlastResultFileNode.PARSED_BLAST_RESULTS_COLLECTION_BASENAME + "." + format);
            if (!sourceFile.exists()) {
                throw new MissingDataException("Could not find result file=" + sourceFile.getAbsolutePath());
            }
            sourceFiles.add(sourceFile);
        }

        // 2 different ways to concatinate
        // is this more correct
        // File resultFile = new File(resultFileNode.getDirectoryPath() + File.separator + resultFilename + "." + format);
        File resultFile = new File(resultFileNode.getDirectoryPath() + File.separator + "blastResults." + format);
        if (USE_SYSTEM_CONCAT) {
            FileUtil.concatFilesUsingSystemCall(sourceFiles, resultFile);
        }
        else {
            FileUtil.concatFiles(sourceFiles, resultFile);
        }
    }
}