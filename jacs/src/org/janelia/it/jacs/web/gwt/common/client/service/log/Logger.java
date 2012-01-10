
package org.janelia.it.jacs.web.gwt.common.client.service.log;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * @author Michael Press
 */
public class Logger {
    private static Logger _instance = null;
    private Level _level = new Level(); // defaults to ERROR
    private String _className = null; // TODO: send to server for server-side class-level output granularity

    private static LoggingServiceAsync _loggingService = (LoggingServiceAsync) GWT.create(LoggingService.class);

    static {
        ((ServiceDefTarget) _loggingService).setServiceEntryPoint("log.oas");
    }

    /**
     * required by GWT
     */
    public Logger() {
    }

    //TODO: send classname to server
    //TODO: return a new logger for each caller (to locally store class name) but prime the log level with the root's?
    public static Logger getLogger(String className) {
        if (_instance == null) {
            _instance = new Logger();
            _instance.setLevel(Level.DEFAULT);
            _instance.setClass(className);
        }
        return _instance;
    }

    public void trace(String msg) {
        write(Level.TRACE, msg);
    }

    public void debug(String msg) {
        write(Level.DEBUG, msg);
    }

    public void info(String msg) {
        write(Level.INFO, msg);
    }

    public void warn(String msg) {
        write(Level.WARN, msg);
    }

    public void error(String msg) {
        write(Level.ERROR, msg);
    }

    public void fatal(String msg) {
        write(Level.FATAL, msg);
    }

    public void trace(Throwable throwable) {
        write(Level.TRACE, null, throwable);
    }

    public void debug(Throwable throwable) {
        write(Level.DEBUG, null, throwable);
    }

    public void info(Throwable throwable) {
        write(Level.INFO, null, throwable);
    }

    public void warn(Throwable throwable) {
        write(Level.WARN, null, throwable);
    }

    public void error(Throwable throwable) {
        write(Level.ERROR, null, throwable);
    }

    public void fatal(Throwable throwable) {
        write(Level.FATAL, null, throwable);
    }

    public void trace(String msg, Throwable throwable) {
        write(Level.TRACE, msg, throwable);
    }

    public void debug(String msg, Throwable throwable) {
        write(Level.DEBUG, msg, throwable);
    }

    public void info(String msg, Throwable throwable) {
        write(Level.INFO, msg, throwable);
    }

    public void warn(String msg, Throwable throwable) {
        write(Level.WARN, msg, throwable);
    }

    public void error(String msg, Throwable throwable) {
        write(Level.ERROR, msg, throwable);
    }

    public void fatal(String msg, Throwable throwable) {
        write(Level.FATAL, msg, throwable);
    }

    public Level getLevel() {
        return _level;
    }

    public void setLevel(Level level) {
        _level = level;
    }

    private void setClass(String className) {
        _className = className;
    }

    public boolean isTraceEnabled() {
        return (isLevelEnabled(Level.TRACE));
    }

    public boolean isDebugEnabled() {
        return (isLevelEnabled(Level.DEBUG));
    }

    public boolean isInfoEnabled() {
        return (isLevelEnabled(Level.INFO));
    }

    public boolean isWarnEnabled() {
        return (isLevelEnabled(Level.WARN));
    }

    public boolean isErrorEnabled() {
        return (isLevelEnabled(Level.ERROR));
    }

    public boolean isFatalEnabled() {
        return (isLevelEnabled(Level.FATAL));
    }

    /**
     * Determines if the specified level can be logged; that is, is the specified level >= the current log level.
     *
     * @param level to log
     * @return true if level's log is >= the currently set Level
     */
    private boolean isLevelEnabled(Level level) {
        return (level.isGreaterOrEqual(getLevel()));
    }

    private void write(Level level, String msg) {
        if (isLevelEnabled(level))
            _loggingService.log("(" + level.toString() + ") " + msg, new NoopCallback());
    }

    private void write(Level level, String msg, Throwable throwable) {
        //TODO: pass level to logging service and have it output on client side
        if (isLevelEnabled(level)) {
            write(level, msg);
            if (throwable != null)
                _loggingService.log(getStackTrace(throwable, level), new NoopCallback());
        }
    }

    private String getStackTrace(Throwable throwable, Level level) {
        StringBuffer stackTrace = new StringBuffer();
        if (throwable != null) {
            if (throwable.getCause() != null)
                stackTrace.append("(").append(level).append(")").append(throwable.getCause().getMessage()).append("\n");
            stackTrace.append("(").append(level).append(") ").append(throwable.getMessage()).append("\n");
            StackTraceElement[] elements = throwable.getStackTrace();
            for (int i = 0; elements != null && i < elements.length; i++)
                stackTrace.append("(").append(level).append(")    at ").append(elements[i].getClassName()).append(":").append(elements[i].getLineNumber()).append("\n");
        }
        return stackTrace.toString();
    }

    private class NoopCallback implements AsyncCallback {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(Object result) {
        }
    }

    /**
     * For debugging only
     */
    public void force(String msg) {
        _loggingService.log("(force) " + msg, null, new NoopCallback());
    }
}
