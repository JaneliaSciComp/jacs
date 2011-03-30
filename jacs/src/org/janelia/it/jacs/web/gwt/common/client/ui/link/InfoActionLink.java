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

package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import org.janelia.it.jacs.web.gwt.common.client.popup.BasePopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.WidgetBasePopupPanelController;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupAboveLauncher;
import org.janelia.it.jacs.web.gwt.common.client.popup.launcher.PopupLauncher;
import org.janelia.it.jacs.web.gwt.common.client.ui.HoverImageSetter;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class InfoActionLink extends ActionLink {
    private static final int POPUP_DELAY = 250;
    private PopupLauncher _launcher = new PopupAboveLauncher(); // default
    private WidgetBasePopupPanelController _popupController;

    public InfoActionLink(String linkText) {
        this(linkText, null);
    }

    public InfoActionLink(BasePopupPanel popup) {
        super("");
        init(popup);
    }

    public InfoActionLink(String linkText, BasePopupPanel popup) {
        super(linkText);
        init(popup);
    }

    private void init(BasePopupPanel popup) {
        setShowBrackets(false);
        setImage(ImageBundleFactory.getControlImageBundle().getInfoImage().createImage());
        setPopup(popup); // has to be after setImage();
    }

    public void setLauncher(PopupLauncher launcher) {
        _launcher = launcher;
        if (_popupController != null)
            _popupController.setLauncher(_launcher);
    }

    public void setPopup(BasePopupPanel popup) {
        if (popup != null) {
            // Change the image on hover
            getImage().addMouseListener(new HoverImageSetter(getImage(),
                    ImageBundleFactory.getControlImageBundle().getInfoImage(),
                    ImageBundleFactory.getControlImageBundle().getInfoHoverImage()));

            // Use a controller to keep the popup visible while the mouse is in it
            _popupController = new WidgetBasePopupPanelController(getImage(), popup, POPUP_DELAY, _launcher);
        }
    }
}