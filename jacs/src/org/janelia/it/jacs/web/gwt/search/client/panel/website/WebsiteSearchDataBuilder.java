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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.AbstractPagingPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.paging.SimplePaginator;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.PagedDataRetriever;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.search.client.model.WebsiteResult;
import org.janelia.it.jacs.web.gwt.search.client.panel.CategorySearchDataBuilder;

import java.util.Iterator;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:54:25 AM
 */
public class WebsiteSearchDataBuilder extends CategorySearchDataBuilder {

    private static final String DATA_PANEL_TITLE = "All Website Matches";

    private class WebSearchResultsPanel extends AbstractPagingPanel {
        WebSearchResultsPanel(SimplePaginator paginator, String[] pageSizeOptions) {
            super(paginator, pageSizeOptions);
        }

        public void render(Object data) {
            List webSearchResults = (List) data;
            dataPanel.clear();
            if (webSearchResults == null || webSearchResults.size() == 0) {
                String message = "No result found";
                dataPanel.clear();
                dataPanel.add(HtmlUtils.getHtml(message, "text"));
            }
            else {
                for (Iterator itr = webSearchResults.iterator(); itr.hasNext();) {
                    WebsiteResult websiteResult = (WebsiteResult) itr.next();
                    dataPanel.add(createWebSearchResultPanel(websiteResult));
                }
            }
        }

        public void renderError(Throwable throwable) {
            String errorMessage = "Error retrieving data: " + throwable.getMessage();
            dataPanel.clear();
            dataPanel.add(HtmlUtils.getHtml(errorMessage, "error"));
        }

        private Panel createWebSearchResultPanel(WebsiteResult websiteResult) {
            VerticalPanel webSearchResultPanel = new VerticalPanel();
            webSearchResultPanel.setStyleName("WebSearchResultPanel");
            // add web page title
            Widget titleLink = HtmlUtils.getHtml(websiteResult.getTitle(), "WebSearchResultTitle");
            webSearchResultPanel.add(titleLink);
            HTML blurb = HtmlUtils.getHtml(websiteResult.getBlurb(), "WebSearchResultBlurb");
            webSearchResultPanel.add(blurb);
            if (websiteResult.getTitleURL() != null) {
                HTML url = HtmlUtils.getHtml(websiteResult.getTitleURL(), "WebSearchResultURL");
                webSearchResultPanel.add(url);
            }
            return webSearchResultPanel;
        }

    }

    private SimplePaginator webSearchResultsPaginator;

    public WebsiteSearchDataBuilder(String searchId, String searchQuery) {
        super(searchId, searchQuery);
    }

    public void populateDataPanel() {
        if (!_haveData) {
            webSearchResultsPaginator.first();
        }
    }

    protected String getPanelSearchCategory() {
        return SearchTask.TOPIC_WEBSITE;
    }

    protected String[][] getSortOptions() {
        return new String[][]{
        };
    }

    protected List formatDataListAsTableRowList(List dataList) {
        // simply return the same data and don't do any formatting in this case
        return dataList;
    }

    protected PagedDataRetriever createDataRetriever() {
        return new CategoryResultDataRetriever();
    }

    protected Panel createDataPanel(int defaultNumVisibleRows, String[] pageLengthOptions) {
        webSearchResultsPaginator = new SimplePaginator(createDataRetriever(), defaultNumVisibleRows, getSortOptions());
        WebSearchResultsPanel webSearchResultsPanel =
                new WebSearchResultsPanel(webSearchResultsPaginator, pageLengthOptions);
        webSearchResultsPanel.setWidth("100%");
        return webSearchResultsPanel;
    }

    protected String createDataPanelTitle() {
        return DATA_PANEL_TITLE;
    }

}
