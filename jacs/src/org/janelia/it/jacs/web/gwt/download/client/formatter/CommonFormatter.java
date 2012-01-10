
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
