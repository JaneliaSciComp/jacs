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

package org.janelia.it.jacs.web.gwt.common.client.popup.launcher;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * Launches a popup above the clicked/hovered widget, unlike its parent which launches the popup below.
 *
 * @author Michael Press
 */
public class PopupAboveLauncher extends PopupLauncher {
    /**
     * For deferred assignment of popup
     */
    public PopupAboveLauncher() {
        super();
    }

    public PopupAboveLauncher(PopupPanel popup) {
        super(popup);
    }

    public PopupAboveLauncher(PopupPanel popup, int msDelay) {
        super(popup, msDelay);
    }

    protected int getPopupTopPosition(UIObject sender) {
        return sender.getAbsoluteTop() - getPopup().getOffsetHeight();
    }
}
