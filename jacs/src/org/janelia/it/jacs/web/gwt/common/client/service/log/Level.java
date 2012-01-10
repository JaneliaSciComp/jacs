
package org.janelia.it.jacs.web.gwt.common.client.service.log;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Defines the set of logging levels, in order:<ol>
 * <li><code>OFF</code> turns off all logging.
 * <li><code>TRACE</code> designates finer-grained informational events than the <code>DEBUG</code level. </li>
 * <li><code>DEBUG</code> designates fine-grained informational events that are most useful to debug an application. </li>
 * <li><code>INFO</code> designates informational messages that highlight the progress of the application at coarse-grained level. </li>
 * <li><code>WARN</code> designates potentially harmful situations. </li>
 * <li><code>ERROR</code> designates error events that might still allow the application to continue running. </li>
 * <li><code>FATAL</code> designates very severe error events that will presumably lead the application to abort. </li>
 * <li><code>ALL</code> turns on all logging. </li>
 * </ol>
 * <p/>
 * Based on org.apache.log4j.Level;
 *
 * @author Michael Press
 */
public class Level implements IsSerializable, Serializable {
    public static final String OFF_STR = "OFF";
    public static final String TRACE_STR = "TRACE";
    public static final String DEBUG_STR = "DEBUG";
    public static final String INFO_STR = "INFO";
    public static final String WARN_STR = "WARN";
    public static final String ERROR_STR = "ERROR";
    public static final String FATAL_STR = "FATAL";
    public static final String ALL_STR = "ALL";

    public static final int ALL_INT = 0;
    public static final int TRACE_INT = 1;
    public static final int DEBUG_INT = 2;
    public static final int INFO_INT = 3;
    public static final int WARN_INT = 4;
    public static final int ERROR_INT = 5;
    public static final int FATAL_INT = 6;
    public static final int OFF_INT = 99;

    final public static Level OFF = new Level(OFF_INT, OFF_STR);
    final public static Level TRACE = new Level(TRACE_INT, TRACE_STR);
    final public static Level DEBUG = new Level(DEBUG_INT, DEBUG_STR);
    final public static Level INFO = new Level(INFO_INT, INFO_STR);
    final public static Level WARN = new Level(WARN_INT, WARN_STR);
    final public static Level ERROR = new Level(ERROR_INT, ERROR_STR);
    final public static Level FATAL = new Level(FATAL_INT, FATAL_STR);
    final public static Level ALL = new Level(ALL_INT, ALL_STR);
    final public static Level DEFAULT = ERROR;

    private static final HashMap<String, Level> _levels = new HashMap<String, Level>();

    static {
        _levels.put(ALL_STR, ALL);
        _levels.put(TRACE_STR, TRACE);
        _levels.put(DEBUG_STR, DEBUG);
        _levels.put(INFO_STR, INFO);
        _levels.put(WARN_STR, WARN);
        _levels.put(ERROR_STR, ERROR);
        _levels.put(FATAL_STR, FATAL);
        _levels.put(OFF_STR, OFF);
    }

    private int _level;
    private String _levelName;


    /**
     * For GWT deserialization support only;  sets level to ERROR. Use constants instead.
     */
    public Level() {
        this(DEFAULT.getLevel(), DEFAULT.toString());
    }

    /**
     * Creates a new Level corresponding to the provided level name.
     *
     * @param levelName the name of the level to create, from the Level "_STR" constants
     * @return a Level object corresponding to levelName, or ERROR if invalid levelName
     */
    public static Level create(String levelName) {
        Level level = _levels.get(levelName);
        if (level == null)
            level = ERROR;
        return level;
    }

    /**
     * internal instantiation of a Level object.
     */
    private Level(int level, String levelName) {
        _level = level;
        _levelName = levelName;
    }

    public boolean isGreaterOrEqual(Level level) {
        return level == null || (getLevel() >= level.getLevel());
    }

    private int getLevel() {
        return _level;
    }

    public String toString() {
        return _levelName;
    }
}
