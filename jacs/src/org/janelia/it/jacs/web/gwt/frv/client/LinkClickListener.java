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

package org.janelia.it.jacs.web.gwt.frv.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAtRelativePixelLauncher;
import org.janelia.it.jacs.web.gwt.detail.client.util.SampleAndSiteInfoPopup;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 29, 2007
 * Time: 4:30:42 PM
 */
public class LinkClickListener implements ClickListener {
    private String sampleId;
    private Widget parentWidget;

    public LinkClickListener(String sampleId, Widget parentWidget) {
        this.sampleId = sampleId;
        this.parentWidget = parentWidget;
    }

    public void onClick(Widget widget) {
        // Put the popup left-aligned with the legend panel and just above the parent FRV panel
        new PopupAtRelativePixelLauncher(new SampleAndSiteInfoPopup(sampleId), -38, -7).showPopup(parentWidget);
    }
}
