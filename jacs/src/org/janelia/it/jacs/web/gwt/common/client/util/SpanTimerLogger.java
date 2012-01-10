
package org.janelia.it.jacs.web.gwt.common.client.util;

import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.FormattedDateTimeMillisecs;

import java.util.Date;

/**
 * Decorates the SpanTimer with logging on start and end operations
 *
 * @author Michael Press
 */
public class SpanTimerLogger {
    private static Logger _logger = Logger.getLogger("");

    private SpanTimer _spanTimer;
    private String _msgBase;

    public SpanTimerLogger(String msgBase) {
        this(new SpanTimer(), msgBase);
    }

    public SpanTimerLogger(SpanTimer spanTimer, String msgBase) {
        _msgBase = msgBase;
        _spanTimer = spanTimer;
    }


    /**
     * Starts timer and outputs a message
     */
    public Date start() {
        return start(true);
    }

    public Date start(boolean output) {
        Date start = _spanTimer.start();
        if (output)
            outputStart();
        return start;
    }

    /**
     * Stops timer and outputs a message
     */
    public Date end() {
        return end(true, true);
    }

    public Date end(boolean outputEndMessage, boolean outputSpanMessage) {
        Date end = _spanTimer.end();
        if (outputEndMessage)
            outputEnd();
        if (outputSpanMessage)
            outputSpan();
        return end;
    }

    public void outputStart() {
        _logger.debug("^^^^^^^^ " + format(_spanTimer.getStartTime()) + " " + _msgBase);
    }

    public void outputEnd() {
        _logger.debug("-------- " + format(_spanTimer.getEndTime()) + " " + _msgBase);
    }

    public void outputSpan() {
        _logger.debug(">>>>>>>> " + _msgBase + " total time: " + ((float) _spanTimer.getSpan()) / 1000 + " s");
    }

    public String format(Date date) {
        return new FormattedDateTimeMillisecs(date.getTime()).toString();
    }
}