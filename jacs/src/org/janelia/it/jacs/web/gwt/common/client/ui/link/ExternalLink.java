
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class ExternalLink extends Link {
    public ExternalLink(String destURL) {
        this(destURL, destURL);
    }

    public ExternalLink(String linkText, String destURL) {
        super(linkText, destURL);
    }

    public ExternalLink(String linkText, ClickListener clickListener) {
        super(linkText, clickListener);
    }

    // specify that the link will open in different window
    protected String getTargetWindow() {
        return "_other" + HTMLPanel.createUniqueId();
    }

    protected String getLinkContents() {
        return _linkText + "&nbsp;" + ImageBundleFactory.getControlImageBundle().getExternalLinkImage().getHTML();
    }

}
