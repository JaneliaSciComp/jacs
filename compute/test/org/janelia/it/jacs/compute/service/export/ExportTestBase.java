
package org.janelia.it.jacs.compute.service.export;

import junit.framework.TestCase;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.ExportRunner;
import org.janelia.it.jacs.compute.api.TaskServiceProperties;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.export.ExportTask;
import org.janelia.it.jacs.shared.utils.DateUtil;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jun 25, 2008
 * Time: 1:49:05 PM
 *
 */
public abstract class ExportTestBase extends TestCase {
    protected Map exportParameters = new HashMap();
    protected ComputeBeanRemote computeBean;
    private Long taskId;
    protected TaskServiceProperties exportProperties;

    private static final boolean CLEAN_DATA_AFTER_RUN = SystemConfigurationProperties.getBoolean("junit.test.cleanDataAfterRun");
    protected static final String TEST_USER_NAME = SystemConfigurationProperties.getString("junit.test.username");

    protected static final String OUTPUT_DIR = "Xcamtest/export/output/";
    protected static final String COMPARE_DIR = "Xcamtest/export/compare/";

    private long startTestTime;
    private static long startTime;
    private static String perfFileName;
    private Writer perfNumsWriter;

    protected List<String> readAccessionList=new ArrayList<String>();

    static {
        Calendar now = Calendar.getInstance();
        perfFileName = "./blastPerfNums"+String.valueOf(now.get(Calendar.YEAR)) + String.valueOf(now.get(Calendar.MONTH + 1)) + String.valueOf(now.get(Calendar.DAY_OF_MONTH)) + "_" + now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE) + now.get(Calendar.SECOND);
        startTime = System.currentTimeMillis();
    }

    public ExportTestBase() {
        super();
    }

    public ExportTestBase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        startTestTime = System.currentTimeMillis();
        exportProperties = new TaskServiceProperties(FileUtil.getResourceAsStream(getExportPropertiesFileName()));
        computeBean = EJBFactory.getRemoteComputeBean();
        perfNumsWriter = new FileWriter(FileUtil.ensureFileExists(perfFileName),true);
        populateTestData();
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

    protected String getExportPropertiesFileName() {
        return "export.parameters";
    }

    protected void submitJobAndWaitForCompletion(String processName, ExportTask exportTask) throws Exception {
        ExportRunner exportRunner = new ExportRunner();
        //exportRunner.setExportProperties(exportProperties); not presently supported
        exportProperties.put("process", processName);
        exportTask=(ExportTask)exportRunner.configureAndSaveTask(exportTask);
        taskId = exportTask.getObjectId();
        if (taskId==null)
            throw new Exception("taskId is null");
        exportRunner.run();
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

    public Long getTaskId() {
        return taskId;
    }

    private void populateTestData() {
        readAccessionList.add("JCVI_READ_1095913042005");
        readAccessionList.add("JCVI_READ_1095900120561");
    }

}