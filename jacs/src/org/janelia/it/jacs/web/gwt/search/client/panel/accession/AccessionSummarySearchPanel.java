
package org.janelia.it.jacs.web.gwt.search.client.panel.accession;

import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class AccessionSummarySearchPanel extends CategorySummarySearchPanel {

    private Image accessionIcon;

    public AccessionSummarySearchPanel(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new AccessionSearchDataBuilder(searchId, searchQuery);
    }

    public void populatePanel() {
        // create the summary panel widgets
        createAccessionIcon();
        addItem(accessionIcon);
        Panel dataPanel = getDataBuilder().createDataPanel();
        addItem(dataPanel);
        // populate the summary panel
        getDataBuilder().populateDataPanel();
    }

    protected void init() {
        _canvas = new HorizontalPanel();
        _canvas.setStyleName("AccessionSummarySearchPanel");
        _canvas.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        _canvas.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        initWidget(_canvas);
    }

    private void createAccessionIcon() {
        SearchIconPanelFactory searchIconFactory = new SearchIconPanelFactory();
        accessionIcon = searchIconFactory.createImage(ImageBundleFactory.getCategoryImageBundle().getAccessionIconSmall(), Constants.SEARCH_ALL);
    }

}
