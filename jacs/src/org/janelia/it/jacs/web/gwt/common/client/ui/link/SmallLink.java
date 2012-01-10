
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;

/**
 * @author Michael Press
 */
public class SmallLink extends Link {
    public SmallLink(String linkText) {
        super(linkText, null, null);
        setHyperlinkStyleName("smallTextLink");
    }

    public SmallLink(String linkText, ClickListener clickListener) {
        super(linkText, null, clickListener);
        setHyperlinkStyleName("smallTextLink");
    }

    public SmallLink(String linkText, String destURL) {
        super(linkText, destURL, null);
        setHyperlinkStyleName("smallTextLink");
    }
}
