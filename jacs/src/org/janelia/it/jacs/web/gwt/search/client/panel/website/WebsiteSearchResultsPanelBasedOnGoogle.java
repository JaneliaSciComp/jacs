/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.search.client.panel.website;

import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.googlesearch.client.GoogleSearchHandler;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.panel.SearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchIconPanelFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.iconpanel.SearchResultsIconPanelFactory;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSearchResultsPanelBasedOnGoogle extends SearchResultsPanel {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.panel.WebsiteSearchResultsPanelBasedOnGoogle");

    public WebsiteSearchResultsPanelBasedOnGoogle(String title, String searchId, String category) {
        super(title, searchId, category);
    }

    public void populatePanel(SearchResultsData searchResult) {
        createResultsPanel(searchResult);
    }

    private void createResultsPanel(SearchResultsData searchResult) {
        // create a top panel that has the search icon and the google search input
        HorizontalPanel topPanel = new HorizontalPanel();
        topPanel.setStyleName("SiteSearchInputPanel");
        add(topPanel);
        SearchIconPanelFactory iconFactory = new SearchResultsIconPanelFactory();
        SearchIconPanel iconPanel = iconFactory.createSearchIconPanel(getCategory(), true);
        topPanel.add(iconPanel);
        //topPanel.setCellWidth(iconPanel,iconPanel.getIconWidth());
        HorizontalPanel googleInputPanel = new HorizontalPanel();
        googleInputPanel.setWidth("100%");
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        topPanel.add(googleInputPanel);
        // create a search results panel that will contain the google search results
        VerticalPanel googleResultsPanel = new VerticalPanel();
        googleResultsPanel.setStyleName("SiteSearchResultPanel");
        add(googleResultsPanel);
        GoogleSearchHandler googleSearch = new GoogleSearchHandler(googleResultsPanel);
        googleSearch.setInputPanel(googleInputPanel);
        googleSearch.addSearchableDomains("JaCS", Constants.VICSWEB_DOMAIN);
        googleSearch.setResultSetSize(GoogleSearchHandler.LARGERESULTSET);
        googleSearch.executeSearch(searchResult.getSearchId(),
                searchResult.getSearchString(),
                GoogleSearchHandler.WEBSEARCH);
    }

}
