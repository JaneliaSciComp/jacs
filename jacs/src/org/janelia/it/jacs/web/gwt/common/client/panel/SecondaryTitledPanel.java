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
 * Same as a TitledPanel but sets styles to use "scondary" colors, such that a SecondaryTitledPanel can be placed
 * inside a TitledPanel and it will visually differentiate itself.
 *
 * @author Michael Press
 */
public class SecondaryTitledPanel extends TitledPanel {
    private static final String SECONDARY_PORTLET_TITLE_STYLE = "secondaryPortletTitle";
    private static final String SECONDARY_PORTLET_STYLE = "secondaryPortlet";
    private static final String SECONDARY_TOP_ROUND_STYLE_NAME = "secondaryPortletTopRounding";
    private static final String SECONDARY_PORTLET_BOTTOM_ROUNDING_STYLE = "secondaryPortletBottomRounding";

    public SecondaryTitledPanel() {
        init();
    }

    public SecondaryTitledPanel(String title) {
        super(title);
        init();
    }

    public SecondaryTitledPanel(String title, boolean showActionLinks) {
        super(title, showActionLinks);
        init();
    }

    public SecondaryTitledPanel(String title, boolean showActionLinks, boolean realizeNow) {
        super(title, showActionLinks, realizeNow);
        init();
    }

    //TODO: refactor TitledPanel to get styles via methods, then override those methods here
    private void init() {
        setContentStyleName(SECONDARY_PORTLET_STYLE);
        setBottomRoundCornerStyleName(SECONDARY_PORTLET_BOTTOM_ROUNDING_STYLE);
        setWidth("auto");
    }

    protected String getRoundingBorder() {
        return "#CACACA";
    }

    protected String getTitleStyleName() {
        return SECONDARY_PORTLET_TITLE_STYLE;
    }

    protected String getTopRoundingBorderStyleName() {
        return SECONDARY_TOP_ROUND_STYLE_NAME;
    }
}
