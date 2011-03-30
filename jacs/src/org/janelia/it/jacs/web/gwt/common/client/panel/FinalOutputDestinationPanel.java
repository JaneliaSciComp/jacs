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

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Sep 18, 2009
 * Time: 3:58:03 PM
 */
public class FinalOutputDestinationPanel extends VerticalPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.common.client.panel.ProjectCodePanel");
    public static final String PATH_DEFAULT = "System Default Directory";
    public static final String PATH_OVERRIDE = "User-Specified Directory";

    private ListBox _finalOutputListBox = new ListBox();
    private VerticalPanel _finalOutputPanel = new VerticalPanel();
    private TextBox _finalOutputDestinationTextBox;

    public FinalOutputDestinationPanel() {
        super();
        init();
    }

    private void init() {
        _finalOutputListBox.addItem(PATH_DEFAULT);
        _finalOutputListBox.addItem(PATH_OVERRIDE);
        _finalOutputPanel.setVisible(false);
        _finalOutputListBox.addChangeListener(new ChangeListener() {
            public void onChange(Widget widget) {
                _finalOutputPanel.setVisible(PATH_OVERRIDE.equals(_finalOutputListBox.getValue(_finalOutputListBox.getSelectedIndex())));
            }
        });
        _finalOutputListBox.setSelectedIndex(0);
        _finalOutputDestinationTextBox = new TextBox();
        _finalOutputDestinationTextBox.setVisibleLength(60);
        _finalOutputPanel.add(_finalOutputDestinationTextBox);
        add(_finalOutputListBox);
        add(_finalOutputPanel);
    }

    public boolean overrideFinalOutputPath() {
        return PATH_OVERRIDE.equals(_finalOutputListBox.getValue(_finalOutputListBox.getSelectedIndex()));
    }

    public String getFinalOutputDestination() {
        return _finalOutputDestinationTextBox.getText();
    }


}
