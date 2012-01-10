
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledPanel but sets styles to use "scondary" colors, such that a SecondaryTitledPanel can be placed
 * inside a TitledPanel and it will visually differentiate itself.
 *
 * @author Michael Press
 */
public class SecondaryTitledBox extends TitledBox {
    public SecondaryTitledBox(String title, boolean showActionLinks) {
        super(title, showActionLinks);
    }

    public SecondaryTitledBox(String title, boolean showActionLinks, boolean showContent) {
        super(title, showActionLinks, showContent);
    }

    protected void init() {
        super.init();
        setContentsPanelStyleName("secondaryTitledBoxContentsPanel");
        setCornerStyleName("secondaryTitledBoxRounding");
        setLabelStyleName("secondaryTitledBoxLabel");
        setLabelCornerStyleName("secondaryTitledBoxLabelRounding");
        setActionLinkBackgroundStyleName("secondaryTitledBoxActionLinkBackground"); // has to be before super.init()
    }

    protected String getBorderColor() {
        return "#AAAAAA";
    }
}
