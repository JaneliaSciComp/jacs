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

package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * @author Michael Press
 */
public class HtmlUtils {
    /**
     * Creates an HTML widget constructed with the specified html and with styleName set to the specified style.
     * Convenience method allows HTML widgets to be constructed with one line of code.
     *
     * @param html       the HTML String to use to construct the HTML Widget
     * @param styleClass the CSS style to set on the HTML Widget
     * @return the HTML Widget
     */
    public static HTML getHtml(String html, String styleClass) {
        html = (html == null) ? "&nbsp;" : html;
        HTML widget = new HTML(html);
        if (styleClass != null)
            widget.setStyleName(styleClass);

        return widget;
    }

    /**
     * Same as <code>getHtml(String html, String styleClass)</code> except adds a second style name to the HTML
     * (<code>setStyleName(firstClass)</code> then <code>addStyleName(secondClass)</code>.
     */
    public static HTML getHtml(String html, String styleClass, String anotherStyleClass) {
        HTML widget = getHtml(html, styleClass);
        widget.addStyleName(anotherStyleClass);
        return widget;
    }

    /**
     * Same as <code>getHtml(String html, String styleClass)</code> except also adds a ClickListener to the HTML
     */
    public static HTML getHtml(String html, String styleClass, ClickListener clickListener) {
        HTML widget = getHtml(html, styleClass);
        if (clickListener != null)
            widget.addClickListener(clickListener);

        return widget;
    }

    /**
     * Creates an HTMLPanel, with the styleName set to the specified class name, and the contents of the
     * HTMLPanel set to an HTML with the specified spanReplacementValue.
     */
    public static HTMLPanel getHtmlPanel(String spanId, String spanReplacementValue, String styleClass) {
        HTMLPanel htmlPanel = new HTMLPanel("<span id='" + spanId + "'></span>");
        htmlPanel.add(new HTML(spanReplacementValue), spanId);
        if (styleClass != null) {
            htmlPanel.setStyleName(styleClass);
        }
        return htmlPanel;
    }
}
