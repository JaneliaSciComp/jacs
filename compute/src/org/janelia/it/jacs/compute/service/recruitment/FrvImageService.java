
package org.janelia.it.jacs.compute.service.recruitment;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.WaitForJobException;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerRecruitmentTask;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerTask;
import org.janelia.it.jacs.model.user_data.genome.GenomeProjectFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.shared.processors.recruitment.RecruitmentDataHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 14, 2007
 * Time: 10:11:38 AM
 */
public class FrvImageService extends SubmitDrmaaJobService {

    // Recruitment Viewer - Property-based values used by the command line executions
    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("RecruitmentViewer.JavaMaxMemory");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    public static final String RECRUITMENT_BASE_PATH = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("RecruitmentViewer.PerlBaseDir");
    public static final String RECRUITMENT_SAMPLE_FILE_NAME = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
    public static final String RECRUITMENT_DATA_HELPER = SystemConfigurationProperties.getString("RecruitmentViewer.processor");
    public static final int TILE_SIDE_PIXELS = SystemConfigurationProperties.getInt("RecruitmentViewer.TileSize");
    private static final String queueName = SystemConfigurationProperties.getString("RecruitmentViewer.ImageGenerationQueue");

    protected RecruitmentFileNode dataFileNode = null;

    public void init(IProcessData processData) throws Exception {
        this.task = ProcessDataHelper.getTask(processData);
        // There should at least be one RecruitmentResultFileNode since we are supped to update the images
        dataFileNode = (RecruitmentFileNode) this.task.getInputNodes().iterator().next();
        processData.putItem(ProcessDataConstants.RECRUITMENT_FILE_NODE, dataFileNode);
        super.init(processData);
    }

    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
//        Placed in process def
//        EJBFactory.getLocalComputeBean().updateTaskStatus(task.getObjectId(), Event.RUNNING_EVENT,
//                "Creating run script for FrvImageService, task " + task.getObjectId());
        logger.debug("\nFrvImageService createJobScriptAndConfigurationFiles started");
        // Format the executable script
        String tmpSampleList = checkValueForCommandLine(((RecruitmentViewerFilterDataTask) task).getSampleListAsCommaSeparatedString());
        String tmpMateSpan = checkValueForCommandLine(((RecruitmentViewerFilterDataTask) task).getMateSpanPoint());
        String tmpAnnotFilter = checkValueForCommandLine(((RecruitmentViewerFilterDataTask) task).getAnnotationFilterString());
        String tmpColorizationType = checkValueForCommandLine(((RecruitmentViewerFilterDataTask) task).getColorizationType());

        // Get the Genome Project Genbank info if it exists
        String tmpGenbankFile = task.getParameter(RecruitmentViewerRecruitmentTask.GENBANK_FILE_NAME);
        String gpNodeId = task.getParameter(RecruitmentViewerTask.GENOME_PROJECT_NODE_ID);
        String gpNodePath;
        if (null != gpNodeId && !"".equals(gpNodeId) && null != tmpGenbankFile && !"".equals(tmpGenbankFile)) {
            GenomeProjectFileNode gpNode = (GenomeProjectFileNode) computeDAO.getNodeById(Long.valueOf(gpNodeId));
            gpNodePath = gpNode.getDirectoryPath() + File.separator + tmpGenbankFile;
        }
        else {
            gpNodePath = RecruitmentDataHelper.EMPTY_VALUE;
        }
        // Running image generation in bulk - one node makes all the images
        // Leaving jobIncrementStop at the default of 1
        StringBuffer script = new StringBuffer();
        script.append(JAVA_PATH).append(" -Xmx").append(JAVA_MAX_MEMORY).append("m -classpath ").append(GRID_JAR_PATH).append(" ")
                .append(RECRUITMENT_DATA_HELPER)
                .append(" -src ").append(dataFileNode.getDirectoryPath())
                .append(" -out ").append(resultFileNode.getDirectoryPath())
                .append(" -genbankFile ").append(gpNodePath)
                .append(" -samplePath ").append(RECRUITMENT_BASE_PATH).append(File.separator).append(RECRUITMENT_SAMPLE_FILE_NAME)
                .append(" -sampleList ").append(tmpSampleList)
                .append(" -pidMin ").append((Double.parseDouble(task.getParameterVO(RecruitmentViewerFilterDataTask.PERCENT_ID_MIN).getStringValue())))
                .append(" -pidMax ").append((Double.parseDouble(task.getParameterVO(RecruitmentViewerFilterDataTask.PERCENT_ID_MAX).getStringValue())))
                .append(" -refBegin ").append(task.getParameter(RecruitmentViewerFilterDataTask.REF_BEGIN_COORD))
                .append(" -refEnd ").append(task.getParameter(RecruitmentViewerFilterDataTask.REF_END_COORD))
                .append(" -mateBits ").append(task.getParameter(RecruitmentViewerFilterDataTask.MATE_BITS))
                .append(" -annotFilter \"").append(tmpAnnotFilter).append("\"")
                .append(" -mateSpanPoint ").append(tmpMateSpan)
                .append(" -colorizationType ").append(tmpColorizationType);
        script.append("\n");
        writer.append(script.toString());

        File configFile = new File(getSGEConfigurationDirectory(), getGridServicePrefixName() + "Configuration.1");
        FileWriter configWriter = new FileWriter(configFile);
        writer.write("");
        configWriter.close();
        logger.debug("\nFrvImageService createJobScriptAndConfigurationFiles complete");
    }

    private String checkValueForCommandLine(String paramValue) {
        if (null == paramValue || "".equals(paramValue)) return RecruitmentDataHelper.EMPTY_VALUE;
        else return paramValue;
    }

    protected String getGridServicePrefixName() {
        return "rvImage";
    }

    @Override
    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("setQueue = " + queueName);
        jt.setNativeSpecification(queueName);
    }

    /**
     * Overriding the base class method as I need to save the number of recruited reads, once the grid processing is complete
     *
     * @throws DrmaaException       - problem submitting a job to the grid
     * @throws DaoException         - problem saving the recruitment data to the database
     * @throws IOException          - unable to access the num hits file
     * @throws MissingDataException - data required for processing was not found
     * @throws WaitForJobException  - error waiting for the grid job to complete
     * @throws InterruptedException - error waiting for the grid job to complete
     * @throws ParameterException
     */
    protected Set<String> submitJob() throws Exception {
        // Call the base class to do all the grid work with waitforJobCompletion flag to true
        return super.submitJob();
    }

}
