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

package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Michael Press
 */
public class SearchIconMouseManager extends MouseListenerAdapter implements ClickListener {
    SearchIconPanel _iconPanel;

    public SearchIconMouseManager(SearchIconPanel iconPanel) {
        _iconPanel = iconPanel;
    }

    /**
     * add the hover style name on mouse enter (if not selected)
     */
    public void onMouseEnter(Widget widget) {
        if (!_iconPanel.isSelected()) {
            _iconPanel.removeStyleName(_iconPanel.getPanelUnselectedStyleName());
            _iconPanel.addStyleName(_iconPanel.getPanelHoverStyleName());
        }
    }

    /**
     * remove the hover style name on mouse enter (if not selected)
     */
    public void onMouseLeave(Widget widget) {
        if (!_iconPanel.isSelected()) {
            _iconPanel.removeStyleName(_iconPanel.getPanelHoverStyleName());
            _iconPanel.addStyleName(_iconPanel.getPanelUnselectedStyleName());
        }
    }

    public void onClick(Widget w) {
        // Toggle selection unless this is the last icon selected (can't unselect the last icon)
        if (!_iconPanel.isSelected() || _iconPanel.getSelectionCounter().getCount() > 1) {
            _iconPanel.toggleSelected();

            if (_iconPanel.isSelected()) { /* unselect */
                _iconPanel.removeStyleName(_iconPanel.getPanelUnselectedStyleName());
                _iconPanel.removeStyleName(_iconPanel.getPanelHoverStyleName());
                _iconPanel.addStyleName(_iconPanel.getPanelSelectedStyleName());
                _iconPanel.unselectOtherIcons();
                _iconPanel.getSelectionCounter().increment();
            }
            else { /* select */
                _iconPanel.removeStyleName(_iconPanel.getPanelSelectedStyleName());
                _iconPanel.addStyleName(_iconPanel.getPanelHoverStyleName());
                _iconPanel.getSelectionCounter().decrement();
            }
        }
    }
}

