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

package org.janelia.it.jacs.shared.utils;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 30, 2007
 * Time: 11:08:13 AM
 */
public class DateUtil {

    public static String getElapsedTime(String msg, long timeStarted, long timeEnded) {
        return getElapsedTime(msg, timeEnded - timeStarted);
    }

    public static String getElapsedTime(long timeStarted, long timeEnded) {
        return getElapsedTime(null, timeStarted - timeEnded);
    }

    public static String getElapsedTime(long elapsed, boolean includeMillis) {
        return getElapsedTime(null, elapsed, includeMillis);
    }

    public static String getElapsedTime(long elapsed) {
        return getElapsedTime(null, elapsed);
    }

    public static String getElapsedTime(String msg, long elapsed) {
        return getElapsedTime(msg, elapsed, true);
    }

    public static String getElapsedTime(String msg, long elapsed, boolean includeMillis) {
        StringBuffer fullMsgBuff = new StringBuffer();
        if (msg != null) {
            fullMsgBuff.append(msg);
        }
        long hrs = (elapsed / 3600000);
        long mins = (elapsed / 60000);
        long secs = ((elapsed / 1000) - (elapsed / 60000) * 60);
        if (hrs > 0) {
            mins = mins - (hrs * 60);
            secs = secs - (mins * 60);
            fullMsgBuff.append(hrs);
            fullMsgBuff.append(" hr(s) ");
        }
        if (mins > 0) {
            fullMsgBuff.append(mins);
            fullMsgBuff.append(" min(s) ");
        }
        if (secs > 0) {
            fullMsgBuff.append(secs);
            fullMsgBuff.append(" sec(s) ");
        }
        if (includeMillis) {
            long millis = (long) (elapsed - (elapsed / 60000.00 * 60000) - (((elapsed / 1000.00) - (elapsed / 60000.00 * 60)) * 1000.00));
            fullMsgBuff.append(millis);
            fullMsgBuff.append(" ms(s)");
        }

        return fullMsgBuff.toString();
    }
}
