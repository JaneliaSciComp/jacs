
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 16, 2009
 * Time: 4:36:06 PM
 */
public class FtpFileTask extends Task {
    transient public static final String TASK_NAME = "ftpFileTask";
    transient public static final String DISPLAY_NAME = "Ftp File Task";

    // Parameter Keys
    transient public static final String PARAM_targetDirectory = "targetDirectory";
    // The ftpSource dir must contain the location; ie ftp://anonymous:anonymous@ftp.ncbi.nih.gov/genomes/Bacteria/
    transient public static final String PARAM_ftpSourceDirectory = "ftpSourceDirectory";
    transient public static final String PARAM_targetExtensions = "targetExtensions";
    transient public static final String PARAM_ftpServer = "ftpServer";
    transient public static final String PARAM_ftpPort = "ftpPort";
    transient public static final String PARAM_ftpLogin = "ftpLogin";
    transient public static final String PARAM_ftpPassword = "ftpPassword";

    public FtpFileTask(Set<Node> inputNodes, String owner, List<Event> events, Set<TaskParameter> taskParameterSet) {
        super(inputNodes, owner, events, taskParameterSet);
        setDefaultValues();
    }

    private void setDefaultValues() {
        setParameter(PARAM_targetDirectory, "");
        setParameter(PARAM_ftpSourceDirectory, "");
        setParameter(PARAM_ftpLogin, "anonymous");
        setParameter(PARAM_ftpPassword, "anonymous");
        setParameter(PARAM_ftpServer, "");
        setParameter(PARAM_ftpPort, "21");
        this.taskName = TASK_NAME;
    }

    public FtpFileTask() {
        setDefaultValues();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        if (key.equals(PARAM_targetDirectory) || key.equals(PARAM_ftpSourceDirectory) || key.equals(PARAM_ftpLogin) ||
                key.equals(PARAM_ftpPassword) || key.equals(PARAM_ftpServer)) {
            return new TextParameterVO(value, 500);
        }
        // No match
        return null;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public boolean isParameterRequired(String parameterKeyName) {
        return super.isParameterRequired(parameterKeyName);
    }

}
