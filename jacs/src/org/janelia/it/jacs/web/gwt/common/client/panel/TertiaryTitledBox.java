
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledPanel but sets styles to use third-level colors.
 *
 * @author Michael Press
 */
public class TertiaryTitledBox extends TitledBox {
    public TertiaryTitledBox(String title, boolean showActionLinks) {
        super(title, showActionLinks);
    }

    protected void init() {
        setActionLinkBackgroundStyleName("tertiaryTitledBoxActionLinkBackground"); // has to be before super.init()
        super.init();
        setContentsPanelStyleName("tertiaryTitledBoxContentsPanel");
        setCornerStyleName("tertiaryTitledBoxRounding");
        setLabelStyleName("tertiaryTitledBoxLabel");
        setLabelCornerStyleName("tertiaryTitledBoxLabelRounding");
        setActionLinkBackgroundStyleName("tertiaryTitledBoxActionLinkBackground");
    }

    protected String getBorderColor() {
        return "#AAAAAA";
    }
}
