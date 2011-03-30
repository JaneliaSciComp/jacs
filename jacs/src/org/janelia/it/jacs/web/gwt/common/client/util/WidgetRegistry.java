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
