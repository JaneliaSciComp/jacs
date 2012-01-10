
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * @author Michael Press
 */
public class NextActionLink extends ActionLink {
    public NextActionLink(String linkText, ClickListener clickListener) {
        this(linkText, clickListener, null);
        init();
    }

    public NextActionLink(String linkText, ClickListener clickListener, String targetHistoryToken) {
        super(linkText, /*Image*/ null, clickListener);
        setTargetHistoryToken(targetHistoryToken);
        init();
    }

    protected void init() {
        Image image = ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().createImage();
        image.setStyleName("backActionLinkImage"); // need to push it down

        setImage(image, Side.RIGHT);
    }

    public void setEnabled(boolean enabled) {
        Image image;
        if (enabled) {
            setLinkStyleName("actionLink");
            image = ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().createImage();
        }
        else {
            setLinkStyleName("disabledActionLink");
            image = ImageBundleFactory.getControlImageBundle().getArrowRightDisabledImage().createImage();
        }
        addLinkStyleName("actionLinkTextWrapper"); // space the text away from images
        image.setStyleName("backActionLinkImage");
        setImage(image, Side.RIGHT);
    }
}