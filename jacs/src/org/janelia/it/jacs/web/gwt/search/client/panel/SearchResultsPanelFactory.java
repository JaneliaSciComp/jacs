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

package org.janelia.it.jacs.web.gwt.search.client.panel;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;
import org.janelia.it.jacs.web.gwt.search.client.panel.project.ProjectSearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationSearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchResultsPanel;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchResultsPanelBasedOnGoogle;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class SearchResultsPanelFactory extends SearchArtifactFactory {

    public SearchResultsPanelFactory() {
        super();
    }

    public SearchResultsPanel createSearchSummaryPanel(String category, String searchId) {
        return (SearchResultsPanel) createCategorySearchArtifact(category,
                new Object[]{
                        searchId,
                        category
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'Accessions' category");
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new SearchResultsPanel("Protein Search Details",
                (String) args[0], (String) args[1]);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new SearchResultsPanel("Protein Clusters Search Details",
                (String) args[0], (String) args[1]);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new PublicationSearchResultsPanel("Publications Search Details",
                (String) args[0], (String) args[1]);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new ProjectSearchResultsPanel("Projects Search Details",
                (String) args[0], (String) args[1]);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SampleSearchResultsPanel("Samples Search Details",
                (String) args[0], (String) args[1]);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return new WebsiteSearchResultsPanelBasedOnGoogle("JaCS Site Search Details",
                    (String) args[0], (String) args[1]);
        }
        return new WebsiteSearchResultsPanel("JaCS Site Search Details",
                (String) args[0], (String) args[1]);
    }

}
