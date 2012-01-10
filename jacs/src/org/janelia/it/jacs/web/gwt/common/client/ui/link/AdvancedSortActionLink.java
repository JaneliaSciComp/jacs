
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class AdvancedSortActionLink extends ActionLink {
    public AdvancedSortActionLink() {
        this(null);
        init();
    }

    public AdvancedSortActionLink(ClickListener clickListener) {
        super("Advanced Sort", /*Image*/ null, clickListener);
        init();
    }

    private void init() {
        setImage(ImageBundleFactory.getControlImageBundle().getAdvancedSortImage().createImage());
    }
}