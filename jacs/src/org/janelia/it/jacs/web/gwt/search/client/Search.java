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

package org.janelia.it.jacs.web.gwt.search.client;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;
import org.janelia.it.jacs.web.gwt.common.client.wizard.WizardController;
import org.janelia.it.jacs.web.gwt.detail.client.DetailPanel;
import org.janelia.it.jacs.web.gwt.search.client.model.SearchResultsData;
import org.janelia.it.jacs.web.gwt.search.client.page.SearchDetailPage;
import org.janelia.it.jacs.web.gwt.search.client.page.SearchEntityDetailPage;
import org.janelia.it.jacs.web.gwt.search.client.page.SearchMainPage;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:50:40 PM
 */
public class Search extends WizardController {
    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.search.client.Search");
    public static final String SEARCH_ID_PARAM = "searchId";
    public static final String SEARCH_KEYWORD_PARAM = "keyword";
    public static final String SEARCH_CATEGORY_PARAM = "searchCategory";
    public static final String SEARCH_PRIOR_ID_PARAM = "priorSearchId";
    public static final String SEARCH_PRIOR_CATEGORY_HITS_PARAM = "priorCategoryHits";

    private SearchResultsData _searchResultsData = new SearchResultsData();

    public void onModuleLoad() {
        // Create and fade in the page contents
        try {
            // Setup the pages in the wizard
            addPage(new SearchMainPage(_searchResultsData, this));
            addPage(new SearchDetailPage(_searchResultsData, this));
            addPage(new SearchEntityDetailPage(_searchResultsData, this));
        }
        catch (Throwable t) {
            _logger.error("Error onModuleLoad - Wizard. ", t);
        }

        // Show the wizard
        start();
    }

    protected void processURLParam(String name, String value) {
        if (SEARCH_PRIOR_ID_PARAM.equalsIgnoreCase(name) || SEARCH_ID_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using priorSearchId=" + value + " from URL");
            _searchResultsData.setPriorSearchId(value);
            _searchResultsData.setSearchId(value);
            _searchResultsData.setFireSearchFlag(true);
        }
        else if (SEARCH_PRIOR_CATEGORY_HITS_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using priorCategoryHits=" + value + " from URL");
            _searchResultsData.setPriorCategoryHits(new Integer(value));
        }
        else if (SEARCH_CATEGORY_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using searchCategory=" + value + " from URL");
            _searchResultsData.setSelectedCategory(value);
        }
        else if (SEARCH_KEYWORD_PARAM.equalsIgnoreCase(name)) {
            _logger.debug("Using keyword=" + value + " from URL");
            if (value != null && value.trim().length() > 0) {
                _searchResultsData.setFireSearchFlag(true);
                _searchResultsData.setSearchString(value);
            }
        }
        else if (DetailPanel.ACC_PARAM.equalsIgnoreCase(name)) {
            _searchResultsData.setEntityDetailAcc(value);
        }
        else {
            _logger.error("Got unknown param " + name + "=" + value + " from URL");
        }
    }

    public Breadcrumb getBreadcrumbSection() {
        return new Breadcrumb(Constants.SEARCH_SECTION_LABEL, UrlBuilder.getSearchUrl());
    }

    /**
     * Always start search on the first page when loading the entrypoint due to unpredictable behavior on
     * later pages if the first page hasn't already been loaded.
     */
    protected int getStartingPage(String startURL) {
        return 0;
    }
}
