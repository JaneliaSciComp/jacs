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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class BackActionLink extends ActionLink {
    public BackActionLink(String linkText, ClickListener clickListener) {
        this(linkText, clickListener, null);
        init();
    }

    public BackActionLink(String linkText, ClickListener clickListener, String targetHistoryToken) {
        super(linkText, /*Image*/ null, clickListener);
        setTargetHistoryToken(targetHistoryToken);
        init();
    }

    private void init() {
        Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
        image.setStyleName("backActionLinkImage"); // need to push it down

        setImage(image);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            setLinkStyleName("actionLink");
            addLinkStyleName("actionLinkTextWrapper"); // space the text away from images

            Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
            image.setStyleName("backActionLinkImage");
            setImage(image);
        }
        else {
            setLinkStyleName("disabledActionLink");
            addLinkStyleName("actionLinkTextWrapper"); // space the text away from images

            Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftDisabledImage().createImage();
            image.setStyleName("backActionLinkImage");
            setImage(image);
        }
    }
}