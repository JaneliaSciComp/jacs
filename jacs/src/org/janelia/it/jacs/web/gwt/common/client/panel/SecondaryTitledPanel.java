
package org.janelia.it.jacs.web.gwt.common.client.panel;

/**
 * Same as a TitledPanel but sets styles to use "scondary" colors, such that a SecondaryTitledPanel can be placed
 * inside a TitledPanel and it will visually differentiate itself.
 *
 * @author Michael Press
 */
public class SecondaryTitledPanel extends TitledPanel {
    private static final String SECONDARY_PORTLET_TITLE_STYLE = "secondaryPortletTitle";
    private static final String SECONDARY_PORTLET_STYLE = "secondaryPortlet";
    private static final String SECONDARY_TOP_ROUND_STYLE_NAME = "secondaryPortletTopRounding";
    private static final String SECONDARY_PORTLET_BOTTOM_ROUNDING_STYLE = "secondaryPortletBottomRounding";

    public SecondaryTitledPanel() {
        init();
    }

    public SecondaryTitledPanel(String title) {
        super(title);
        init();
    }

    public SecondaryTitledPanel(String title, boolean showActionLinks) {
        super(title, showActionLinks);
        init();
    }

    public SecondaryTitledPanel(String title, boolean showActionLinks, boolean realizeNow) {
        super(title, showActionLinks, realizeNow);
        init();
    }

    //TODO: refactor TitledPanel to get styles via methods, then override those methods here
    private void init() {
        setContentStyleName(SECONDARY_PORTLET_STYLE);
        setBottomRoundCornerStyleName(SECONDARY_PORTLET_BOTTOM_ROUNDING_STYLE);
        setWidth("auto");
    }

    protected String getRoundingBorder() {
        return "#CACACA";
    }

    protected String getTitleStyleName() {
        return SECONDARY_PORTLET_TITLE_STYLE;
    }

    protected String getTopRoundingBorderStyleName() {
        return SECONDARY_TOP_ROUND_STYLE_NAME;
    }
}
