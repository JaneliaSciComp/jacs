
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.Image;
import org.janelia.it.jacs.web.gwt.search.client.panel.SelectionCounter;

/**
 * Same as a SearchResultsIconPanel except that it is not located in a CategorySummarySearchPanel, meaning
 * it's by itself in a TitledBox, so it doesn't have a background.
 *
 * @author Michael Press
 */
public class StandaloneSearchResultsIconPanel extends SearchResultsIconPanel {
    public StandaloneSearchResultsIconPanel(Image iconImage, String header, SelectionCounter selectionCounter) {
        super(iconImage, header, selectionCounter);
        setHighlightMatches(false);
        setFooterStyleName("StandaloneSearchResultIconFooter");
        setPanelHoverStyleName("StandaloneSearchResultsIconPanelHover");
        setPanelSelectedStyleName("StandaloneSearchResultsIconPanelSelected");
        setPanelUnselectedStyleName("StandaloneSearchResultsIconPanelUnselected");
    }
}
