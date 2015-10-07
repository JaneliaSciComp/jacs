
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.recruitment.RecruitmentViewerFilterDataTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentFileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jun 14, 2007
 * Time: 10:11:38 AM
 */
public abstract class FrvNonGridServiceBase implements IService {

    // Recruitment Viewer - Property-based values used by the command line executions
    public static final String JAVA_PATH = SystemConfigurationProperties.getString("Java.Path");
    public static final String JAVA_MAX_MEMORY = SystemConfigurationProperties.getString("RecruitmentViewer.JavaMaxMemory");
    public static final String GRID_JAR_PATH = SystemConfigurationProperties.getFilePath("Grid.Lib.Path", "Grid.Jar.Name");
    public static final String RECRUITMENT_SAMPLE_FILE_PATH = SystemConfigurationProperties.getString("RecruitmentViewer.SampleFile.Name");
    public static final String RECRUITMENT_DATA_HELPER = SystemConfigurationProperties.getString("RecruitmentViewer.processor");
    public static final int TILE_SIDE_PIXELS = SystemConfigurationProperties.getInt("RecruitmentViewer.TileSize");

    protected Logger logger;
    protected RecruitmentViewerFilterDataTask task;
    protected IProcessData processData;
    protected FileNode resultFileNode;
    protected Set<String> jobSet;
    protected ComputeDAO computeDAO;
    protected RecruitmentFileNode dataFileNode = null;

    protected void init(IProcessData processData) throws MissingDataException, IOException {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        computeDAO = new ComputeDAO(logger);
        this.task = (RecruitmentViewerFilterDataTask) ProcessDataHelper.getTask(processData);
        dataFileNode = (RecruitmentFileNode) this.task.getInputNodes().iterator().next();
        processData.putItem(ProcessDataConstants.RECRUITMENT_FILE_NODE, dataFileNode);
        // There should at least be one RecruitmentResultFileNode since we are supped to update the images
        this.resultFileNode = (RecruitmentResultFileNode) computeDAO.getResultNodeByTaskId(this.task.getObjectId());
        this.jobSet = new HashSet<String>();

        if (resultFileNode == null)
            throw new MissingDataException("ResultFileNode for createtask " + task.getObjectId() +
                    " must exist before a grid job is submitted");
    }

    public void execute(IProcessData processData) throws ServiceException {
        // Need to set the task and result node information before the Grid base class takes over
        try {
            init(processData);
            recruit();
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    protected abstract void recruit() throws IOException, ParameterException, InterruptedException;

    /**
     * Overriding the base class method as I need to save the number of recruited reads, once the grid processing is complete
     *
     * @throws java.io.IOException - unable to access the num hits file
     */
    protected void saveNumRecruitedReads() throws IOException {
        // Now save the number of hits which occurred to the db
        FileInputStream fis = new FileInputStream(resultFileNode.getDirectoryPath() + File.separator + RecruitmentResultFileNode.NUM_HITS_FILENAME);
        String tmpHits = "";
        int available = fis.available();
        if (available > 0) {
            byte[] tmpBytes = new byte[fis.available()];
            int bytesRead = fis.read(tmpBytes);
            if (available != bytesRead) {
                logger.warn("Something may be wrong.  Some bytes were not read from the tmpNumRecruited.txt file");
            }
            tmpHits = new String(tmpBytes).trim();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Node " + resultFileNode.getName() + " has " + tmpHits + " recruited hits.");
        }
        EJBFactory.getRemoteComputeBean().setRVHitsForNode(resultFileNode.getObjectId(), tmpHits);
    }

}