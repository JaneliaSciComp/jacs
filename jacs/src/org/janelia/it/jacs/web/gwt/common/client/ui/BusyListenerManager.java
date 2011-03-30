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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * @author Michael Press
 */
public class BusyListenerManager {
    private ArrayList<BusyListener> _listeners = new ArrayList<BusyListener>();
    private Widget _caller;

    public BusyListenerManager(Widget busyCaller) {
        _caller = busyCaller;
    }

    public void addBusyListener(BusyListener listener) {
        _listeners.add(listener);
    }

    public void removeBusyListener(BusyListener listener) {
        _listeners.remove(listener);
    }

    public void notifyOnBusy() {
        for (BusyListener _listener : _listeners) {
            _listener.onBusy(_caller);
        }
    }

    public void notifyOnBusyComplete() {
        for (BusyListener _listener : _listeners) {
            _listener.onBusyDone(_caller);
        }
    }
}
