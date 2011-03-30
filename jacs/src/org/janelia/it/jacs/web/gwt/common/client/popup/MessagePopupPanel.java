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

package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * Modal alert popup.
 */
public class MessagePopupPanel extends BasePopupPanel {
    private String _buttonText = "Ok";
    private String _message = "";

    public MessagePopupPanel(String title, String message) {
        super(title, /*realize now*/ false);
        _message = message;
    }

    public MessagePopupPanel(String title, String message, String buttonText) {
        super(title, /*realize now*/ false);
        _message = message;
        setButtonText(buttonText);
    }

    private void setButtonText(String buttonText) {
        _buttonText = buttonText;
        // Update button if already created
        if (getButtonSet() != null && getButtonSet().getButtons() != null && getButtonSet().getButtons().size() == 1)
            getButtonSet().getButtons().iterator().next().setText(_buttonText);
    }

    protected void populateContent() {
        add(HtmlUtils.getHtml(_message, "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

    protected ButtonSet createButtons() {
        return new ButtonSet(new RoundedButton[]{
                new RoundedButton(_buttonText, new ClickListener() {
                    public void onClick(Widget widget) {
                        hide();
                    }
                })
        });
    }
}