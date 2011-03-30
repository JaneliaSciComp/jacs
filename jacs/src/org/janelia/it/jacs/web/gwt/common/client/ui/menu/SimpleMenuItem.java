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

package org.janelia.it.jacs.web.gwt.common.client.ui.menu;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Widget;

/**
 * A SimpleMenuItem is an item in a menu.  It has a label and a Command that is executed when the item is clicked.
 *
 * @author Michael Press
 */
public class SimpleMenuItem extends HTML {
    public static final String STYLE_NAME = "HeaderMenuItem";
    public static final String HOVER_STYLE_NAME = "HeaderMenuItemHover";

    private Command _command;

    public SimpleMenuItem(String html, Command command) {
        super(html);
        _command = command;
        init();
    }

    private void init() {
        setStyleName(STYLE_NAME);
        addMouseListener(new MouseListenerAdapter() {
            public void onMouseLeave(Widget sender) {
                setStyleName(STYLE_NAME);
            }

            public void onMouseEnter(Widget sender) {
                setStyleName(HOVER_STYLE_NAME);
            }
        });
        addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _command.execute();
            }
        });
    }
}
