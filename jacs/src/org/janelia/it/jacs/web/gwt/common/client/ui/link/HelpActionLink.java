
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class HelpActionLink extends ActionLink {
    public HelpActionLink(String linkText) {
        this(linkText, /*url*/ null);
    }

    public HelpActionLink(String linkText, String url) {
        super(linkText);
        setShowBrackets(false);
        init(url);
    }

    private void init(final String url) {
        // Create the icon
        setImage(ImageBundleFactory.getControlImageBundle().getHelpImage().createImage());
        setUrl(url);
    }

    public void setUrl(final String url) {
        addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(url, "_other", "");
            }
        });
    }
}

