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
import org.janelia.it.jacs.web.gwt.search.client.panel.cluster.ClusterSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.project.ProjectSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.protein.ProteinSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.publication.PublicationSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.sample.SampleSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchDataBuilder;
import org.janelia.it.jacs.web.gwt.search.client.panel.website.WebsiteSearchDataBuilderBasedOnGoogle;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
public class SearchResultTableBuilderFactory extends SearchArtifactFactory {

    public SearchResultTableBuilderFactory() {
        super();
    }

    public CategorySearchDataBuilder createResultTableBuilder(String category,
                                                              String searchId,
                                                              String searchQuery) {
        return (CategorySearchDataBuilder) createCategorySearchArtifact(category,
                new Object[]{
                        searchId,
                        searchQuery
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result builder for the 'Accessions' category");
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new ProteinSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new ClusterSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        return new PublicationSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        return new ProjectSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        return new SampleSearchDataBuilder((String) args[0], (String) args[1]);
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        if (Constants.USE_GOOGLE_API_FOR_WEBSITE_SEARCH) {
            return new WebsiteSearchDataBuilderBasedOnGoogle((String) args[0], (String) args[1]);
        }
        return new WebsiteSearchDataBuilder((String) args[0], (String) args[1]);
    }

}
