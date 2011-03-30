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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.panel.CenteredWidgetHorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author Michael Press
 */
public class ButtonSet extends HorizontalPanel {
    private CenteredWidgetHorizontalPanel _panel;
    private HashMap<String, RoundedButton> _buttons = new HashMap<String, RoundedButton>();

    private static final String DEFAULT_STYLE_NAME = "buttonSet";

    public ButtonSet(RoundedButton[] buttons) {
        super();
        init(buttons);
    }

    private void init(RoundedButton[] buttons) {
        HorizontalPanel innerPanel = new HorizontalPanel();
        _panel = new CenteredWidgetHorizontalPanel();
        _panel.add(innerPanel);
        add(_panel);

        setStyleName(DEFAULT_STYLE_NAME);
        setWidth("100%");

        for (RoundedButton button : buttons) {
            innerPanel.add(button);
            innerPanel.add(HtmlUtils.getHtml("&nbsp;", "spacer"));
            _buttons.put(button.getText(), button);
        }
    }

    public void setStyleName(String style) {
        if (style != null)
            _panel.setStyleName(style);
    }

    public RoundedButton getButton(String name) {
        return _buttons.get(name);
    }

    public Collection<RoundedButton> getButtons() {
        return _buttons.values();
    }
}
