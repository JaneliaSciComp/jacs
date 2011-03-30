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

import java.util.HashMap;
import java.util.Map;

/**
 * This class can be used on to track amount of time that code sections take to complete
 *
 * @author Tareq Nabeel
 */
public class PerfStats {
    //private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.util.PerfStats");

    private static boolean perfOn = false;

    // TreeMap cannot be used with GWT
    private static Map<String, PerfItem> perfItems = new HashMap<String, PerfItem>();

    /**
     * Method call to start the timer for a wrapped piece of code
     *
     * @param perfItemName performance item name
     */
    public synchronized static void start(String perfItemName) {
        if (!perfOn) {
            return;
        }
        PerfItem perfItem = perfItems.get(perfItemName);
        if (perfItem == null) {
            perfItem = new PerfItem(perfItemName);
        }
        perfItem.startTime = System.currentTimeMillis();
        perfItems.put(perfItemName, perfItem);
    }

    /**
     * Method call to end the timer for a wrapped piece of code.  This must match
     * parameter in start call
     *
     * @param perfItem perf item name
     */
    public synchronized static void end(String perfItem) {
        if (!perfOn) {
            return;
        }
        PerfItem item = perfItems.get(perfItem);
        item.total = item.total + (System.currentTimeMillis() - item.startTime);
    }

    /**
     * Prints all the numbers.  Should be called once when after all start/end calls.
     */
    public synchronized static void printNumbers() {
        if (!perfOn) {
            return;
        }
        StringBuffer fullMsg = new StringBuffer("\n-------- Performance -------\n");
        for (PerfItem perfItem : perfItems.values()) {
            fullMsg.append(perfItem.name);
            fullMsg.append(getElapsedTime(" took ", perfItem.total));
            fullMsg.append("\n");
        }

        perfItems.clear();

        //_logger.info(fullMsg.toString());
    }

    /**
     * Used to pretty up the elapse time
     *
     * @param msg     message
     * @param elapsed elapsed time in millis
     * @return String - the elapsed time
     */
    private static String getElapsedTime(String msg, long elapsed) {
        StringBuffer fullMsgBuff = new StringBuffer();
        if (msg != null) {
            fullMsgBuff.append(msg);
        }
        long mins = (elapsed / 60000);
        long secs = ((elapsed / 1000) - ((elapsed / 60000) * 60));
        long millis = (elapsed - ((elapsed / 60000) * 60000) - (((elapsed / 1000) - ((elapsed / 60000) * 60)) * 1000));

        if (mins > 0) {
            fullMsgBuff.append(mins);
            fullMsgBuff.append(" min(s) ");
        }
        if (secs > 0) {
            fullMsgBuff.append(secs);
            fullMsgBuff.append(" sec(s) ");
        }
        fullMsgBuff.append(millis);
        fullMsgBuff.append(" ms(s)");

        return fullMsgBuff.toString();

    }
}
