
package org.janelia.it.jacs.model.tasks.prokAnnotation;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

/**
 * This class is used by an MBean to populate the list of organism directories used for the Prok pipeline.
 * It describes dirs like /usr/local/annotation/NTPP13 to the system.
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class ProkAnnotationDirectoryUpdateTask extends Task {

    public static final String DISPLAY_NAME = "Prokaryotic Annotation Update Data";

    public ProkAnnotationDirectoryUpdateTask() {
        super();
        this.taskName = "Prokaryotic Annotation Update Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) {
            return null;
        }
        String value = getParameter(key);
        if (value == null) {
            return null;
        }
        else {
            return new TextParameterVO(value);
        }
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }
}