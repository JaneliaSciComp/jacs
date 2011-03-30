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

package org.janelia.it.jacs.web.gwt.common.client.popup;

import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.PopperUpperHTML;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: May 23, 2007
 * Time: 4:33:04 PM
 */
public class PopperUpperPopup extends PopperUpperHTML {

    private BasePopupPanel popupPanel;

    public PopperUpperPopup(String text, String textStyleName, String hostTextStyleName, String hostHoverName, String hostClickName, boolean needHoverPopup, boolean needClickPopup, BasePopupPanel popupPanel) {
        super(text, textStyleName, DEFAULT_HOVER_STYLE, hostTextStyleName, hostHoverName, hostClickName, null, true, needHoverPopup, needClickPopup);
        this.popupPanel = popupPanel;
    }

    protected void configLauncher() {
        getLauncher().setPopup(popupPanel);
        getLauncher().setDelay(300);
    }

}
