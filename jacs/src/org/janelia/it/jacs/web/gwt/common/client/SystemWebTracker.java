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

package org.janelia.it.jacs.web.gwt.common.client;

/**
 * @author Cristian Goina
 */
public class SystemWebTracker {

    public static void trackActivity(String activity, String[] activityData) {
        if (false) // Did not want to comment this whole piece of code - so came up with this idiocy :) (LK)
        {
            StringBuffer activityBuffer = new StringBuffer();
            activityBuffer.append(activity);
            if (activityData != null) {
                for (String anActivityData : activityData) {
                    if (anActivityData != null && anActivityData.length() > 0) {
                        activityBuffer.append('/');
                        activityBuffer.append(anActivityData);
                    }
                }
            }
            trackActivity(activityBuffer.toString());
        }
    }

    //    $wnd.urchinTracker(activity);

    public native static void trackActivity(String activity) /*-{
//        $wnd.pageTracker._trackPageview(activity);
    }-*/;

}
