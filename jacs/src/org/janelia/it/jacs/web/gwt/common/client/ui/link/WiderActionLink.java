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

import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * Abstract ActionLink that toggles between "wider" (primary state) and "narrower" (secondary state). Subclasses
 * must override toggleTo[Primary,Secondary]State to implement behavior on user action to change width.
 */
abstract public class WiderActionLink extends ToggleActionLink {
    private String _wideWidth;
    private String _narrowWidth;

    /**
     * @param narrowWidth String representing the narrower width of the panel (suitable for Widget.setWidth())
     * @param wideWidth   String representing the wider width of the panel (suitable for Widget.setWidth())
     */
    public WiderActionLink(String narrowWidth, String wideWidth) {
        super("width", "width");
        _narrowWidth = narrowWidth;
        _wideWidth = wideWidth;
        init();
    }

    private void init() {
        Image leftArrow = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
        leftArrow.setStyleName("widerActionLinkLeftImage");

        Image rightArrow = ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().createImage();
        rightArrow.setStyleName("widerActionLinkRightImage");

        setPrimaryImage(leftArrow);
        setSecondaryImage(rightArrow, Side.RIGHT);
    }

    public String getNarrowWidth() {
        return _narrowWidth;
    }

    public String getWideWidth() {
        return _wideWidth;
    }
}
