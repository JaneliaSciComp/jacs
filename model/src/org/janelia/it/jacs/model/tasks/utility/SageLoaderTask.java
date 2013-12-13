
package org.janelia.it.jacs.model.tasks.utility;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.util.HashSet;
import java.util.List;

/**
 * This action can work against directories or single files passed as source
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 3, 2008
 * Time: 10:08:35 AM
 */
public class SageLoaderTask extends Task {

    public static final String DISPLAY_NAME = "Sage Loader Task";
    public static final String PARAM_ITEM = "item";
    public static final String PARAM_CONFIG = "config";
    public static final String PARAM_GRAMMAR = "grammar";
    public static final String PARAM_LAB = "lab";
    public static final String PARAM_DEBUG = "debug";
    public static final String PARAM_LOCK = "lock";

    public SageLoaderTask() {
        super();
    }

    public SageLoaderTask(String owner,
                          List<Event> events,
                          String item,
                          String configPath,
                          String grammarPath,
                          String lab,
                          String debug,
                          String lock) {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setParameter(PARAM_ITEM, item);
        setParameter(PARAM_CONFIG, configPath);
        setParameter(PARAM_GRAMMAR, grammarPath);
        setParameter(PARAM_LAB, lab);
        setParameter(PARAM_DEBUG, debug);
        setParameter(PARAM_LOCK, lock);
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

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public static String getParamConfig() {
        return PARAM_CONFIG;
    }

    public static String getParamDebug() {
        return PARAM_DEBUG;
    }

    public static String getParamGrammar() {
        return PARAM_GRAMMAR;
    }

    public static String getParamItem() {
        return PARAM_ITEM;
    }

    public static String getParamLab() {
        return PARAM_LAB;
    }

    public static String getParamLock() {
        return PARAM_LOCK;
    }
}