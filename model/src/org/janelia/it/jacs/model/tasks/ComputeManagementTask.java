
package org.janelia.it.jacs.model.tasks;

import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

/**
 * Created by IntelliJ IDEA.
 * User: adrozdet
 * Date: Aug 20, 2008
 * Time: 2:47:10 PM
 */
public class ComputeManagementTask extends Task {

    transient public static final String DISPLAY_COMPUTE_MANAGE = "Compute Management";

    public ComputeManagementTask() {
        // Do construct
    }

    public String getDisplayName() {
        return DISPLAY_COMPUTE_MANAGE;
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        // Figure out what to put here
        return null;
    }

    public void cancelTask(long taskId) {
        // Do something
    }
}
