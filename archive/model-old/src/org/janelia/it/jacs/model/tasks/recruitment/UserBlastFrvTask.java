
package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class UserBlastFrvTask extends Task {

    public static final String DISPLAY_NAME = "User Blast-to-Frv Task";

    public UserBlastFrvTask() {
        super();
    }

    public UserBlastFrvTask(Set inputNodes, String owner, List events, Set parameters) {
        super(inputNodes, owner, events, parameters);
        this.taskName = "User-initiated Recruitment Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) {
            return null;
        }
        String value = getParameter(key);
        if (value == null) {
            return null;
        }
        // Default to something.  Not expecting any parameters for this task anyway.
        else {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}