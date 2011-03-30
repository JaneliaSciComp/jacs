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

package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledBox but sets no background styles, so it inherits its parents styles.  Requires the specification
 * of a title label background color (using setActionLinkPanelStyleName(StyleName)) or else the title label is transparent and shows part of the main box line
 * underneath the text.  The background color of the style padded to setActionLinkPanelStyleName() should match the
 * background color of the panel to which the ClearTitledBox is added.
 *
 * @author Michael Press
 */
public class ClearTitledBox extends SecondaryTitledBox {
    /**
     * @param title
     * @param showActionLinks
     */
    public ClearTitledBox(String title, boolean showActionLinks) {
        super(title, showActionLinks);
    }

    protected void init() {
        super.init();

        setContentsPanelStyleName("clearTitledBoxContentsPanel");
        setCornerStyleName("clearTitledBoxRounding");
        setLabelStyleName("clearTitledBoxLabel");
        setLabelCornerStyleName("clearTitledBoxLabelRounding");
        setLabelPanelStyleName("clearTitledBoxTitleLabel");
        setActionLinkBackgroundStyleName("clearTitledBoxTitleLabel");
    }

    /**
     * Explicitly returns null to signal no border on title area
     */
    protected String getTitleBorderColor() {
        return null;
    }
}
