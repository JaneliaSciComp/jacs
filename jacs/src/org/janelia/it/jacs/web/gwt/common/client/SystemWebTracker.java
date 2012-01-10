
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
