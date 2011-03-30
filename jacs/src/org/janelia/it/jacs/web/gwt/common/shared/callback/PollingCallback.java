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

package org.janelia.it.jacs.web.gwt.common.shared.callback;

import com.google.gwt.user.client.Timer;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 18, 2007
 * Time: 3:28:32 PM
 */
public abstract class PollingCallback extends Timer {
    public static final int DEFAULT_INTERVAL = 1000; // 1.0 seconds
    private int _repeatInterval;
    private boolean _started = false;

    public PollingCallback() {
        this(DEFAULT_INTERVAL);
    }

    public PollingCallback(int repeatInterval) {
        _repeatInterval = repeatInterval;
        scheduleRepeating(_repeatInterval);
    }

    public void run() {
        if (!_started) {
            start();
            _started = true;
        }
        else {
            getData();
        }
    }

    /* Starts the polling process, typically by making a call to the server which starts
     * a database process in a spun-off thread. Should not block.
     */
    abstract protected void start();

    /* Checks to see if data is available. If it is available, then (1) cancel() the timer,
     * (2) branch to handle it.
     */
    abstract protected void getData();

    public void restart() {
        scheduleRepeating(_repeatInterval);
    }

}
