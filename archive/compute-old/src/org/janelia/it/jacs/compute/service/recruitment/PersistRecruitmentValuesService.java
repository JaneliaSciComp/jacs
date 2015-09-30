
package org.janelia.it.jacs.compute.service.recruitment;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.recruitment.RecruitmentResultFileNode;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 2, 2009
 * Time: 3:35:04 PM
 */
public class PersistRecruitmentValuesService implements IService {
    protected Logger logger;
    protected Task task;
    protected IProcessData processData;
    protected FileNode resultFileNode;
    protected ComputeDAO computeDAO;

    @Override
    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            // Now save the number of hits which occurred to the db
            File tmpHitsFile = new File(resultFileNode.getDirectoryPath() + File.separator + RecruitmentResultFileNode.NUM_HITS_FILENAME);
            if (tmpHitsFile.exists()) {
                FileInputStream fis = new FileInputStream(tmpHitsFile);
                try {
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
                finally {
                    fis.close();
                }
            }
            else {
                logger.warn("FrvImageService - Could not find file: " + tmpHitsFile.getAbsolutePath());
            }
        }
        catch (Exception e) {
            logger.error("There was an error recording the recruitment values.", e);
            throw new ServiceException("There was an error recording the recruitment values.", e);
        }
    }

    protected void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.processData = processData;
        // Permit the task to be predefined elsewhere
        if (this.task == null) {
            this.task = ProcessDataHelper.getTask(processData);
        }
        // Permit the resultNode to be defined elsewhere
        if (this.resultFileNode == null) {
            this.resultFileNode = ProcessDataHelper.getResultFileNode(processData);
        }
        if (resultFileNode == null) {
            throw new MissingDataException("ResultFileNode for createtask " + task.getObjectId() +
                    " must exist before a grid job is submitted");
        }
        // Needs to run in separate transaction
        if (computeDAO == null)
            computeDAO = new ComputeDAO(logger);
    }


}
