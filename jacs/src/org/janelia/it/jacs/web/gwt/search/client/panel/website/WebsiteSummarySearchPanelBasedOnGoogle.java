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

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.googlesearch.client.GoogleSearchHandler;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySummarySearchPanel;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSummarySearchPanelBasedOnGoogle extends CategorySummarySearchPanel {

    public WebsiteSummarySearchPanelBasedOnGoogle(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    protected CategorySearchDataBuilder createDataBuilder(String searchId, String searchQuery) {
        return new WebsiteSearchDataBuilderBasedOnGoogle(searchId, searchQuery);
    }

    public void populatePanel() {
        DockPanel iconPanel = createSearchIconPanel();
        VerticalPanel googleBrandingPanel = new VerticalPanel();
        iconPanel.add(googleBrandingPanel, DockPanel.SOUTH);
        addItem(iconPanel);
        VerticalPanel googleResultsPanel = new VerticalPanel();
        googleResultsPanel.setStyleName("SiteSearchSummaryPanel");
        addItem(googleResultsPanel);
        GoogleSearchHandler googleSearch = new GoogleSearchHandler(googleResultsPanel);
        googleSearch.setBrandingPanel(googleBrandingPanel);
        googleSearch.addSearchableDomains("JaCS", Constants.VICSWEB_DOMAIN);
        googleSearch.executeSearch(
                getDataBuilder().getSearchId(),
                getDataBuilder().getSearchQuery(),
                GoogleSearchHandler.WEBSEARCH);
    }

}
