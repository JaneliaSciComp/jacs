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

package org.janelia.it.jacs.web.gwt.search.server.formatter;

import org.janelia.it.jacs.web.gwt.search.client.SearchArtifactFactory;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 7, 2007
 * Time: 5:04:54 PM
 */
public class SearchResultsCSVFormatterFactory extends SearchArtifactFactory {


    public SearchResultsCSVFormatterFactory() {
        super();
    }

    public SearchResultCSVFormatter createSearchResultCSVFormatter(String category) {
        return (SearchResultCSVFormatter) createCategorySearchArtifact(category,
                new Object[]{
                });
    }

    public Object createAllSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result formatter for the 'ALL' category");
    }

    public Object createAccessionsSearchArtifact(Object[] args) {
        throw new IllegalArgumentException("Cannot create a result formatter for the 'Accessions' category");
    }

    public Object createProteinsSearchArtifact(Object[] args) {
        return new ProteinSearchResultCSVFormatter();
    }

    public Object createClustersSearchArtifact(Object[] args) {
        return new ClusterSearchResultCSVFormatter();
    }

    public Object createPublicationsSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Publication result CSV Formatter is not implemented");
    }

    public Object createProjectsSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Project result CSV Formatter is not implemented");
    }

    public Object createSamplesSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Sample result CSV Formatter is not implemented");
    }

    public Object createWebsiteSearchArtifact(Object[] args) {
        throw new UnsupportedOperationException("Web result CSV Formatter is not implemented");
    }

}
