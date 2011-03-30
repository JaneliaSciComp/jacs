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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Dec 1, 2006
 * Time: 6:14:36 PM
 */
public class LinkGroupSelectionModel {
    private String _selectedValue;
    private List<SelectionListener> _listeners = new ArrayList<SelectionListener>();

    public void setSelectedValue(String selectedValue) {
        _selectedValue = selectedValue;
        notify(true, selectedValue);
    }

    // NOTE: synchronized is used here as an advisory that there could be conflict
    // if this class were used outside of GWT, rather than for any functional reason.
    // GWT does not have multithreading, and does not support real synchronization.
    public void addSelectionListener(SelectionListener listener) {
        _listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        if (_listeners.contains(listener))
            _listeners.remove(listener);
    }

    private void notify(boolean onSelect, String value) {
        // Notify listeners
        for (SelectionListener _listener : _listeners) {
            if (onSelect)
                _listener.onSelect(value);
            else
                _listener.onUnSelect(value);

        }
    }

    public String getSelectedValue() {
        return _selectedValue;
    }
}
