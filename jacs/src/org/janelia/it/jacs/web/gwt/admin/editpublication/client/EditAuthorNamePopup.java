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

package org.janelia.it.jacs.web.gwt.admin.editpublication.client;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;


public class EditAuthorNamePopup extends ModalPopupPanel { /*copy cat of EditUserNodeNamePopup*/


    /*
    private static Logger _logger =Logger.getLogger("org.janelia.it.jacs.web.gwt.blast.client.popup.EditUserNodeNamePopup");
    */

    private EditAuthorNameListener nameReplacer;
    private TextBox nodeNameText;
    private String authorName;


    public EditAuthorNamePopup(EditAuthorNameListener nameReplacer, boolean realizeNow, String authorName) {

        super("Edit User Node Name", realizeNow);

        this.authorName = authorName;

        this.nameReplacer = nameReplacer;

    }


    protected ButtonSet createButtons() {


        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Save", new ClickListener() {
            public void onClick(Widget widget) {
                replace();
            }
        });
        tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {

        HorizontalPanel editPanel = new HorizontalPanel();

        Label nameLabel = new Label("Name:   ");
        nameLabel.setStyleName("prompt");
        editPanel.add(nameLabel);

        nodeNameText = new TextBox();
        nodeNameText.setVisibleLength(20);
        nodeNameText.setMaxLength(40);

        nodeNameText.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress(Widget sender, char keyCode, int modifiers) {
                // Check for enter
                if ((keyCode == 13) && (modifiers == 0)) {
                    replace();
                }
            }
        });

        if (authorName != null) {
            nodeNameText.setText(authorName);
        }

        editPanel.add(nodeNameText);
        this.add(editPanel);
        add(HtmlUtils.getHtml("&nbsp;", "text"));

    }


    private void replace() {

        /*
        if (nodeNameText==null)
            _logger.debug("EditUserNodeNamePopup: submit() - nodeNameText is null");
        */

        if (!nodeNameText.getText().equals(authorName)) {
            nameReplacer.replaceUserNodeName(nodeNameText.getText());
        }

        hide();

    }

}
