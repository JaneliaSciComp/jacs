
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.ProkPipelineBaseTask;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:39:32 PM
 */
public class BtabToMultiAlignmentService implements IService {
    protected static final String perlPath = SystemConfigurationProperties.getString("Perl.Path");
    protected static final String basePath = SystemConfigurationProperties.getString("Executables.ModuleBase") + SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");

    protected Task _task;
    protected String _databaseUser;
    protected String _databasePassword;
    protected String _targetDatabase;
    protected String _targetDirectory;
    protected Logger _logger;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            init(processData);
            String fullCmd = perlPath + " " + basePath + getCommandLine() + " &> "+getLogFilePath()+_task.getTaskName()+
                    "."+System.currentTimeMillis();
            fullCmd = "export PATH=$PATH:" + basePath + ";export PERL5LIB=$PERL5LIB:" + basePath + ";" + fullCmd;

            SystemCall call = new SystemCall(_logger);
            int success = call.emulateCommandLine(fullCmd, true, null, new File(_targetDirectory));
            if (success != 0) {
                throw new ServiceException("Execution of " + getServiceName() + " failed with exit value: " + success);
            }
        }
        catch (Throwable e) {
            try {
                EJBFactory.getLocalComputeBean().saveEvent(_task.getObjectId(), Event.ERROR_EVENT, e.getMessage(), new Date());
            }
            catch (Throwable e1) {
                e1.printStackTrace();
                _logger.error("Unable to save error event to task " + _task.getObjectId());
            }
            _logger.error("Error in " + getServiceName() + ". for task " + _task.getObjectId(), e);
            throw new ServiceException("Unable to process " + getServiceName() + ".", e);
        }
    }

    protected void init(IProcessData processData) throws MissingDataException, IOException, ServiceException {
        this._task = ProcessDataHelper.getTask(processData);
        if (null != _task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME) && !"".equals(_task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME))) {
            this._databaseUser = _task.getParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the database username was undefined.");
        }
        if (null != _task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD) && !"".equals(_task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD))) {
            this._databasePassword = _task.getParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the database password was undefined.");
        }
        if (null != _task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME) && !"".equals(_task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME))) {
            this._targetDatabase = _task.getParameter(ProkPipelineBaseTask.PARAM_DB_NAME);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the target database was undefined.");
        }
        if (null != _task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY) && !"".equals(_task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY))) {
            this._targetDirectory = _task.getParameter(ProkPipelineBaseTask.PARAM_DIRECTORY);
        }
        else {
            throw new ServiceException("Execution of " + getServiceName() + " failed because the target directory was undefined.");
        }
        this._logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
        FileUtil.ensureDirExists(getLogFilePath());
    }

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
        return _targetDirectory +File.separator+"logs"+File.separator;
    }

    public String getCommandLine() {
        return "btab_to_multi.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword +
                " -G " + getDefaultProjectCode() + " -I ";
    }

}
