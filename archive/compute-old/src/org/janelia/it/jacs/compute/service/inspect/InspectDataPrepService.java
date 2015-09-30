
package org.janelia.it.jacs.compute.service.inspect;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.recruitment.CreateRecruitmentFileNodeException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.inspect.InspectTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;

/**
 * @author Todd Safford
 */
public class InspectDataPrepService implements IService {

    Long resultNodeId;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            Logger _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            FileNode resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            String ldLib = "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/packages/boost-1.43.0/lib;";
            String pythonPath = SystemConfigurationProperties.getString("Python.Path");
            // todo Need to not call this thing a perl path
            String codePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
            // Example archive path /usr/local/archive/projects/PGP/Arthrobacter.FB24/
            String pipelineCmd = ldLib + pythonPath + " "+ codePath +SystemConfigurationProperties.getString("InspectPrep.Cmd");
            String fullCmd = pipelineCmd + " -t "+ task.getParameter(InspectTask.PARAM_archiveFilePath)+" -s "+resultFileNode.getDirectoryPath();

            SystemCall call = new SystemCall(_logger);
            int success = call.emulateCommandLine(fullCmd, true, null, new File(resultFileNode.getDirectoryPath()));
            if (success!=0) {
                throw new ServiceException("There was a problem running the Inspect Data Prep Service.");
            }
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

}