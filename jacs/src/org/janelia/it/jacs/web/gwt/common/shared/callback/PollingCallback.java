
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
