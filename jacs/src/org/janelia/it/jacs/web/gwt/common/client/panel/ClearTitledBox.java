
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledBox but sets no background styles, so it inherits its parents styles.  Requires the specification
 * of a title label background color (using setActionLinkPanelStyleName(StyleName)) or else the title label is transparent and shows part of the main box line
 * underneath the text.  The background color of the style padded to setActionLinkPanelStyleName() should match the
 * background color of the panel to which the ClearTitledBox is added.
 *
 * @author Michael Press
 */
public class ClearTitledBox extends SecondaryTitledBox {
    /**
     * @param title
     * @param showActionLinks
     */
    public ClearTitledBox(String title, boolean showActionLinks) {
        super(title, showActionLinks);
    }

    protected void init() {
        super.init();

        setContentsPanelStyleName("clearTitledBoxContentsPanel");
        setCornerStyleName("clearTitledBoxRounding");
        setLabelStyleName("clearTitledBoxLabel");
        setLabelCornerStyleName("clearTitledBoxLabelRounding");
        setLabelPanelStyleName("clearTitledBoxTitleLabel");
        setActionLinkBackgroundStyleName("clearTitledBoxTitleLabel");
    }

    /**
     * Explicitly returns null to signal no border on title area
     */
    protected String getTitleBorderColor() {
        return null;
    }
}
