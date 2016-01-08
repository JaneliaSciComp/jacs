
package org.janelia.it.jacs.model.tasks.utility;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;

import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Task for launching informatics sageLoader.pl script.
 *
 * @author Todd Safford
 */
public class SageLoaderTask extends Task {

    private static final String DISPLAY_NAME = "Sage Loader Task";
    private static final String PARAM_ITEMLIST = "item list";
    private static final String PARAM_LINE = "line";
    private static final String PARAM_CONFIG = "config";
    private static final String PARAM_GRAMMAR = "grammar";
    private static final String PARAM_LAB = "lab";
    private static final String PARAM_DEBUG = "debug";
    private static final String PARAM_LOCK = "lock";

    private static final String[] SCRIPT_ARGUMENT_NAMES = {
            PARAM_LINE, PARAM_CONFIG, PARAM_GRAMMAR, PARAM_LAB, PARAM_LOCK
    };
    private static final String[] SCRIPT_FLAG_NAMES = { PARAM_DEBUG };

    @SuppressWarnings("UnusedDeclaration")
    public SageLoaderTask() {
        super();
    }

    public SageLoaderTask(String owner,
                          List<Event> events,
                          String item,
                          String line,
                          String configPath,
                          String grammarPath,
                          String lab,
                          String debug,
                          String lockPath) throws IllegalArgumentException {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setItemList(ImmutableList.of(item));
        setOptionalParameter(PARAM_LINE, line);
        setPathParameter(PARAM_CONFIG, configPath, true);
        setPathParameter(PARAM_GRAMMAR, grammarPath, true);
        setRequiredParameter(PARAM_LAB, lab);
        setOptionalParameter(PARAM_DEBUG, debug);
        setPathParameter(PARAM_LOCK, lockPath, false);
    }

    public SageLoaderTask(String owner,
                          List<Event> events,
                          List<String> items,
                          String line,
                          String configPath,
                          String grammarPath,
                          String lab,
                          String debug,
                          String lockPath) throws IllegalArgumentException {
        super(new HashSet<Node>(), owner, events, new HashSet<TaskParameter>());
        this.taskName = DISPLAY_NAME;
        setItemList(items);
        setOptionalParameter(PARAM_LINE, line);
        setPathParameter(PARAM_CONFIG, configPath, true);
        setPathParameter(PARAM_GRAMMAR, grammarPath, true);
        setRequiredParameter(PARAM_LAB, lab);
        setOptionalParameter(PARAM_DEBUG, debug);
        setPathParameter(PARAM_LOCK, lockPath, false);
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

    public String[] getScriptArgumentNames() {
        return SCRIPT_ARGUMENT_NAMES;
    }

    public String[] getScriptFlagNames() {
        return SCRIPT_FLAG_NAMES;
    }

    public boolean hasParameter(String key) {
        final String value = getParameter(key);
        return ((value != null) && (value.trim().length() > 0));
    }

    private String getTrimmedValue(String name,
                                   String value,
                                   boolean isRequired) throws IllegalArgumentException {
        String trimmedValue = null;
        if (value != null) {
            trimmedValue = value.trim();
        }
        if (isRequired && (! isDefined(trimmedValue))) {
            throw new IllegalArgumentException(name + " parameter is not defined");
        }
        return trimmedValue;
    }

    private void setOptionalParameter(String name,
                                      String value) {
        final String trimmedValue = getTrimmedValue(name, value, false);
        if (isDefined(trimmedValue)) {
            setParameter(name, trimmedValue);
        }
    }

    private void setRequiredParameter(String name,
                                      String value) throws IllegalArgumentException {
        setParameter(name, getTrimmedValue(name, value, true));
    }

    private void setPathParameter(String name,
                                  String value,
                                  boolean isRequired) throws IllegalArgumentException {

        final String pathValue = getTrimmedValue(name, value, isRequired);
        if (isDefined(pathValue)) {
            final File file = new File(pathValue);
            final String absolutePath = file.getAbsolutePath();
            if (file.canRead()) {
                setParameter(name, absolutePath);
            } else {
                String error;
                if (file.exists()) {
                    error = "is not readable";
                } else {
                    error = "does not exist";
                }
                throw new IllegalArgumentException(name + " path parameter '" + absolutePath + "' " + error);
            }
        }
    }

    private void setItemList(List<String> values) throws IllegalArgumentException {
        final String name = PARAM_ITEMLIST;
        setParameter(PARAM_ITEMLIST, Joiner.on(',').skipNulls().join(values));
    }

    public List<String> getItemList() {
        return listOfStringsFromCsvString(getParameter(PARAM_ITEMLIST));
    }

    private boolean isDefined(String value) {
        return ((value != null) && (value.length() > 0));
    }

}