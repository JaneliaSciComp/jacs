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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A HorizontalPanel that automatically centers all added Widgets.  Defaults to 100% wide .
 *
 * @author Michael Press
 */
public class CenteredWidgetHorizontalPanel extends HorizontalPanel {
    private HorizontalPanel _centeredPanel;

    public CenteredWidgetHorizontalPanel() {
        init();
    }

    public CenteredWidgetHorizontalPanel(Widget centeredWidget) {
        init();
        add(centeredWidget);
    }

    private void init() {
        setWidth("100%");

        HTML leftSpacer = new HTML("&nbsp;");
        super.add(leftSpacer);
        setCellWidth(leftSpacer, "50%");

        _centeredPanel = new HorizontalPanel();
        super.add(_centeredPanel);

        HTML rightSpacer = new HTML("&nbsp;");
        super.add(rightSpacer);
        setCellWidth(rightSpacer, "50%");
    }

    public void add(Widget widget) {
        _centeredPanel.add(widget);
    }

    public boolean remove(Widget widget) {
        return _centeredPanel.remove(widget);
    }
}
