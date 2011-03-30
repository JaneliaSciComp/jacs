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

package org.janelia.it.jacs.web.gwt.common.client.popup.download;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.popup.PopupCloser;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
abstract public class DownloadPopup extends ModalPopupPanel {
    //TODO: move download model to common so DownloadFilePopup can move to common
    public DownloadPopup(String title) {
        this(title, false);
    }

    public DownloadPopup(String title, boolean realizeNow) {
        super(title, realizeNow);
        init();
    }

    public void realize() {
        super.realize();
        init(); // make sure style overrides take
    }

    private void init() {
        setStyleName("downloadFilePopupOuterPanel");
        setCornerStyleName("downloadFilePopupOuterPanelRounding");
    }

    protected String getBorderColor() {
        return "#99CCFF"; // hack until border color can be retrieved dynamically from CSS
    }

    protected void addDescription(String prompt, String description) {
        if (description == null)
            return;

        //setTitle(description);
        if (prompt == null)
            add(HtmlUtils.getHtml(description, "downloadFilePopupDescription"));
        else
            add(new HTMLPanel(
                    "<span class='prompt'>File description:&nbsp;</span>" +
                            "<span class='downloadFilePopupDescription'>" + description + "</span>"));
    }

    protected Panel createDownloadLink(String prefixText, Link link) {
        SimplePanel panel = new SimplePanel();
        panel.setStyleName("downloadFilePopupFormatLine");

        String id = HTMLPanel.createUniqueId();
        HTMLPanel line = new HTMLPanel("<span class='greaterGreater'>&gt;&gt;</span>&nbsp;" + prefixText + "&nbsp;<span id='" + id + "'></span>");
        line.setStyleName("text");
        line.add(link, id);

        panel.add(line);

        DOM.setStyleAttribute(panel.getElement(), "display", "inline");
        DOM.setStyleAttribute(link.getElement(), "display", "inline");
        DOM.setStyleAttribute(line.getElement(), "display", "inline");

        return panel;
    }

    protected void addCloseLink() {
        VerticalPanel closePanel = new VerticalPanel();
        closePanel.setStyleName("downloadFilePopupCloseLinkWrapper");
        closePanel.setWidth("100%");
        closePanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        closePanel.add(new ActionLink("close", new PopupCloser(this)));
        add(closePanel);
    }
}
