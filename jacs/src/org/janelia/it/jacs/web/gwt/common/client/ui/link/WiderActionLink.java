
package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;

/**
 * Abstract ActionLink that toggles between "wider" (primary state) and "narrower" (secondary state). Subclasses
 * must override toggleTo[Primary,Secondary]State to implement behavior on user action to change width.
 */
abstract public class WiderActionLink extends ToggleActionLink {
    private String _wideWidth;
    private String _narrowWidth;

    /**
     * @param narrowWidth String representing the narrower width of the panel (suitable for Widget.setWidth())
     * @param wideWidth   String representing the wider width of the panel (suitable for Widget.setWidth())
     */
    public WiderActionLink(String narrowWidth, String wideWidth) {
        super("width", "width");
        _narrowWidth = narrowWidth;
        _wideWidth = wideWidth;
        init();
    }

    private void init() {
        Image leftArrow = ImageBundleFactory.getControlImageBundle().getArrowLeftEnabledImage().createImage();
        leftArrow.setStyleName("widerActionLinkLeftImage");

        Image rightArrow = ImageBundleFactory.getControlImageBundle().getArrowRightEnabledImage().createImage();
        rightArrow.setStyleName("widerActionLinkRightImage");

        setPrimaryImage(leftArrow);
        setSecondaryImage(rightArrow, Side.RIGHT);
    }

    public String getNarrowWidth() {
        return _narrowWidth;
    }

    public String getWideWidth() {
        return _wideWidth;
    }
}
