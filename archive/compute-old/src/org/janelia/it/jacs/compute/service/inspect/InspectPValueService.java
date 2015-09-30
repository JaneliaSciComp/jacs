
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
import org.janelia.it.jacs.model.user_data.inspect.InspectResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;

/**
 * @author Todd Safford
 */
public class InspectPValueService implements IService {

    Long resultNodeId;

    public void execute(IProcessData processData) throws CreateRecruitmentFileNodeException {
        try {
            Logger _logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            Task task = ProcessDataHelper.getTask(processData);
            FileNode resultFileNode = ProcessDataHelper.getResultFileNode(processData);
            String tmpName=task.getParameter(InspectTask.PARAM_archiveFilePath);
            // Cut off any extra slash.  We only want the "bug" name
            if (tmpName.endsWith("/")){ tmpName=tmpName.substring(0,tmpName.length()-1); }
            String critterName = tmpName.substring(tmpName.lastIndexOf("/")+1);
            // Things are stored in archive due to size, so go grab it for execution
            String depotLocation=resultFileNode.getDirectoryPath()+File.separator+critterName;
            File resultsXDir = new File(depotLocation+File.separator+"ResultsX");
            File pepNovoDir = new File(resultsXDir.getAbsolutePath()+File.separator+InspectResultNode.TAG_PEPNOVO_DIR);
            File msgfDir = new File(resultsXDir.getAbsolutePath()+File.separator+ InspectResultNode.TAG_MSGF_DIR);
            File pvalueDir = new File(resultsXDir.getAbsolutePath()+File.separator+InspectResultNode.TAG_PVALUE_DIR);
            FileUtil.ensureDirExists(pvalueDir.getAbsolutePath());
            FileUtil.ensureDirExists(msgfDir.getAbsolutePath());

            String ldLib = "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/packages/boost-1.43.0/lib:/usr/local/packages/atlas/lib;";
            String pythonPath = SystemConfigurationProperties.getString("Python.Path");
            // todo Need to not call this thing a perl path
            String codePath = SystemConfigurationProperties.getString("Executables.ModuleBase");
            // Example archive path /usr/local/archive/projects/PGP/Arthrobacter.FB24/
            String pipelineCmd = ldLib + pythonPath + " "+ codePath +SystemConfigurationProperties.getString("Inspect.PValueCmd");
            String fullCmd = pipelineCmd +" -r "+pepNovoDir.getAbsolutePath() +" -w . -p 0.1 -S 0.5 -1 -H &> pvalue.log";

            SystemCall call = new SystemCall(_logger);
            int success = call.emulateCommandLine(fullCmd, true, null, pvalueDir);
            if (success!=0) {
                throw new ServiceException("There was a problem running the Inspect Data Prep Service.");
            }
        }
        catch (Exception e) {
            throw new CreateRecruitmentFileNodeException(e);
        }
    }

}