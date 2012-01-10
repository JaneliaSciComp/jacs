
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledPanel but sets styles to use third-level colors.
 *
 * @author Michael Press
 */
public class WhiteTitledBox extends TertiaryTitledBox {
    public WhiteTitledBox(String title, boolean showActionLinks) {
        super(title, showActionLinks);
    }

    protected void init() {
        setActionLinkBackgroundStyleName("whiteTitledBoxActionLinkBackground"); // has to be before super.init()
        super.init();
        setContentsPanelStyleName("whiteTitledBoxContentsPanel");
        setCornerStyleName("whiteTitledBoxRounding");
        setLabelStyleName("whiteTitledBoxLabel");
        setLabelCornerStyleName("whiteTitledBoxLabelRounding");
        setActionLinkBackgroundStyleName("whiteTitledBoxActionLinkBackground");
    }

    protected String getBorderColor() {
        return "#AAAAAA";
    }
}