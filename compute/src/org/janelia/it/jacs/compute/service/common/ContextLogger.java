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
            logContext.append(", task ");
            logContext.append(task.getObjectId());
            final Long parentTaskId = task.getParentTaskId();
            if (parentTaskId != null) {
                logContext.append(" (child of task ");
                logContext.append(parentTaskId);
                logContext.append(")");
            }
        }
    }

    /**
     * Appends the specified message to the logging context.
     *
     * @param  additionalContext  context to append.
     */
    public void appendToLogContext(String additionalContext) {
        if (! StringUtils.isEmpty(additionalContext)) {
            logContext.append(", ");
            logContext.append(additionalContext);
        }
    }

    /**
     * @return true if info logging is enabled; otherwise false.
     */
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Logs the specified message with the current context appended.
     *
     * @param  msg  detail message to log.
     */
    public void info(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(msg + logContext);
        }
    }

}
