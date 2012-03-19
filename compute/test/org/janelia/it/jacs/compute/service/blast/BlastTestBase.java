
package org.janelia.it.jacs.compute.service.blast;

import junit.framework.TestCase;
import org.janelia.it.jacs.compute.api.BlastRunner;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.TaskServiceProperties;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.model.vo.LongParameterVO;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Apr 19, 2007
 * Time: 2:54:23 PM
 *
 */
public abstract class BlastTestBase extends TestCase {
    protected Map<String, ParameterVO> blastParameters = new HashMap<String, ParameterVO>();
    protected ComputeBeanRemote computeBean;
    private Long taskId;
    private Long expectedHits;
    private Integer expectedMessageCount;
    protected TaskServiceProperties blastProperties;
    private String expectedBlastResultsZipFilePath;

    private static final boolean CLEAN_DATA_AFTER_RUN = SystemConfigurationProperties.getBoolean("junit.test.cleanDataAfterRun");
    private static final String TEST_USER_NAME = SystemConfigurationProperties.getString("junit.test.username");

    protected static final String ALL_METAGENOMIC_READS = "All Metagenomic Sequence Reads (N)";
    protected static final String GOS_CHESAPEAKE_BAY = "GOS: move858 Assembled Sequences from 0.002-0.22 Chesapeake Bay (N)";
    protected static final String DEEP_MED = "DeepMed: All Metagenomic Sequence Reads (N)";
    protected static final String GOS_GS00a_SARGASSO_11_13 = "GOS: GS000a Reads from 0.1-0.8 Sargasso Sea Stations 11 and 13 (N)";
    protected static final String MARINE_VIROMES = "MarineViromes: All Metagenomic Sequence (454) Reads (N)";

    protected static final String INPUT_DIR = "Xcamtest/blast/input/";
    protected static final String COMPARE_DIR = "Xcamtest/blast/compare/";

    private long startTestTime;
    private static long startTime;
    private static String perfFileName;
    private Writer perfNumsWriter;

    static {
        Calendar now = Calendar.getInstance();
        perfFileName = "./blastPerfNums"+String.valueOf(now.get(Calendar.YEAR)) + String.valueOf(now.get(Calendar.MONTH + 1)) + String.valueOf(now.get(Calendar.DAY_OF_MONTH)) + "_" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) + now.get(Calendar.SECOND);
        startTime = System.currentTimeMillis();
    }

    public BlastTestBase() {
        super();
    }

    public BlastTestBase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        // Load blast parameter values
        startTestTime = System.currentTimeMillis();
        blastProperties = new TaskServiceProperties(FileUtil.getResourceAsStream(getBlastPropertiesFileName()));
        computeBean = EJBFactory.getRemoteComputeBean();
        blastProperties.put(BlastRunner.PARAM_USER_ID_KEY, TEST_USER_NAME);
        perfNumsWriter = new FileWriter(FileUtil.ensureFileExists(perfFileName),true);
    }

    /**
     * This runs after every test and connection creations is expensive.  It makes sense though
     * considering that one could run a single test that persists lots of data.
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        if (CLEAN_DATA_AFTER_RUN) {
            Class.forName(SystemConfigurationProperties.getString("jdbc.driverClassName"));
            Connection connection = DriverManager.getConnection(
                    SystemConfigurationProperties.getString("jdbc.url"),
                    SystemConfigurationProperties.getString("jdbc.username"),
                    SystemConfigurationProperties.getString("jdbc.password"));
            connection.setAutoCommit(false);
            PreparedStatement pstmt = connection.prepareStatement("select delete_user_data('" + TEST_USER_NAME + "',false)");
            pstmt.executeQuery();
            connection.commit();
            pstmt.close();
            connection.close();
        }
        String perfMsg = DateUtil.getElapsedTime(getClass().getSimpleName() + " " + this.getName() + " took: ", startTestTime, System.currentTimeMillis());
        perfMsg += DateUtil.getElapsedTime("   total: ",startTime, System.currentTimeMillis());
        perfNumsWriter.write(perfMsg);
        perfNumsWriter.write("\n");
        perfNumsWriter.close();
        startTestTime = 0;
    }

    protected String getBlastPropertiesFileName() {
        return "blast.parameters";
    }

    protected void submitJobAndWaitForCompletion(String processName) throws Exception {
        BlastRunner blastRunner = new BlastRunner();
        blastRunner.setBlastProperties(blastProperties);
        blastProperties.put("process", processName);
        Task task = blastRunner.init();
        taskId = task.getObjectId();
        blastRunner.run();
        waitAndVerifySuccessfulCompletion(taskId);
    }

    protected String waitAndVerifyCompletion(Long taskId) throws Exception {
        String[] statusTypeAndValue = computeBean.getTaskStatus(taskId);
        while (!Task.isDone(statusTypeAndValue[0])) {
            Thread.sleep(5000);
            statusTypeAndValue = computeBean.getTaskStatus(taskId);
        }
        System.out.println(statusTypeAndValue[1]);
        return statusTypeAndValue[0];
    }

    private void waitAndVerifySuccessfulCompletion(Long taskId) throws Exception {
        String status = waitAndVerifyCompletion(taskId);
        assertEquals(Event.COMPLETED_EVENT, status);
    }

    protected void verifyErrorCompletion() throws Exception {
        String status = waitAndVerifyCompletion(taskId);
        assertEquals(Event.ERROR_EVENT, status);
    }

    protected void validateHits() throws Exception {
        validateHits(taskId);
    }

    protected void validateMessageCount() throws Exception {
        validateMessageCount(taskId);
    }

    private void validateHits(Long taskId) throws Exception {
        Long blastHitCount = EJBFactory.getRemoteComputeBean().getBlastHitCountByTaskId(taskId);
        validateHitCount(blastHitCount);
    }

    protected void validateHitsAndBlastResultContent() throws Exception {
        validateHitsAndBlastResultContent(taskId, getExpectedBlastResultsZipFilePath());
    }

    private void validateHitsAndBlastResultContent(Long taskId, String expectedBlastResultsZipFilePath) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();

        BlastResultFileNode blastResultFileNode = computeBean.getBlastResultFileNodeByTaskId(taskId);
        validateHitCount(blastResultFileNode.getBlastHitCount());

        // File contents could be huge.  Do comparison on server
        computeBean.verifyBlastResultContents(blastResultFileNode.getObjectId(), expectedBlastResultsZipFilePath);
    }

    private void validateHitCount(Long actualBlastHitCount) {
        System.out.println("Received blastHitCount=" + actualBlastHitCount + " for taskId=" + taskId);
        assertEquals(getExpectedHits(), actualBlastHitCount);
    }

    protected void validateMessageCount(Long taskId) throws Exception {
        Task task = EJBFactory.getRemoteComputeBean().getTaskWithMessages(taskId);
        System.out.println("Received message count=" + task.getMessages().size() + " messages=" + task.getMessages() + " for taskId=" + taskId);
        assertEquals(getExpectedMessageCount(), new Integer(task.getMessages().size()));
    }

    public Long getExpectedHits() {
        return expectedHits;
    }

    public void setExpectedHits(Long expectedHits) {
        this.expectedHits = expectedHits;
    }

    public Integer getExpectedMessageCount() {
        return expectedMessageCount;
    }

    public void setExpectedMessageCount(Integer expectedMessageCount) {
        this.expectedMessageCount = expectedMessageCount;
    }

    public String getExpectedBlastResultsZipFilePath() {
        return expectedBlastResultsZipFilePath;
    }

    public void setExpectedBlastResultsZipFileName(String expectedBlastResultsZipFileName) {
        this.expectedBlastResultsZipFilePath = COMPARE_DIR + expectedBlastResultsZipFileName;
    }

    public void setBlastInputFileName(String blastInputFileName) {
        blastProperties.put(BlastRunner.PARAM_FASTAFILE_KEY, INPUT_DIR + blastInputFileName);
        blastProperties.remove(BlastRunner.PARAM_FASTATEXT_KEY);
    }

    public void setBlastInputFastaText(String fastatext) {
        blastProperties.put(BlastRunner.PARAM_FASTATEXT_KEY, fastatext);
        blastProperties.remove(BlastRunner.PARAM_FASTAFILE_KEY);
    }

    public void setBlastDatasetName(String datasetName) {
        blastProperties.put(BlastRunner.PARAM_DATASET_KEY, datasetName);
    }

    public Long getTaskId() {
        return taskId;
    }

    //--------------------------------------------------------------------------------------------------------------


    protected void submitSelfContainedBlastAndWaitForCompletion(String processName) throws Exception {
        Map processConfiguration = createBlastConfiguration(processName);
        taskId = computeBean.submitJob(processName, processConfiguration);
        waitAndVerifySuccessfulCompletion(taskId);
    }

    private Map createBlastConfiguration(String processName) {
        Map<String, Object> processConfiguration = new HashMap<String, Object>();
        blastProperties.put("process", processName);
        processConfiguration.put(BlastProcessDataConstants.BLAST_TYPE, blastProperties.getString(BlastRunner.PARAM_BLAST_TYPE_KEY));
        processConfiguration.put(IProcessData.JOB_NAME, processName);
        processConfiguration.put(BlastProcessDataConstants.DATA_SET_NAME, blastProperties.getString(BlastRunner.PARAM_DATASET_KEY));
        processConfiguration.put(IProcessData.USER_NAME, blastProperties.getString(BlastRunner.PARAM_USER_ID_KEY));
        processConfiguration.put(BlastProcessDataConstants.FAST_TEXT, blastProperties.getString(BlastRunner.PARAM_FASTATEXT_KEY));
        blastParameters.put(BlastTask.PARAM_evalue, new LongParameterVO(
                new Long("-100"), new Long("3"), blastProperties.getLong("evalue")));
        blastParameters.put(BlastTask.PARAM_databaseAlignments, new LongParameterVO(
                1l, (long) 50000, blastProperties.getLong("databaseAlignments")));
        processConfiguration.put(BlastProcessDataConstants.BLAST_PARAMETERS, this.blastParameters);
//        processConfiguration.putAll(TaskServiceProperties.getInstance());
        return processConfiguration;
    }


}
