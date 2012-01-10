
package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;

/**
 * Storage for widgets.  Allows different areas of the app to get a handle on widgets.  The internal HashMap does
 * not need to be synchronized since this class is compiled into JavaScript;  the GWT compiler ignores synchronization
 * since there's no such construct in JavaScript.
 *
 * @author mpress
 */
public class WidgetRegistry {
    private static HashMap<String, Widget> hash = new HashMap<String, Widget>();

    public static void putWidget(Widget w, String key) {
        hash.put(key, w);
    }

    public static Widget getWidget(String key) {
        return hash.get(key);
    }

    public static void removeWidget(String key) {
        hash.remove(key);
    }
}
