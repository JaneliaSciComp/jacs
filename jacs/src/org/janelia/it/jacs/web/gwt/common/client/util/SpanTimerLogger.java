/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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