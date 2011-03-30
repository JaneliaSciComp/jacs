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
