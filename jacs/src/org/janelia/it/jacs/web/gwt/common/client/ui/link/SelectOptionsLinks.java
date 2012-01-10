
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.List;

/**
 * Creates a Widget that looks like this:
 * <p/>
 * Select: this | that
 * <p/>
 * where "this" and "that" are links, and there's an optional leading icon image.
 *
 * @author Michael Press
 */
public class SelectOptionsLinks extends Composite {
    private String _prompt;
    private List<OptionItem> _options;

    public SelectOptionsLinks(String prompt, List<OptionItem> options) {
        _prompt = prompt;
        _options = options;
        init();
    }

    private void init() {
        HorizontalPanel panel = new HorizontalPanel();

        panel.add(HtmlUtils.getHtml(_prompt + ":&nbsp;&nbsp;", "infoPrompt"));
        int i = 0;
        for (OptionItem item : _options) {
            if (i++ > 0)
                panel.add(HtmlUtils.getHtml("&nbsp;|&nbsp;", "smallLinkSeparator"));
            panel.add(new SmallLink(item.getLabel(), item.getClickListener()));
        }

        initWidget(panel);
    }
}
