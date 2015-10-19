
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import org.janelia.it.jacs.web.gwt.search.client.panel.SelectionCounter;

/**
 * @author Michael Press
 */
public class SearchTinyIconPanel extends SearchIconPanel {
    public SearchTinyIconPanel(Image iconImage, String header, SelectionCounter selectionCounter) {
        super(iconImage, header, selectionCounter);
        setHighlightMatches(false);
        setIconStyleName("SearchTinyIconImage");
        setHeaderStyleName("SearchTinyIconTitle");
        setFooterStyleName("SearchTinyIconFooter");
        setPanelHoverStyleName("SearchTinyIconPanelHover");
        setPanelSelectedStyleName("SearchTinyIconPanelSelected");
        setPanelUnselectedStyleName("SearchTinyIconPanelUnselected");
    }

    /**
     * Override parent with no selection behavior on click
     */
    protected ClickListener createClickListener() {
        return null;
    }

    protected MouseListener createMouseListener() {
        return null;
    }
}