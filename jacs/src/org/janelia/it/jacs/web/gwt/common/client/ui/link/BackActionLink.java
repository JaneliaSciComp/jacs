
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class BackActionLink extends ActionLink {
    public BackActionLink(String linkText, ClickListener clickListener) {
        this(linkText, clickListener, null);
        init();
    }

    public BackActionLink(String linkText, ClickListener clickListener, String targetHistoryToken) {
        super(linkText, /*Image*/ null, clickListener);
        setTargetHistoryToken(targetHistoryToken);
        init();
    }

    private void init() {
        Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
        image.setStyleName("backActionLinkImage"); // need to push it down

        setImage(image);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            setLinkStyleName("actionLink");
            addLinkStyleName("actionLinkTextWrapper"); // space the text away from images

            Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
            image.setStyleName("backActionLinkImage");
            setImage(image);
        }
        else {
            setLinkStyleName("disabledActionLink");
            addLinkStyleName("actionLinkTextWrapper"); // space the text away from images

            Image image = ImageBundleFactory.getControlImageBundle().getArrowLeftDisabledImage().createImage();
            image.setStyleName("backActionLinkImage");
            setImage(image);
        }
    }
}