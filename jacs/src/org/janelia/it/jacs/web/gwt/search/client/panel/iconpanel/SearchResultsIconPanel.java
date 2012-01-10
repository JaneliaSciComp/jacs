
package org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListener;
import org.janelia.it.jacs.web.gwt.common.client.util.NumberUtils;
import org.janelia.it.jacs.web.gwt.search.client.panel.SelectionCounter;

/**
 * @author Michael Press
 */
public class SearchResultsIconPanel extends SearchIconPanel {
    public SearchResultsIconPanel(Image iconImage, String header, SelectionCounter selectionCounter) {
        super(iconImage, header, selectionCounter);
        setHighlightMatches(false);
        setIconStyleName("SearchResultIconImage");
        setHeaderStyleName("SearchResultIconTitle");
        setFooterStyleName("SearchResultIconFooter");
        setPanelHoverStyleName("SearchResultsIconPanelHover");
        setPanelSelectedStyleName("SearchResultsIconPanelSelected");
        setPanelUnselectedStyleName("SearchResultsIconPanelUnselected");
    }

    protected void setFooterHTML(int numMatches) {
        setFooterHTML(NumberUtils.formatInteger(numMatches) + "&nbsp;match" + ((numMatches > 1) ? "es" : ""));
    }

    protected MouseListener createMouseListener() {
        return null;
    }

    protected ClickListener createClickListener() {
        return new SearchResultsIconMouseManager(this);
    }
}
