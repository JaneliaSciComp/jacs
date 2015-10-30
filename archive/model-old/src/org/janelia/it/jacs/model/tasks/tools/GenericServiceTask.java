
package org.janelia.it.jacs.model.tasks.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 20, 2010
 * Time: 2:15:30 PM
 */
public class GenericServiceTask extends Task {
    transient public static final String PARAM_service_name = "service name";
    transient public static final String PARAM_service_options = "service options";
    transient public static final String PARAM_grid_options = "grid_options";

    public GenericServiceTask() {
        super();
        setTaskName("Generic Service Task");
    }

    public GenericServiceTask(String serviceName) {
        super();
        setTaskName(serviceName.concat(" Task"));
        setParameter(PARAM_service_name, serviceName);
    }

    public GenericServiceTask(String serviceName, String serviceOptions) {
        super();
        setTaskName(serviceName.concat(" Task"));
        setParameter(PARAM_service_name, serviceName);
        setParameter(PARAM_service_options, serviceOptions);
    }

    public String getDisplayName() {
        String displayName = getParameter(PARAM_service_name);
        if (null == displayName) {
            return "GenericServiceTask";
        }
        else {
            return displayName.concat("Task");
        }
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

    public String generateCommandOptions(String outputDirectory) throws ParameterException {
        return getParameter(PARAM_service_options);
    }
}
