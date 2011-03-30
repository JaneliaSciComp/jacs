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

package org.janelia.it.jacs.web.gwt.download.client.formatter;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Panel;
import org.janelia.it.jacs.web.gwt.common.client.panel.SpanListExpander;
import org.janelia.it.jacs.web.gwt.common.client.ui.SpanList;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 11, 2006
 * Time: 11:25:26 AM
 * <p/>
 * Common facilities for use among all types of specific formatters.
 */
public class CommonFormatter {
    /**
     * Often, we use an expandable prompted panel--that is a "label: value after label".
     *
     * @param prompt    what to bold up for appearance.
     * @param value     what comes after the prompt
     * @param styleName how to style
     * @return panel with prompt and label.
     */
    public Panel getExpandablePromptedPanel(String prompt, String value, String styleName) {
        return getExpandablePromptedPanel(prompt, value, styleName, 200);
    }

    public Panel getExpandablePromptedPanel(String prompt, String value, String styleName, int minChars) {
        SpanList spanList = new SpanList();
        spanList.addSpan(prompt, "DownloadInheritsColorPrompt");
        spanList.addSpan(value, styleName);

        return new SpanListExpander(spanList, minChars).getContentPanel();
    }

    /**
     * Very common to use a prompted panel--that is a "label: value after label".
     *
     * @param prompt    what to bold up for appearance.
     * @param value     what comes after the prompt
     * @param styleName how to style
     * @return panel with prompt and label.
     */
    public HTMLPanel getPromptedPanel(String prompt, String value, String styleName) {
        HTMLPanel panel = new HTMLPanel(
                "<span class='DownloadInheritsColorPrompt'>" +
                        prompt +
                        ":</span>&nbsp;<span class='" + styleName + "'>" +
                        value +
                        "</span>"
        );

        return panel;
    }

    /**
     * Very common to use a prompted panel--that is a "label: value after label".
     *
     * @param prompt    what to bold up for appearance.
     * @param value     what comes after the prompt
     * @param styleName how to style
     * @return panel with prompt and label.
     */
    public HTMLPanel getItalicPromptedPanel(String prompt, String value, String styleName) {
        HTMLPanel panel = new HTMLPanel(
                "<span class='italicPrompt'>" +
                        prompt +
                        ":</span>&nbsp;<span class='" + styleName + "'>" +
                        value +
                        "</span>");

        return panel;
    }

}
