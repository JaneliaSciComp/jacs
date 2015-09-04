
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkPipelineBaseTask;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.GenericFileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 8, 2010
 * Time: 10:18:16 PM
 */
public abstract class ProkAnnotationBaseService extends SubmitDrmaaJobService {
    private static final String temporaryDirectory = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    protected static final String perlPath = SystemConfigurationProperties.getString("Perl.Path");
    protected static final String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");

    protected String _databaseUser;
    protected String _databasePassword;
    protected String _targetDatabase;
    protected String _targetDirectory;
    protected Logger _logger;

    public void init(IProcessData processData) throws Exception {
        logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        this.processData = processData;
        // Permit the task to be predefined elsewhere
        if (this.task == null) {
            this.task = ProcessDataHelper.getTask(processData);
        }
        // Permit the resultNode to be defined elsewhere
        resultFileNode = new GenericFileNode(task.getOwner(), task, "","", FileNode.VISIBILITY_PRIVATE,
                FileNode.DIRECTORY_DATA_TYPE, null);
        resultFileNode.setPathOverride(temporaryDirectory+File.separator+task.getObjectId());
        this.jobSet = new HashSet<String>();
        // Needs to run in separate transaction
        if (computeDAO == null) {computeDAO = new ComputeDAO(logger);}

        // ensure the SGE dirs exist
        FileUtil.ensureDirExists(getSGEConfigurationDirectory());
        FileUtil.ensureDirExists(getSGEOutputDirectory());
        FileUtil.ensureDirExists(getSGEErrorDirectory());
        if (null != task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME) && !"".equals(task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME))) {
            this._databaseUser = task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the database username was undefined.");
        }
        if (null != task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD) && !"".equals(task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD))) {
            this._databasePassword = task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the database password was undefined.");
        }
        if (null != task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME) && !"".equals(task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME))) {
            this._targetDatabase = task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the target database was undefined.");
        }
        if (null != task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY) && !"".equals(task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY))) {
            this._targetDirectory = task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the target directory was undefined.");
        }
        this._logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        FileUtil.ensureDirExists(getLogFilePath());
    }

    @Override
    protected String getGridServicePrefixName() {
        return task.getTaskName();
    }

    @Override
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        String fullCmd = perlPath + " " + basePath + getCommandLine() + " &> " + getLogFilePath() + task.getTaskName() +
                "." + System.currentTimeMillis();
        fullCmd = "export PATH=$PATH:/usr/local/bin:/usr/local/common:/usr/bin:/bin:" + basePath + ";export PERL5LIB=$PERL5LIB:" + basePath + ";" + fullCmd + "\n";

        // todo Remove these env variables.  NOT A FAN OF THIS.
        writer.write("cd "+_targetDirectory+"\n");
        writer.write("export ANNOTATION_DIR=/usr/local/annotation\n");
        writer.write("export ANNOT_DEVEL=/usr/local/devel/ANNOTATION\n");
        writer.write("export DSQUERY=SYBTIGR\n");
        writer.write("export HMM_SCRIPTS=/usr/local/devel/ANNOTATION/hmm/bin\n");
        writer.write("export MICROBIAL_SCRIPTS=/usr/local/devel/ANNOTATION/microbial/bin\n");
        writer.write("export SYBASE=/usr/local/packages/sybase\n");
        writer.write("export LANG=en_US\n");
        writer.write(fullCmd);
        new File(getSGEConfigurationDirectory()+File.separator+task.getTaskName()+"Configuration.1").createNewFile();
        new File(getSGEOutputDirectory()+File.separator+task.getTaskName()+"Output").createNewFile();
        setJobIncrementStop(1);
    }

    public abstract String getCommandLine() throws ServiceException;

    public String getServiceName() {
        return this.getClass().getSimpleName();
    }

    // NOTE:  This really isn't the best coupling.  The ProkAnnotation.DefaultGridCode should be enough.
    public String getDefaultProjectCode() {
        String defaultCode = SystemConfigurationProperties.getString("ProkAnnotation.DefaultGridCode");
        String finalCode = null;
        try {
            HashSet<String> codeList = EJBFactory.getLocalComputeBean().getProjectCodes();
            if (null == codeList || 0 == codeList.size()) {
                throw new ServiceException("Cannot obtain project code list!");
            }

            // If the code list doesn't have our supplied default grab anything.
            if (!codeList.contains(defaultCode)) {
                finalCode = codeList.iterator().next();
                _logger.error("The project code list does not contain the default code: " + defaultCode + ". Using: " + finalCode);
            }
            else {
                finalCode = defaultCode;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return finalCode;
    }

    public String getLogFilePath() {
        return _targetDirectory + File.separator + "logs" + File.separator;
    }

    @Override
    protected String getSGEQueue() {
        return "-l medium";
    }
    
}
