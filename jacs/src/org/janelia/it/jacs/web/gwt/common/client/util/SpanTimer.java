
package org.janelia.it.jacs.web.gwt.common.client.util;

import java.util.Date;

/**
 * Mechanism to determine time between operations to ms resolution.  Use SpanTimerLogger for output.
 *
 * @author Michael Press
 */
public class SpanTimer {
    private Date _startTime;
    private Date _endTime;

    public SpanTimer() {
    }

    public Date start() {
        _startTime = new Date();
        return _startTime;
    }

    public Date end() {
        _endTime = new Date();
        return _endTime;
    }

    public long getSpan() {
        return _endTime.getTime() - _startTime.getTime();
    }

    public Date getEndTime() {
        return _endTime;
    }

    public Date getStartTime() {
        return _startTime;
    }
}
