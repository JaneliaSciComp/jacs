package org.janelia.it.jacs.compute.service.common;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * Utility to store contextual logging information for reuse
 * in multiple logging calls.
 *
 * @author Eric Trautman
 */
public class ContextLogger {

    private Logger logger;
    private StringBuilder logContext;

    /**
     * @param  logger  the logger instance to wrap.
     */
    public ContextLogger(Logger logger) {
        this.logger = logger;
        this.logContext = new StringBuilder(256);
    }

    /**
     * Appends task id information to the logging context.
     *
     * @param  task  the current task.
     */
    public void appendToLogContext(Task task) {
        if (task != null) {
            StringBuilder taskContext = new StringBuilder(128);
            taskContext.append("task ");
            taskContext.append(task.getObjectId());
            final Long parentTaskId = task.getParentTaskId();
            if (parentTaskId != null) {
                taskContext.append(" (child of task ");
                taskContext.append(parentTaskId);
                taskContext.append(")");
            }
            appendToLogContext(taskContext.toString());
        }
    }

    /**
     * Appends the specified message to the logging context.
     *
     * @param  additionalContext  context to append.
     */
    public void appendToLogContext(String additionalContext) {
        if (! StringUtils.isEmpty(additionalContext)) {
            final int len = logContext.length();
            if (len == 0) {
                logContext.append("\t [CONTEXT: ");
            } else {
                logContext.setLength(len - 1); // remove trailing ']'
                logContext.append(", ");
            }

            logContext.append(additionalContext);
            logContext.append(']');
        }
    }

    /**
     * @return true if info logging is enabled; otherwise false.
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Logs the specified error message with the current context appended.
     *
     * @param  msg  detail message to log.
     */
    public void error(String msg) {
        logger.error(msg + logContext);
    }

    /**
     * Logs the specified error message with the current context appended.
     *
     * @param  msg   detail message to log.
     * @param  cause the cause of this error.
     */
    public void error(String msg,
                      Throwable cause) {
        logger.error(msg + logContext, cause);
    }

    /**
     * Logs the specified warn message with the current context appended.
     *
     * @param  msg  detail message to log.
     */
    public void warn(String msg) {
        logger.warn(msg + logContext);
    }

    /**
     * Logs the specified info message with the current context appended.
     *
     * @param  msg  detail message to log.
     */
    public void info(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(msg + logContext);
        }
    }

    /**
     * Logs the specified debug message with the current context appended.
     *
     * @param  msg  detail message to log.
     */
    public void debug(String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg + logContext);
        }
    }

}
