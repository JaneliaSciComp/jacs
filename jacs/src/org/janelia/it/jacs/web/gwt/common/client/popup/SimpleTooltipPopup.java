
package org.janelia.it.jacs.web.gwt.common.client.popup;

import com.google.gwt.user.client.ui.HTML;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

/**
 * @author Michael Press
 */
public class SimpleTooltipPopup extends BasePopupPanel {
    private HTML _html = null;
    private static String DEFAULT_TEXT_STYLE_NAME = "text";

    public SimpleTooltipPopup(String text) {
        this(text, DEFAULT_TEXT_STYLE_NAME);
    }

    public SimpleTooltipPopup(String text, String styleName) {
        this(HtmlUtils.getHtml(text, styleName));
    }

    public SimpleTooltipPopup(HTML html) {
        super(null, false); // defer realizing until needed
        _html = html;
    }

    protected void populateContent() {
        add(_html);
    }
}
