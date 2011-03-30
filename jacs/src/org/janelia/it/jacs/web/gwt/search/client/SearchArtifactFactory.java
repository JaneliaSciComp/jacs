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

import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 6, 2007
 * Time: 1:44:54 PM
 */
abstract public class SearchArtifactFactory {

    protected SearchArtifactFactory() {
    }

    abstract public Object createAllSearchArtifact(Object[] args);

    abstract public Object createAccessionsSearchArtifact(Object[] args);

    abstract public Object createProteinsSearchArtifact(Object[] args);

    abstract public Object createClustersSearchArtifact(Object[] args);

    abstract public Object createPublicationsSearchArtifact(Object[] args);

    abstract public Object createProjectsSearchArtifact(Object[] args);

    abstract public Object createSamplesSearchArtifact(Object[] args);

    abstract public Object createWebsiteSearchArtifact(Object[] args);

    public Object createCategorySearchArtifact(String category, Object[] args) {
        Object searchArtifact = null;
        if (category == null) {
            throw new IllegalArgumentException("Invalid category");
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_ALL) ||
                category.equalsIgnoreCase(Constants.SEARCH_ALL)) {
            searchArtifact = createAllSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_ACCESSION) ||
                category.equalsIgnoreCase(Constants.SEARCH_ACCESSION)) {
            searchArtifact = createAccessionsSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_PROTEIN) ||
                category.equalsIgnoreCase(Constants.SEARCH_PROTEINS)) {
            searchArtifact = createProteinsSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_CLUSTER) ||
                category.equalsIgnoreCase(Constants.SEARCH_CLUSTERS)) {
            searchArtifact = createClustersSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_PUBLICATION) ||
                category.equalsIgnoreCase(Constants.SEARCH_PUBLICATIONS)) {
            searchArtifact = createPublicationsSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_PROJECT) ||
                category.equalsIgnoreCase(Constants.SEARCH_PROJECTS)) {
            searchArtifact = createProjectsSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_SAMPLE) ||
                category.equalsIgnoreCase(Constants.SEARCH_SAMPLES)) {
            searchArtifact = createSamplesSearchArtifact(args);
        }
        else if (category.equalsIgnoreCase(SearchTask.TOPIC_WEBSITE) ||
                category.equalsIgnoreCase(Constants.SEARCH_WEBSITE)) {
            searchArtifact = createWebsiteSearchArtifact(args);
        }
        else {
            throw new IllegalArgumentException("Unknown search category: " +
                    category);
        }
        return searchArtifact;
    }

}
